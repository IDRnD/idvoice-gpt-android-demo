package net.idrnd.idvoicegpt.idrnd

sealed class IDVoiceResponse {
    object Success : IDVoiceResponse()
    sealed class Failure : IDVoiceResponse() {
        object CheckLivenessFailed : Failure()
        object MatchFailed : Failure()
    }
}
