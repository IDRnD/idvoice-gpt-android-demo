package net.idrnd.idvoicegpt.ui.main

import android.Manifest.permission.RECORD_AUDIO
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import net.idrnd.idvoicegpt.R
import net.idrnd.idvoicegpt.speechrecognition.SpeechService
import net.idrnd.idvoicegpt.speechrecognition.SpeechService.Companion.ACCESS_TOKEN_NULL
import net.idrnd.idvoicegpt.speechrecognition.SpeechService.Companion.API_FAILED_CALL
import net.idrnd.idvoicegpt.speechrecognition.SpeechService.Companion.API_NOT_READY
import net.idrnd.idvoicegpt.speechrecognition.SpeechService.Companion.SPEECH_SERVICE_ERROR_TYPE
import net.idrnd.idvoicegpt.ui.ChatIntent
import net.idrnd.idvoicegpt.ui.NetworkNotifier
import net.idrnd.idvoicegpt.ui.widgets.AnimatedTextView
import net.idrnd.idvoicegpt.util.ErrorMessageUtil
import net.idrnd.idvoicegpt.util.HapticFeedbackUtil
import net.idrnd.idvoicegpt.util.IDVoiceMessageGetter
import net.idrnd.idvoicegpt.util.UIStateUtil
import net.idrnd.idvoicegpt.util.openAppInfo
import net.idrnd.idvoicegpt.util.toast

/**
 * Main screen of the application
 */
class MainFragment : Fragment(R.layout.fragment_main), NetworkNotifier {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(requireActivity().applicationContext)
    }

    private val speechServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(SPEECH_SERVICE_ERROR_TYPE, -1)) {
                ACCESS_TOKEN_NULL -> context.toast(getString(R.string.access_token_is_null))
                API_NOT_READY -> context.toast(getString(R.string.api_not_ready))
                API_FAILED_CALL -> context.toast(getString(R.string.api_call_error))
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.userIntent.send(ChatIntent.ListenIntent)
                }
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.permission_required))
                    .setMessage(getString(R.string.permission_not_granted))
                    .setPositiveButton(getString(R.string.close)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()
            }
        }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            viewModel.connectService(SpeechService.from(binder))
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            viewModel.disconnectService()
        }
    }

    private lateinit var logoIV: ImageView
    private lateinit var instructionText: TextView
    private lateinit var micFAB: FloatingActionButton

    private lateinit var cancelButton: FloatingActionButton
    private lateinit var listenedTV: TextView
    private lateinit var sendButton: FloatingActionButton

    private lateinit var chatUserLayout: ConstraintLayout
    private lateinit var chatUserIcon: ImageView
    private lateinit var chatUserText: TextView

    private lateinit var chatGPTLayout: ConstraintLayout
    private lateinit var chatGPTIcon: ImageView
    private lateinit var chatGPTText: AnimatedTextView

    private lateinit var bigStopButton: FloatingActionButton

    private val hapticFeedbackUtil by lazy { HapticFeedbackUtil(requireContext()) }
    private val idVoiceMessageGetter
        get() = IDVoiceMessageGetter(requireContext().resources)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMenu()
        activity?.findViewById<Toolbar>(R.id.main_toolbar)?.title = getString(R.string.title)
        viewModel.setIDVoiceMessageGetter(idVoiceMessageGetter)
        setViews(view)
        setListeners()
        setObservers()
    }

    private fun setMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_refresh -> {
                            viewLifecycleOwner.lifecycleScope.launch {
                                viewModel.userIntent.send(ChatIntent.CleanUpAppIntent)
                            }
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun setObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mainState.collect {
                UIStateUtil.setUIState(
                    it,
                    requireContext(),
                    logoIV,
                    instructionText,
                    micFAB,
                    cancelButton,
                    listenedTV,
                    sendButton,
                    chatUserLayout,
                    chatUserText,
                    chatGPTLayout,
                    chatGPTIcon,
                    chatGPTText,
                    bigStopButton,
                    hapticFeedbackUtil
                )
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorFlow.collect {
                ErrorMessageUtil.showErrorMessage(it, requireContext())
            }
        }
    }

    private fun setListeners() {
        chatGPTIcon.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.userIntent.send(ChatIntent.Speak)
            }
        }

        micFAB.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.userIntent.send(ChatIntent.ListenIntent)
                    }
                }

                shouldShowRequestPermissionRationale(RECORD_AUDIO) -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.permission_required))
                        .setMessage(getString(R.string.rationale_message))
                        .setPositiveButton(getString(R.string.go_to_settings)) { dialog, _ ->
                            requireContext().openAppInfo()
                            dialog.dismiss()
                        }
                        .setCancelable(true)
                        .show()
                }

                else -> {
                    requestPermissionLauncher.launch(
                        RECORD_AUDIO
                    )
                }
            }
        }

        cancelButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.userIntent.send(ChatIntent.CancelListeningIntent)
            }
        }

        sendButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.userIntent.send(ChatIntent.SendQuestionIntent)
            }
        }

        bigStopButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.userIntent.send(ChatIntent.StopAnsweringIntent(chatGPTText.text.toString()))
            }
        }
    }

    private fun setViews(view: View) {
        logoIV = view.findViewById(R.id.logo_iv)
        instructionText = view.findViewById(R.id.instruction_tv)
        micFAB = view.findViewById(R.id.mic_fab)

        cancelButton = view.findViewById(R.id.cancel_button)
        listenedTV = view.findViewById(R.id.question_tv)
        sendButton = view.findViewById(R.id.send_button)

        chatUserLayout = view.findViewById(R.id.chat_user_layout)
        chatUserIcon = view.findViewById(R.id.chat_user_icon)
        chatUserText = view.findViewById(R.id.chat_user_text)

        chatGPTLayout = view.findViewById(R.id.chat_gpt_layout)
        chatGPTIcon = view.findViewById(R.id.chat_gpt_icon)
        chatGPTText = view.findViewById(R.id.chat_gpt_text)

        bigStopButton = view.findViewById(R.id.big_stop_button)
    }

    override fun onStart() {
        super.onStart()
        bindService()
        viewModel.startListeningSpeechAndSpeechIfNeeded()
        ContextCompat.registerReceiver(
            requireContext(),
            speechServiceReceiver,
            IntentFilter(SpeechService.ACTION_SPEECH_SERVICE_ERROR),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
    private fun bindService() {
        requireContext().apply {
            bindService(
                Intent(this, SpeechService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE
            )
        }
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(speechServiceReceiver)
        viewModel.stopListeningVoiceAndSpeech()
        unbindService()
    }

    private fun unbindService() {
        requireContext().unbindService(serviceConnection)
    }

    override fun notifyNetworkStatus(isConnected: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userIntent.send(ChatIntent.ConnectionUpdateIntent(isConnected))
        }
    }
}
