package net.idrnd.idvoicegpt.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * TextView extension animates its alpha value
 */
class AnimatedTextView : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val animator = ValueAnimator.ofFloat(0f, 1f)

    fun animateBackground() {
        animator.addUpdateListener {
            alpha = it.animatedValue as Float
        }

        animator.duration = 700
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = -1
        animator.start()
    }

    fun stopBackgroundAnimation() {
        alpha = 1f
        animator.cancel()
        text = ""
    }
}
