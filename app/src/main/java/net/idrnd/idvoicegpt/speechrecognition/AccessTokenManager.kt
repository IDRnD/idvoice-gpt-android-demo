package net.idrnd.idvoicegpt.speechrecognition

import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import net.idrnd.idvoicegpt.R
import java.io.IOException
import java.util.Date

/**
 * Handles the access token for Google Cloud services.
 */
class AccessTokenManager(
    private val sharedPreferences: SharedPreferences,
    private val resources: Resources
) {

    /**
     * Represents access token, it´s fetched from local if existent and valid, else it´s fetched
     * from remote and saved
     */
    val accessToken: AccessToken?
        get() {
            var token = fetchAccessTokenLocal()
            // Check if the current token is still valid for a while.
            if (token != null && token.expirationTime.time > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE) {
                return token
            }

            token = fetchAccessTokenRemote()
            if (token == null) {
                return null
            }
            saveAccessToken(token)
            return token
        }

    /**
     * Saves access token locally.
     *
     * @param accessToken The access token to save.
     *
     */
    private fun saveAccessToken(accessToken: AccessToken) {
        sharedPreferences.apply {
            edit()
                .putString(PREF_ACCESS_TOKEN_VALUE, accessToken.tokenValue)
                .putLong(
                    PREF_ACCESS_TOKEN_EXPIRATION_TIME,
                    accessToken.expirationTime.time
                ).apply()
        }
    }

    /**
     * Fetches the access token from local.
     *
     * @return the AccessToken
     */
    private fun fetchAccessTokenLocal(): AccessToken? {
        val tokenValue = sharedPreferences.getString(PREF_ACCESS_TOKEN_VALUE, null)
        val expirationTime = sharedPreferences.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1)
        if (tokenValue == null || expirationTime < System.currentTimeMillis()) {
            return null
        }
        return AccessToken(tokenValue, Date(expirationTime))
    }

    /**
     * Fetches the access token from remote.
     *
     * @return the AccessToken
     */
    private fun fetchAccessTokenRemote(): AccessToken? {
        val stream = resources.openRawResource(R.raw.credentials)
        try {
            val credentials = GoogleCredentials.fromStream(stream)
                .createScoped(SpeechService.SCOPE)
            return credentials.refreshAccessToken()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to obtain access token.", e)
        }
        return null
    }

    companion object {
        private val TAG = AccessTokenManager::class.qualifiedName

        private const val PREF_ACCESS_TOKEN_VALUE = "access_token_value"
        private const val PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time"

        // We refresh the current access token before it expires.
        const val ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000 // One minute.

        // We reuse an access token if its expiration time is longer than this.
        const val ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000 // Thirty minutes.
    }
}
