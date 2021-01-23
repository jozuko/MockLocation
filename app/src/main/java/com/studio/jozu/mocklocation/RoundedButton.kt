package com.studio.jozu.mocklocation

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import kotlin.math.roundToInt

/**
 * Created by r.mori on 2021/01/19.
 * Copyright (c) 2021 rei-frontier. All rights reserved.
 */
@Suppress("MemberVisibilityCanBePrivate")
class RoundedButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : CardView(context, attrs, defStyleAttr) {
    companion object {
        private const val SANS = 1
        private const val SERIF = 2
        private const val MONOSPACE = 3
        private const val MIN_RIPPLE_SIZE = 5
    }

    private val defPadding = 0f
    private val defTextColor = ColorStateList.valueOf(Color.WHITE)
    private val defTextSize = (20 * context.resources.displayMetrics.density).roundToInt()
    private val defRippleColor = ContextCompat.getColor(context, android.R.color.transparent)

    private val rippleDrawable = context.getDrawable(R.drawable.ripple_drawable)
    private val viewText: TextView = TextView(context)
    private val viewRipple = ImageView(context)

    init {
        viewRipple.layoutParams = LayoutParams(MIN_RIPPLE_SIZE, MIN_RIPPLE_SIZE)
        viewRipple.isVisible = false
        addView(viewRipple, viewRipple.layoutParams)

        viewText.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(viewText, viewText.layoutParams)

        this.isClickable = true
        this.isFocusable = true
        this.clipChildren = true
        this.clipToPadding = true
        this.setPadding(0, 0, 0, 0)

        attrs?.let { setUpAttrs(it) }
    }

    var textMargin: Float = defPadding
        set(value) {
            field = value
            viewText.updateLayoutParams<LayoutParams> {
                val margin = value.roundToInt()
                setMargins(margin, margin, margin, margin)
            }
        }

    var textColors: ColorStateList = defTextColor
        set(value) {
            field = value
            viewText.setTextColor(value)
        }

    var text: CharSequence = ""
        set(value) {
            field = value
            viewText.text = text
        }

    var textSize: Int = defTextSize
        set(value) {
            field = textSize
            viewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, value.toFloat())
        }

    var rippleColor: Int = defRippleColor
        set(value) {
            field = value
            val tint = value and 0x00FFFFFF or 0x40000000
            rippleDrawable?.setTint(tint)
            rippleDrawable?.setTintMode(PorterDuff.Mode.SRC_IN)
            viewRipple.setImageDrawable(rippleDrawable)
        }

    private fun setUpAttrs(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundedButton)
        try {
            initPadding(typedArray)
            initText(typedArray)
            initTextSize(typedArray)
            initTextColors(typedArray)
            initTextStyle(typedArray)
            initRippleColor(typedArray)
        } finally {
            typedArray.recycle()
        }
    }

    private fun initPadding(typedArray: TypedArray) {
        textMargin = typedArray.getDimension(R.styleable.RoundedButton_android_padding, defPadding)
    }

    private fun initTextColors(typedArray: TypedArray) {
        val colors = typedArray.getColorStateList(R.styleable.RoundedButton_android_textColor)
        textColors = colors ?: defTextColor
    }

    private fun initText(typedArray: TypedArray) {
        this.text = typedArray.getText(R.styleable.RoundedButton_android_text)
    }

    private fun initTextSize(typedArray: TypedArray) {
        this.textSize = typedArray.getDimensionPixelSize(R.styleable.RoundedButton_android_textSize, defTextSize)
    }

    private fun initTextStyle(typedArray: TypedArray) {
        val familyName = typedArray.getString(R.styleable.RoundedButton_android_fontFamily)
        val typefaceIndex = typedArray.getInt(R.styleable.RoundedButton_android_typeface, -1)
        val styleIndex = typedArray.getInt(R.styleable.RoundedButton_android_textStyle, -1)

        if (familyName != null) {
            val typeface = Typeface.create(familyName, styleIndex)
            if (typeface != null) {
                viewText.typeface = typeface
                return
            }
        }

        var typeface: Typeface? = null
        when (typefaceIndex) {
            SANS -> {
                typeface = Typeface.SANS_SERIF
            }
            SERIF -> {
                typeface = Typeface.SERIF
            }
            MONOSPACE -> {
                typeface = Typeface.MONOSPACE
            }
        }
        viewText.setTypeface(typeface, styleIndex)
    }

    private fun initRippleColor(typedArray: TypedArray) {
        this.rippleColor = typedArray.getColor(R.styleable.RoundedButton_rippleColor, defRippleColor)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_UP -> {
                rippleAnimation(event.x, event.y)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun rippleAnimation(x: Float, y: Float) {
        val scale = this.width.coerceAtLeast(this.height).toFloat() / MIN_RIPPLE_SIZE.toFloat() * 2
        ValueAnimator.ofFloat(1.0f, scale).apply {
            duration = 300
            addUpdateListener {
                val value = it.animatedValue as Float
                viewRipple.scaleX = value
                viewRipple.scaleY = value
            }

            doOnStart {
                viewRipple.x = x
                viewRipple.y = y
                viewRipple.isVisible = true
            }

            doOnEnd {
                viewRipple.isVisible = false
            }
        }.start()
    }
}