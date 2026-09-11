package studio.guitarlab.core.codec

enum class SampleRateAction {
    PASSTHROUGH,
    RESAMPLE,
}

data class SampleRatePlan(
    val sourceRateHz: Int,
    val targetRateHz: Int,
    val action: SampleRateAction,
    val ratio: Double,
)

object SampleRateStrategy {
    private val supportedProjectRates = setOf(44_100, 48_000, 88_200, 96_000)

    fun plan(sourceRateHz: Int, projectRateHz: Int): SampleRatePlan {
        require(sourceRateHz in 8_000..384_000) { "Unsupported source sample rate: $sourceRateHz" }
        require(projectRateHz in supportedProjectRates) { "Unsupported project sample rate: $projectRateHz" }
        val action = if (sourceRateHz == projectRateHz) SampleRateAction.PASSTHROUGH else SampleRateAction.RESAMPLE
        return SampleRatePlan(
            sourceRateHz = sourceRateHz,
            targetRateHz = projectRateHz,
            action = action,
            ratio = projectRateHz.toDouble() / sourceRateHz.toDouble(),
        )
    }
}
