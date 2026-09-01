package dev.gaphunter.grpcmissingdeadlinecompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import dev.gaphunter.grpcmissingdeadlinecompanion.detect.JavaMissingDeadlineFinder
import dev.gaphunter.grpcmissingdeadlinecompanion.review.ReviewPrompt

/**
 * Flags an RPC call on a generated gRPC blocking stub whose
 * stub-construction chain never sets a deadline -- gRPC's own
 * documentation states plainly that without one, "a client can end up
 * waiting for a response effectively forever." Runs via `checkFile`
 * (same shape as every other inspection in this catalog);
 * [JavaMissingDeadlineFinder] does the real PSI walk.
 */
class MissingDeadlineInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null
        if (file !is PsiJavaFile) return null

        val hits = JavaMissingDeadlineFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                "gRPC blocking-stub call with no deadline -- gRPC's own docs state that without one, " +
                    "\"a client can end up waiting for a response effectively forever\", " +
                    "hanging this thread if the downstream service is slow or unreachable",
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
            }
        }

        return problems.toTypedArray()
    }
}
