package dev.gaphunter.grpcmissingdeadlinecompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.grpcmissingdeadlinecompanion.model.MissingDeadlineHit

/**
 * Finds an RPC call on a generated gRPC blocking stub
 * (`XxxGrpc.newBlockingStub(channel)....call(request)`) whose
 * stub-construction chain never passes through
 * `.withDeadline(...)`/`.withDeadlineAfter(...)` -- gRPC's own
 * documentation states plainly that "by default, gRPC does not set a
 * deadline... a client can end up waiting for a response effectively
 * forever". A slow downstream service hangs the caller's thread
 * indefinitely, propagating failures in cascade.
 *
 * **v0.1 scope, stated honestly:** only the synchronous client
 * (`newBlockingStub`) -- the async/reactive case (`newStub`/
 * `newFutureStub`) propagates deadlines via `Context` instead of a
 * builder chain, a structurally different mechanism deferred to a
 * future version.
 */
object JavaMissingDeadlineFinder {

    private val DEADLINE_METHODS = setOf("withDeadline", "withDeadlineAfter")
    private val NON_RPC_CHAIN_METHODS = setOf("newBlockingStub") + DEADLINE_METHODS

    fun findAll(file: PsiFile): List<MissingDeadlineHit> {
        val hits = mutableListOf<MissingDeadlineHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                hitFor(expression)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitFor(call: PsiMethodCallExpression): MissingDeadlineHit? {
        if (!isTerminalCallInChain(call)) return null // an intermediate builder step, not the actual RPC invocation

        val methodName = call.methodExpression.referenceName ?: return null
        if (methodName in NON_RPC_CHAIN_METHODS) return null // a dead-end chain with no real RPC call yet

        val sawDeadline = walksThroughBlockingStubRoot(call.methodExpression.qualifierExpression) ?: return null
        if (sawDeadline) return null

        return MissingDeadlineHit(anchorOf(call.methodExpression))
    }

    /**
     * Walks [expression] down its own qualifier chain looking for the
     * `XxxGrpc.newBlockingStub(channel)` root. Returns whether a
     * `withDeadline`/`withDeadlineAfter` call was seen anywhere along
     * the way, or `null` if the chain never reaches a real
     * `newBlockingStub` root (not a gRPC stub chain at all -- never
     * flagged).
     */
    private fun walksThroughBlockingStubRoot(expression: PsiExpression?): Boolean? {
        var current = expression
        var sawDeadline = false
        while (current is PsiMethodCallExpression) {
            val name = current.methodExpression.referenceName
            if (name in DEADLINE_METHODS) sawDeadline = true
            if (name == "newBlockingStub") {
                val qualifier = current.methodExpression.qualifierExpression ?: return null
                if (!qualifier.text.endsWith("Grpc")) return null
                return sawDeadline
            }
            current = current.methodExpression.qualifierExpression
        }
        return null
    }

    /** True when [call]'s own result isn't immediately chained into a further `.method()` call -- i.e. it's the last call in the chain, not an intermediate builder step. */
    private fun isTerminalCallInChain(call: PsiMethodCallExpression): Boolean {
        val parent = call.parent
        return !(parent is PsiReferenceExpression && parent.qualifierExpression === call)
    }

    private fun anchorOf(methodExpr: PsiReferenceExpression): PsiElement = methodExpr.referenceNameElement ?: methodExpr
}
