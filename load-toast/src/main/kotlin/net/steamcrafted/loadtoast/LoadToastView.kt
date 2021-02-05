package net.steamcrafted.loadtoast

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import com.nineoldandroids.animation.Animator
import com.nineoldandroids.animation.AnimatorListenerAdapter
import com.nineoldandroids.animation.ValueAnimator
import kotlin.math.max

class LoadToastView(context: Context?) : ImageView(context) {
    private val textPaint = Paint().apply {
        textSize = 15f
        color = Color.BLACK
        isAntiAlias = true
    }

    private val backPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    private val iconBackPaint = Paint().apply {
        color = Color.BLUE
        isAntiAlias = true
    }

    private val loaderPaint = Paint().apply {
        strokeWidth = 4.dpToPx().toFloat()
        isAntiAlias = true
        color = fetchPrimaryColor()
        style = Paint.Style.STROKE
    }

    private val successPaint = Paint().apply {
        color = resources.getColor(R.color.loadtoast__color_success)
        isAntiAlias = true
    }

    private val errorPaint = Paint().apply {
        color = resources.getColor(R.color.loadtoast__color_error)
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = (borderOffset * 2).toFloat()
        color = Color.TRANSPARENT
        style = Paint.Style.STROKE
    }

    private val iconBounds: Rect
    private val mTextBounds = Rect()
    private val spinnerRect = RectF()
    private val completeicon: Drawable
    private val failedicon: Drawable
    private val va: ValueAnimator?
    private val toastPath = Path()
    private val easeinterpol = AccelerateDecelerateInterpolator()
    private var mText = ""
    private var MAX_TEXT_WIDTH = 100 // in DP
    private var BASE_TEXT_SIZE = 20
    private var IMAGE_WIDTH = 40
    private var TOAST_HEIGHT = 48
    private var LINE_WIDTH = 3
    private var WIDTH_SCALE = 0f
    private var MARQUE_STEP = 1
    private var prevUpdate: Long = 0
    private var cmp: ValueAnimator? = null
    private var success = true
    private var outOfBounds = false
    private var mLtr = true
    private var spinnerDrawable: MaterialProgressDrawable? = null
    private var borderOffset = 1.dpToPx()

    init {
        MAX_TEXT_WIDTH = MAX_TEXT_WIDTH.dpToPx()
        BASE_TEXT_SIZE = BASE_TEXT_SIZE.dpToPx()
        IMAGE_WIDTH = IMAGE_WIDTH.dpToPx()
        TOAST_HEIGHT = TOAST_HEIGHT.dpToPx()
        LINE_WIDTH = LINE_WIDTH.dpToPx()
        MARQUE_STEP = MARQUE_STEP.dpToPx()

        val padding = (TOAST_HEIGHT - IMAGE_WIDTH) / 2

        iconBounds =
            Rect(
                TOAST_HEIGHT + MAX_TEXT_WIDTH - padding,
                padding,
                TOAST_HEIGHT + MAX_TEXT_WIDTH - padding + IMAGE_WIDTH,
                IMAGE_WIDTH + padding
            )

        //loadicon = getResources().getDrawable(R.mipmap.ic_launcher);
        //loadicon.setBounds(iconBounds);
        completeicon = resources.getDrawable(R.drawable.loadtoast__ic_navigation_check)
        completeicon.bounds = iconBounds
        failedicon = resources.getDrawable(R.drawable.loadtoast__ic_error)
        failedicon.bounds = iconBounds
        va = ValueAnimator.ofFloat(0f, 1f)
        va.duration = 6000
        va.addUpdateListener(ValueAnimator.AnimatorUpdateListener { //WIDTH_SCALE = valueAnimator.getAnimatedFraction();
            postInvalidate()
        })
        va.repeatMode = ValueAnimator.INFINITE
        va.repeatCount = 9999999
        va.interpolator = LinearInterpolator()
        va.start()
        initSpinner()
        calculateBounds()
    }

    private fun initSpinner() {
        spinnerDrawable = MaterialProgressDrawable(context, this)
        spinnerDrawable!!.setStartEndTrim(0f, .5f)
        spinnerDrawable!!.setProgressRotation(.5f)
        val mDiameter = TOAST_HEIGHT
        val mProgressStokeWidth = LINE_WIDTH
        spinnerDrawable!!.setSizeParameters(
            mDiameter.toDouble(), mDiameter.toDouble(), (
                (mDiameter - mProgressStokeWidth * 2) / 4).toDouble(),
            mProgressStokeWidth.toDouble(), (
                mProgressStokeWidth * 4).toFloat(), (
                mProgressStokeWidth * 2).toFloat()
        )
        spinnerDrawable!!.setBackgroundColor(Color.TRANSPARENT)
        spinnerDrawable!!.setColorSchemeColors(loaderPaint.color)
        spinnerDrawable!!.setVisible(true, false)
        spinnerDrawable!!.alpha = 255
        setImageDrawable(null)
        setImageDrawable(spinnerDrawable)
        spinnerDrawable!!.start()
    }

    fun setTextColor(color: Int) {
        textPaint.color = color
    }

    fun setTextDirection(isLeftToRight: Boolean) {
        mLtr = isLeftToRight
    }

    fun setTextTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    fun setTextSizeSp(size: Int) {
        textPaint.textSize = size.spToPx().toFloat()
    }

    override fun setBackgroundColor(color: Int) {
        backPaint.color = color
        iconBackPaint.color = color
    }

    fun setProgressColor(color: Int) {
        loaderPaint.color = color
        spinnerDrawable!!.setColorSchemeColors(color)
    }

    fun setBorderColor(color: Int) {
        borderPaint.color = color
    }

    fun setBorderWidthPx(widthPx: Int) {
        borderOffset = widthPx / 2
        borderPaint.strokeWidth = (borderOffset * 2).toFloat()
    }

    fun setBorderWidthRes(resourceId: Int) {
        setBorderWidthPx(resources.getDimensionPixelSize(resourceId))
    }

    fun setBorderWidthDp(width: Int) {
        setBorderWidthPx(width.dpToPx())
    }

    fun show() {
        spinnerDrawable!!.stop()
        spinnerDrawable!!.start()
        WIDTH_SCALE = 0f
        if (cmp != null) {
            cmp!!.removeAllUpdateListeners()
            cmp!!.removeAllListeners()
        }
    }

    fun success() {
        success = true
        done()
    }

    fun error() {
        success = false
        done()
    }

    private fun done() {
        val cmp = ValueAnimator.ofFloat(0f, 1f)
        cmp.duration = 600
        cmp.addUpdateListener { valueAnimator ->
            WIDTH_SCALE = 2f * valueAnimator.animatedFraction
            //Log.d("lt", "ws " + WIDTH_SCALE);
            postInvalidate()
        }
        cmp.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                cleanup()
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                cleanup()
            }
        })
        cmp.interpolator = DecelerateInterpolator()
        cmp.start()

        this.cmp = cmp
    }

    private fun fetchPrimaryColor(): Int {
        var color = Color.rgb(155, 155, 155)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val typedValue = TypedValue()
            val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.colorAccent))
            color = a.getColor(0, color)
            a.recycle()
        }
        return color
    }

    fun setText(text: String) {
        mText = text
        calculateBounds()
    }

    private fun calculateBounds() {
        outOfBounds = false
        prevUpdate = 0
        textPaint.textSize = BASE_TEXT_SIZE.toFloat()
        textPaint.getTextBounds(mText, 0, mText.length, mTextBounds)
        if (mTextBounds.width() > MAX_TEXT_WIDTH) {
            var textSize = BASE_TEXT_SIZE
            while (textSize > 13.dpToPx() && mTextBounds.width() > MAX_TEXT_WIDTH) {
                textSize--
                //Log.d("bounds", "width " + mTextBounds.width() + " max " + MAX_TEXT_WIDTH);
                textPaint.textSize = textSize.toFloat()
                textPaint.getTextBounds(mText, 0, mText.length, mTextBounds)
            }
            if (mTextBounds.width() > MAX_TEXT_WIDTH) {
                outOfBounds = true
                /**
                 * float keep = (float)MAX_TEXT_WIDTH / (float)mTextBounds.width();
                 * int charcount = (int)(mText.length() * keep);
                 * //Log.d("calc", "keep " + charcount + " per " + keep + " len " + mText.length());
                 * mText = mText.substring(0, charcount);
                 * textPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
                 */
            }
        }
    }

    override fun onDraw(c: Canvas) {
        var ws = Math.max(1f - WIDTH_SCALE, 0f)
        // If there is nothing to display, just draw a circle
        if (mText.length == 0) ws = 0f
        val translateLoad = (1f - ws) * (IMAGE_WIDTH + MAX_TEXT_WIDTH)
        val leftMargin = translateLoad / 2
        val textOpactity = Math.max(0f, ws * 10f - 9f)
        textPaint.alpha = (textOpactity * 255).toInt()
        spinnerRect[iconBounds.left + 4.dpToPx() - translateLoad / 2, (iconBounds.top + 4.dpToPx()).toFloat(), iconBounds.right - 4.dpToPx() - translateLoad / 2] =
            (iconBounds.bottom - 4.dpToPx()).toFloat()
        val circleOffset = (TOAST_HEIGHT * 2 * (Math.sqrt(2.0) - 1) / 3).toInt()
        val th = TOAST_HEIGHT
        val pd = (TOAST_HEIGHT - IMAGE_WIDTH) / 2
        val iconoffset = (IMAGE_WIDTH * 2 * (Math.sqrt(2.0) - 1) / 3).toInt()
        val iw = IMAGE_WIDTH
        val totalWidth = leftMargin * 2 + th + ws * (IMAGE_WIDTH + MAX_TEXT_WIDTH) - translateLoad
        toastPath.reset()
        toastPath.moveTo(leftMargin + th / 2, 0f)
        toastPath.rLineTo(ws * (IMAGE_WIDTH + MAX_TEXT_WIDTH), 0f)
        toastPath.rCubicTo(circleOffset.toFloat(), 0f, (th / 2).toFloat(), (th / 2 - circleOffset).toFloat(), (th / 2).toFloat(), (th / 2).toFloat())
        toastPath.rLineTo(-pd.toFloat(), 0f)
        toastPath.rCubicTo(0f, -iconoffset.toFloat(), (-iw / 2 + iconoffset).toFloat(), (-iw / 2).toFloat(), (-iw / 2).toFloat(), (-iw / 2).toFloat())
        toastPath.rCubicTo(-iconoffset.toFloat(), 0f, (-iw / 2).toFloat(), (iw / 2 - iconoffset).toFloat(), (-iw / 2).toFloat(), (iw / 2).toFloat())
        toastPath.rCubicTo(0f, iconoffset.toFloat(), (iw / 2 - iconoffset).toFloat(), (iw / 2).toFloat(), (iw / 2).toFloat(), (iw / 2).toFloat())
        toastPath.rCubicTo(iconoffset.toFloat(), 0f, (iw / 2).toFloat(), (-iw / 2 + iconoffset).toFloat(), (iw / 2).toFloat(), (-iw / 2).toFloat())
        toastPath.rLineTo(pd.toFloat(), 0f)
        toastPath.rCubicTo(0f, circleOffset.toFloat(), (circleOffset - th / 2).toFloat(), (th / 2).toFloat(), (-th / 2).toFloat(), (th / 2).toFloat())
        toastPath.rLineTo(ws * (-IMAGE_WIDTH - MAX_TEXT_WIDTH), 0f)
        toastPath.rCubicTo(
            -circleOffset.toFloat(),
            0f,
            (-th / 2).toFloat(),
            (-th / 2 + circleOffset).toFloat(),
            (-th / 2).toFloat(),
            (-th / 2).toFloat()
        )
        toastPath.rCubicTo(
            0f,
            -circleOffset.toFloat(),
            (-circleOffset + th / 2).toFloat(),
            (-th / 2).toFloat(),
            (th / 2).toFloat(),
            (-th / 2).toFloat()
        )
        c.drawCircle(spinnerRect.centerX(), spinnerRect.centerY(), iconBounds.height() / 1.9f, backPaint)
        c.drawPath(toastPath, backPaint)
        val thb = th - borderOffset * 2
        toastPath.reset()
        toastPath.moveTo(leftMargin + th / 2, borderOffset.toFloat())
        toastPath.rLineTo(ws * (IMAGE_WIDTH + MAX_TEXT_WIDTH), 0f)
        toastPath.rCubicTo(
            circleOffset.toFloat(),
            0f,
            (thb / 2).toFloat(),
            (thb / 2 - circleOffset).toFloat(),
            (thb / 2).toFloat(),
            (thb / 2).toFloat()
        )
        toastPath.rCubicTo(
            0f,
            circleOffset.toFloat(),
            (circleOffset - thb / 2).toFloat(),
            (thb / 2).toFloat(),
            (-thb / 2).toFloat(),
            (thb / 2).toFloat()
        )
        toastPath.rLineTo(ws * (-IMAGE_WIDTH - MAX_TEXT_WIDTH), 0f)
        toastPath.rCubicTo(
            -circleOffset.toFloat(),
            0f,
            (-thb / 2).toFloat(),
            (-thb / 2 + circleOffset).toFloat(),
            (-thb / 2).toFloat(),
            (-thb / 2).toFloat()
        )
        toastPath.rCubicTo(
            0f,
            -circleOffset.toFloat(),
            (-circleOffset + thb / 2).toFloat(),
            (-thb / 2).toFloat(),
            (thb / 2).toFloat(),
            (-thb / 2).toFloat()
        )
        c.drawPath(toastPath, borderPaint)
        toastPath.reset()
        val prog = va!!.animatedFraction * 6.0f
        var progrot = prog % 2.0f
        var proglength = easeinterpol.getInterpolation(prog % 3f / 3f) * 3f - .75f
        if (proglength > .75f) {
            proglength = .75f - (prog % 3f - 1.5f)
            progrot += (prog % 3f - 1.5f) / 1.5f * 2f
        }
        if (mText.isEmpty()) {
            ws = max(1f - WIDTH_SCALE, 0f)
        }
        c.save()
        c.translate((totalWidth - TOAST_HEIGHT) / 2, 0f)
        super.onDraw(c)
        c.restore()
        if (WIDTH_SCALE > 1f) {
            val icon = if (success) completeicon else failedicon
            val circleProg = WIDTH_SCALE - 1f
            textPaint.alpha = (128 * circleProg + 127).toInt()
            val paddingicon = ((1f - (.25f + .75f * circleProg)) * TOAST_HEIGHT / 2).toInt()
            val completeoff = ((1f - circleProg) * TOAST_HEIGHT / 8).toInt()
            icon.setBounds(
                spinnerRect.left.toInt() + paddingicon,
                spinnerRect.top.toInt() + paddingicon + completeoff,
                spinnerRect.right.toInt() - paddingicon,
                spinnerRect.bottom.toInt() - paddingicon + completeoff
            )
            c.drawCircle(
                leftMargin + TOAST_HEIGHT / 2, (1f - circleProg) * TOAST_HEIGHT / 8 + TOAST_HEIGHT / 2,
                (.25f + .75f * circleProg) * TOAST_HEIGHT / 2, if (success) successPaint else errorPaint
            )
            c.save()
            c.rotate(90 * (1f - circleProg), leftMargin + TOAST_HEIGHT / 2, (TOAST_HEIGHT / 2).toFloat())
            icon.draw(c)
            c.restore()
            prevUpdate = 0
            return
        }
        val yPos = (th / 2 - (textPaint.descent() + textPaint.ascent()) / 2).toInt()
        if (outOfBounds) {
            var shift = 0f
            if (prevUpdate == 0L) {
                prevUpdate = System.currentTimeMillis()
            } else {
                shift = (System.currentTimeMillis() - prevUpdate).toFloat() / 16f * MARQUE_STEP
                if (shift - MAX_TEXT_WIDTH > mTextBounds.width()) {
                    prevUpdate = 0
                }
            }
            c.clipRect(th / 2, 0, th / 2 + MAX_TEXT_WIDTH, TOAST_HEIGHT)
            if (!mLtr || Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && textDirection == TEXT_DIRECTION_ANY_RTL) {
                c.drawText(mText, th / 2 - mTextBounds.width() + shift, yPos.toFloat(), textPaint)
            } else {
                c.drawText(mText, th / 2 - shift + MAX_TEXT_WIDTH, yPos.toFloat(), textPaint)
            }
        } else {
            c.drawText(mText, 0, mText.length, (th / 2 + (MAX_TEXT_WIDTH - mTextBounds.width()) / 2).toFloat(), yPos.toFloat(), textPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            measureWidth(widthMeasureSpec),
            measureHeight(heightMeasureSpec)
        )
    }

    /**
     * Determines the width of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private fun measureWidth(measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize
        } else {
            // Measure the text
            result = IMAGE_WIDTH + MAX_TEXT_WIDTH + TOAST_HEIGHT
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize)
            }
        }
        return result
    }

    /**
     * Determines the height of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private fun measureHeight(measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = TOAST_HEIGHT
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize)
            }
        }
        return result
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(javaClass.simpleName, "detached")
        cleanup()
    }

    fun cleanup() {
        if (cmp != null) {
            cmp!!.removeAllUpdateListeners()
            cmp!!.removeAllListeners()
        }
        if (va != null) {
            va.removeAllUpdateListeners()
            va.removeAllListeners()
        }
        spinnerDrawable!!.stop()
    }

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), resources.displayMetrics).toInt()
    }

    private fun Int.spToPx(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, toFloat(), resources.displayMetrics).toInt()
    }
}