package net.idrnd.idvoicegpt.util

import android.content.res.Resources
import net.idrnd.idvoicegpt.R
import net.idrnd.idvoicegpt.idrnd.IDVoiceResponse
import kotlin.random.Random

class IDVoiceMessageGetter(resources: Resources) {

    private var matchFailureMessages = getMatchFailureMessages(resources)
    private var livenessFailureMessages = getLivenessFailureMessages(resources)

    fun getFailureMessage(failure: IDVoiceResponse.Failure) = when (failure) {
        IDVoiceResponse.Failure.MatchFailed -> matchFailureMessages
        IDVoiceResponse.Failure.CheckLivenessFailed -> livenessFailureMessages
    }.let {
        val position = Random.nextInt(0, it.size - 1)
        it[position]
    }

    private fun getMatchFailureMessages(resources: Resources): List<String> =
        listOf(
            resources.getString(R.string.match_failure_1),
            resources.getString(R.string.match_failure_2),
            resources.getString(R.string.match_failure_3),
            resources.getString(R.string.match_failure_4),
            resources.getString(R.string.match_failure_5),
            resources.getString(R.string.match_failure_6)
        )

    private fun getLivenessFailureMessages(resources: Resources) =
        listOf(
            resources.getString(R.string.liveness_failure_1),
            resources.getString(R.string.liveness_failure_2),
            resources.getString(R.string.liveness_failure_3)
        )
}
