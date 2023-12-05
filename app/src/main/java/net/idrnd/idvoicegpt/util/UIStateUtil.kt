package net.idrnd.idvoicegpt.util

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.idrnd.idvoicegpt.R
import net.idrnd.idvoicegpt.ui.UIState
import net.idrnd.idvoicegpt.ui.widgets.AnimatedTextView

object UIStateUtil {

    fun setUIState(
        uiState: UIState,
        context: Context,
        logoIV: ImageView,
        instructionText: TextView,
        micFAB: FloatingActionButton,
        cancelButton: FloatingActionButton,
        listenedTV: TextView,
        sendButton: FloatingActionButton,
        chatUserLayout: ConstraintLayout,
        chatUserText: TextView,
        chatGPTLayout: ConstraintLayout,
        chatGPTIcon: ImageView,
        chatGPTText: AnimatedTextView,
        bigStopButton: FloatingActionButton,
        hapticFeedbackUtil: HapticFeedbackUtil
    ) {
        when (uiState) {
            is UIState.Idle -> {
                logoIV.isVisible = uiState.showLogo
                instructionText.isVisible = uiState.showLogo
                micFAB.isVisible = true
                micFAB.isEnabled = uiState.isConnected

                cancelButton.isVisible = false
                listenedTV.isVisible = false
                sendButton.isVisible = false
                bigStopButton.isVisible = false

                chatUserLayout.isVisible = !uiState.showLogo
                chatUserText.text = uiState.question

                chatGPTLayout.isVisible = !uiState.showLogo

                chatGPTText.stopBackgroundAnimation()

                chatGPTText.text = uiState.response

                listenedTV.text = uiState.listened

                chatGPTIcon.setImageDrawable(
                    getTintedDrawable(
                        chatGPTIcon.drawable,
                        R.color.green,
                        context.resources
                    )
                )
            }

            is UIState.Listening -> {
                logoIV.isVisible = uiState.showLogo
                instructionText.isVisible = uiState.showLogo

                micFAB.isVisible = false

                cancelButton.isVisible = true
                listenedTV.isVisible = true
                sendButton.isVisible = true
                bigStopButton.isVisible = false

                chatUserLayout.isVisible = !uiState.showLogo
                chatGPTLayout.isVisible = !uiState.showLogo

                chatUserText.text = uiState.question
                chatGPTText.text = uiState.response

                when (uiState.listened) {
                    "" -> {
                        listenedTV.text = context.getText(R.string.listening)
                        sendButton.isEnabled = false
                    }

                    else -> {
                        sendButton.isEnabled = true
                        listenedTV.text = uiState.listened
                    }
                }
            }

            is UIState.Thinking -> {
                logoIV.isVisible = false
                instructionText.isVisible = false
                micFAB.isVisible = true

                cancelButton.isVisible = false
                listenedTV.isVisible = false
                sendButton.isVisible = false
                bigStopButton.isVisible = true

                chatUserLayout.isVisible = true
                chatGPTLayout.isVisible = true

                chatUserText.text = uiState.userQuestion
                chatGPTText.text = "..."
                chatGPTText.animateBackground()

                chatGPTIcon.setImageDrawable(
                    getTintedDrawable(
                        chatGPTIcon.drawable,
                        R.color.gray,
                        context.resources
                    )
                )
            }

            is UIState.Answering -> {
                logoIV.isVisible = false
                instructionText.isVisible = false
                micFAB.isVisible = true

                cancelButton.isVisible = false
                listenedTV.isVisible = false
                sendButton.isVisible = false
                bigStopButton.isVisible = true

                chatGPTText.stopBackgroundAnimation()
                chatGPTText.text = uiState.answer
                hapticFeedbackUtil.vibrate()
                chatGPTIcon.setImageDrawable(
                    getTintedDrawable(
                        chatGPTIcon.drawable,
                        R.color.gray,
                        context.resources
                    )
                )
            }
        }
    }

    private fun getTintedDrawable(drawable: Drawable, color: Int, resources: Resources): Drawable {
        val wrappedDrawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(wrappedDrawable, ResourcesCompat.getColor(resources, color, null))
        return wrappedDrawable
    }
}
