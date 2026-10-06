package studio.guitarlab.core.audio

/** Session-scoped capability of one explicit MAIN/CUE physical route pair. */
enum class CueRouteCapabilityState {
    NOT_CONFIGURED,
    CANDIDATE_AVAILABLE,
    VERIFYING,
    SUPPORTED,
    CONVERGED_TO_MAIN,
    MISSING_EFFECTIVE_ROUTE,
    WRONG_OR_MIRRORED_ROUTE,
    CLOCK_UNAVAILABLE,
    OFFSET_EXCEEDED,
    LOST,
    FAILED,
}
