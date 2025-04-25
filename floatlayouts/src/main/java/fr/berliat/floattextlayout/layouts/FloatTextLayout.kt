package fr.berliat.floatlayouts.layouts

import android.content.Context
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import fr.berliat.floatlayouts.R
import kotlin.math.max
import androidx.core.content.withStyledAttributes

class FloatTextLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private val floatView: View
        get() = getChildAt(0) // Expected to be the image or floating block

    private val textTop = TextView(context)
    private val textBottom = TextView(context)
    private var floatPadding = 0

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = textTop.textSize
        color = textTop.currentTextColor
    }

    var text: CharSequence = ""
        set(value) {
            field = value
            requestLayout()
        }

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FloatTextLayout,
            0,
            0
        )

        try {
            text = typedArray.getString(R.styleable.FloatTextLayout_textToFloat) ?: ""

            context.withStyledAttributes(attrs, R.styleable.FloatTextLayout) {
                floatPadding = getDimensionPixelSize(
                    R.styleable.FloatTextLayout_floatPadding,
                    0
                )
            }


            val textSize = typedArray.getDimensionPixelSize(
                R.styleable.FloatTextLayout_android_textSize,
                -1
            )
            val textColor = typedArray.getColor(
                R.styleable.FloatTextLayout_android_textColor,
                textTop.currentTextColor // fallback
            )

            if (textSize > 0) {
                textTop.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
                textBottom.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
            }

            textTop.setTextColor(textColor)
            textBottom.setTextColor(textColor)

            textTop.setPadding(floatPadding, 0, 0, 0)

        } finally {
            typedArray.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        addView(textTop)
        addView(textBottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)

        // Measure floatView (the ImageView or block)
        measureChild(floatView, widthMeasureSpec, heightMeasureSpec)
        val floatW = floatView.measuredWidth
        val floatH = floatView.measuredHeight

        val textAreaWidth = (width - floatW - floatPadding).coerceAtLeast(0)

        // Use StaticLayout to figure out how much text fits beside the image
        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, textAreaWidth)
            .build()

        var cutIndex = 0
        var accumulatedHeight : Int

        // Iterate through the lines and accumulate the height
        for (i in 0 until staticLayout.lineCount) {
            accumulatedHeight = staticLayout.getLineBottom(i)

            // Check if the accumulated height exceeds the floating block height
            if (accumulatedHeight > floatH) {
                // The current line is where the cut should happen
                cutIndex = staticLayout.getLineEnd(i - 2)  // Cut before this line
                break
            }
        }

        // Handle the case when the text doesn't exceed the height of the floating block
        if (cutIndex == 0) {
            cutIndex = text.length // Keep all the text in the top section if no cut is needed
        }

        val topText = text.substring(0, cutIndex)
        val bottomText = text.substring(cutIndex)

        textTop.text = topText
        textBottom.text = bottomText

        textTop.measure(
            MeasureSpec.makeMeasureSpec(textAreaWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED) // Remove the floatH constraint here
        )

        textBottom.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )

        val totalHeight = max(floatH, textTop.measuredHeight) + if (bottomText == "") 0 else textBottom.measuredHeight
        setMeasuredDimension(width, totalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val floatW = floatView.measuredWidth
        val floatH = floatView.measuredHeight

        floatView.layout(0, 0, floatW, floatH)

        textTop.layout(
            floatW,
            0,
            floatW + textTop.measuredWidth + floatPadding,
            textTop.measuredHeight + floatPadding
        )

        val textBottomTop = max(floatH, textTop.measuredHeight)
        textBottom.layout(
            0,
            textBottomTop,
            measuredWidth,
            textBottomTop + textBottom.measuredHeight
        )
    }
}