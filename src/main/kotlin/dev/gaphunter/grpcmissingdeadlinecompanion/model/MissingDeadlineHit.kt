package dev.gaphunter.grpcmissingdeadlinecompanion.model

import com.intellij.psi.PsiElement

/** One gRPC blocking-stub RPC call with no `.withDeadline(...)`/`.withDeadlineAfter(...)` anywhere in its stub-construction chain. */
data class MissingDeadlineHit(val anchor: PsiElement)
