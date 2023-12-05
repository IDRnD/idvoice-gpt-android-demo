package net.idrnd.idvoicegpt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import net.idrnd.idvoicegpt.speechrecognition.SpeechService
import net.idrnd.idvoicegpt.ui.NetworkNotifier
import net.idrnd.idvoicegpt.ui.history.HistoryFragment
import net.idrnd.idvoicegpt.ui.main.MainFragment
import net.idrnd.idvoicegpt.ui.settings.SettingsFragment
import net.idrnd.idvoicegpt.util.NetworkUtil
import net.idrnd.idvoicegpt.util.toast

/**
 * Main activity that handles app general events and fragment back stack.
 */
class MainActivity : AppCompatActivity() {

    val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isConnected = NetworkUtil.isNetworkConnected(connectivityManager)
            if (!isConnected) {
                toast(getString(R.string.no_internet_connection))
            }
            notifyNetworkStatus(isConnected)
        }
    }

    private val googleCloudAPINotReadyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            context.toast(getString(R.string.api_not_ready))
        }
    }

    private fun notifyNetworkStatus(isConnected: Boolean) {
        supportFragmentManager.fragments.forEach {
            if (it is NetworkNotifier) {
                it.notifyNetworkStatus(isConnected)
            }
        }
    }

    private fun isNetworkConnectedOnDemand() {
        NetworkUtil.isNetworkConnected(
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        ).apply {
            if (!this) {
                toast(getString(R.string.no_internet_connection))
            }
            notifyNetworkStatus(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.main_toolbar))

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.activity_main_layout,
                    MainFragment(),
                    MainFragment::class.simpleName
                ).commit()
        }
    }

    override fun onStart() {
        super.onStart()
        registerNetworkReceiver()
        registerGoogleCloudAPINotReadyReceiver()
        isNetworkConnectedOnDemand()
    }

    private fun registerNetworkReceiver() {
        ContextCompat.registerReceiver(
            this,
            networkReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun registerGoogleCloudAPINotReadyReceiver() {
        ContextCompat.registerReceiver(
            this,
            googleCloudAPINotReadyReceiver,
            IntentFilter(SpeechService.ACTION_GOOGLE_CLOUD_SPEECH_CLIENT_NOT_READY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
        unregisterReceiver(googleCloudAPINotReadyReceiver)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                false
            }

            R.id.action_history -> {
                fragmentTransaction(
                    R.id.activity_main_layout,
                    HistoryFragment()
                )
                true
            }

            R.id.action_settings -> {
                fragmentTransaction(
                    R.id.activity_main_layout,
                    SettingsFragment()
                )
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fragmentTransaction(id: Int, fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                id,
                fragment,
                fragment.javaClass.simpleName
            )
            .addToBackStack(null)
            .commit()
    }

}
