package com.khtn.zone.custom.progressbar

import com.khtn.zone.R
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.khtn.zone.BuildConfig
import kotlin.math.cos

class ArcProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {
    private var paint: Paint? = null
    protected var textPaint: Paint? = null
    private val rectF = RectF()
    private var strokeWidth = 0f
    private var suffixTextSize = 0f
    private var bottomTextSize = 0f
    private var bottomText: String? = null
    private var text: String? = null
    private var textSize = 0f
    private var textColor = 0
    private var currentProgress = 0
    private var progress = 0f
    var max = 0
        set(max) {
            if (max > 0) {
                field = max
                invalidate()
            }
        }
    private var finishedStrokeColor = 0
    private var unfinishedStrokeColor = 0
    private var arcAngle = 0f
    private var suffixText: String? = "%"
    private var suffixTextPadding = 0f
    private var typeFace: Typeface? = null
    private val fontResourceId = 0
    private var arcBottomHeight = 0f
    private val default_finished_color = ResourcesCompat.getColor(resources, R.color.blue_250x, null)
    private val default_unfinished_color = ResourcesCompat.getColor(resources, R.color.blue_250, null)
    private val default_text_color = ResourcesCompat.getColor(resources, R.color.grey_100, null)
    private val default_suffix_text_size: Float
    private val default_suffix_padding: Float
    private val default_bottom_text_size: Float
    private val default_stroke_width: Float
    private val default_suffix_text: String
    private val default_max = 100
    private val default_arc_angle = 360 * 0.8f
    private var default_text_size: Float
    private val min_size: Int

    init {
        default_text_size = resources.getDimension(R.dimen.dp18)
        min_size = resources.getDimension(R.dimen.dp100).toInt()
        default_text_size = resources.getDimension(R.dimen.sp40)
        default_suffix_text_size = resources.getDimension(R.dimen.sp15)
        default_suffix_padding = resources.getDimension(R.dimen.dp4)
        default_suffix_text = "%"
        default_bottom_text_size = resources.getDimension(R.dimen.sp10)
        default_stroke_width = resources.getDimension(R.dimen.dp4)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ArcProgress)
        initByAttributes(attributes)
        attributes.recycle()
        initPainters()
    }

    private fun initByAttributes(attributes: TypedArray) {
        finishedStrokeColor =
            attributes.getColor(R.styleable.ArcProgress_arc_finished_color, default_finished_color)
        unfinishedStrokeColor = attributes.getColor(
            R.styleable.ArcProgress_arc_unfinished_color,
            default_unfinished_color
        )
        textColor = attributes.getColor(R.styleable.ArcProgress_arc_text_color, default_text_color)
        textSize = attributes.getDimension(R.styleable.ArcProgress_arc_text_size, default_text_size)
        arcAngle = attributes.getFloat(R.styleable.ArcProgress_arc_angle, default_arc_angle)
        max = attributes.getInt(R.styleable.ArcProgress_arc_max, default_max)
        setProgress(attributes.getFloat(R.styleable.ArcProgress_arc_progress, 0f))
        strokeWidth =
            attributes.getDimension(R.styleable.ArcProgress_arc_stroke_width, default_stroke_width)
        suffixTextSize = attributes.getDimension(
            R.styleable.ArcProgress_arc_suffix_text_size,
            default_suffix_text_size
        )
        suffixText =
            if (TextUtils.isEmpty(attributes.getString(R.styleable.ArcProgress_arc_suffix_text))) default_suffix_text else attributes.getString(
                R.styleable.ArcProgress_arc_suffix_text
            )
        suffixTextPadding = attributes.getDimension(
            R.styleable.ArcProgress_arc_suffix_text_padding,
            default_suffix_padding
        )
        bottomTextSize = attributes.getDimension(
            R.styleable.ArcProgress_arc_bottom_text_size,
            default_bottom_text_size
        )
        bottomText = attributes.getString(R.styleable.ArcProgress_arc_bottom_text)
        initTypeFace(attributes)
    }

    private fun initTypeFace(attributes: TypedArray) {
        if (Build.VERSION.SDK_INT < 26) {
            val fontId = attributes.getResourceId(R.styleable.ArcProgress_arc_suffix_text_font, 0)
            if (fontId != 0) {
                try {
                    typeFace = ResourcesCompat.getFont(context, fontId)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) e.printStackTrace()
                }
            }
        } else {
            typeFace = attributes.getFont(R.styleable.ArcProgress_arc_suffix_text_font)
        }
    }

    protected fun initPainters() {
        textPaint = TextPaint()
        textPaint!!.color = textColor
        textPaint!!.textSize = textSize
        textPaint!!.isAntiAlias = true

        paint = Paint()
        paint!!.color = default_unfinished_color
        paint!!.isAntiAlias = true
        paint!!.strokeWidth = strokeWidth
        paint!!.style = Paint.Style.STROKE
        paint!!.strokeCap = Paint.Cap.ROUND
    }

    override fun invalidate() {
        initPainters()
        super.invalidate()
    }

    fun getStrokeWidth(): Float {
        return strokeWidth
    }

    fun setStrokeWidth(strokeWidth: Float) {
        this.strokeWidth = strokeWidth
        this.invalidate()
    }

    fun getSuffixTextSize(): Float {
        return suffixTextSize
    }

    fun setSuffixTextSize(suffixTextSize: Float) {
        this.suffixTextSize = suffixTextSize
        this.invalidate()
    }

    fun getBottomText(): String? {
        return bottomText
    }

    fun setBottomText(bottomText: String?) {
        this.bottomText = bottomText
        this.invalidate()
    }

    fun getProgress(): Float {
        return progress
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        if (this.progress > max) {
            this.progress %= max.toFloat()
        }
        currentProgress = 0
        invalidate()
    }

    fun getBottomTextSize(): Float {
        return bottomTextSize
    }

    fun setBottomTextSize(bottomTextSize: Float) {
        this.bottomTextSize = bottomTextSize
        this.invalidate()
    }

    fun getText(): String? {
        return text
    }

    /**
     * Setting Central Text to custom String
     */
    fun setText(text: String?) {
        this.text = text
        this.invalidate()
    }

    /**
     * Setting Central Text back to default one (value of the progress)
     */
    fun setDefaultText() {
        text = getProgress().toString()
        invalidate()
    }

    fun getTextSize(): Float {
        return textSize
    }

    fun setTextSize(textSize: Float) {
        this.textSize = textSize
        this.invalidate()
    }

    fun getTextColor(): Int {
        return textColor
    }

    fun setTextColor(textColor: Int) {
        this.textColor = textColor
        this.invalidate()
    }

    fun getFinishedStrokeColor(): Int {
        return finishedStrokeColor
    }

    fun setFinishedStrokeColor(finishedStrokeColor: Int) {
        this.finishedStrokeColor = finishedStrokeColor
        this.invalidate()
    }

    fun getUnfinishedStrokeColor(): Int {
        return unfinishedStrokeColor
    }

    fun setUnfinishedStrokeColor(unfinishedStrokeColor: Int) {
        this.unfinishedStrokeColor = unfinishedStrokeColor
        this.invalidate()
    }

    fun getArcAngle(): Float {
        return arcAngle
    }

    fun setArcAngle(arcAngle: Float) {
        this.arcAngle = arcAngle
        this.invalidate()
    }

    fun getSuffixText(): String? {
        return suffixText
    }

    fun setSuffixText(suffixText: String?) {
        this.suffixText = suffixText
        this.invalidate()
    }

    fun getSuffixTextPadding(): Float {
        return suffixTextPadding
    }

    fun setSuffixTextPadding(suffixTextPadding: Float) {
        this.suffixTextPadding = suffixTextPadding
        this.invalidate()
    }

    override fun getSuggestedMinimumHeight(): Int {
        return min_size
    }

    override fun getSuggestedMinimumWidth(): Int {
        return min_size
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        rectF[strokeWidth / 2f, strokeWidth / 2f, width - strokeWidth / 2f] =
            MeasureSpec.getSize(heightMeasureSpec) - strokeWidth / 2f
        val radius = width / 2f
        val angle = (360 - arcAngle) / 2f
        arcBottomHeight = radius * (1 - cos(angle / 180 * Math.PI)).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val startAngle = 270 - arcAngle / 2f
        val finishedSweepAngle = currentProgress / max.toFloat() * arcAngle
        var finishedStartAngle = startAngle
        if (progress == 0f) finishedStartAngle = 0.01f
        paint!!.color = unfinishedStrokeColor
        canvas.drawArc(rectF, startAngle, arcAngle, false, paint!!)
        paint!!.color = finishedStrokeColor
        canvas.drawArc(rectF, finishedStartAngle, finishedSweepAngle, false, paint!!)
        val text = currentProgress.toString()
        if (typeFace != null) textPaint!!.typeface = typeFace
        if (!TextUtils.isEmpty(text)) {
            textPaint!!.color = textColor
            textPaint!!.textSize = textSize
            val textHeight = textPaint!!.descent() + textPaint!!.ascent()
            val textBaseline = (height - textHeight) / 2.0f
            canvas.drawText(
                text,
                (width - textPaint!!.measureText(text)) / 2.0f,
                textBaseline,
                textPaint!!
            )
            textPaint!!.textSize = suffixTextSize
            val suffixHeight = textPaint!!.descent() + textPaint!!.ascent()
            canvas.drawText(
                suffixText!!,
                width / 2.0f + textPaint!!.measureText(text) + suffixTextPadding,
                textBaseline + textHeight - suffixHeight,
                textPaint!!
            )
        }
        if (arcBottomHeight == 0f) {
            val radius = width / 2f
            val angle = (360 - arcAngle) / 2f
            arcBottomHeight = radius * (1 - cos(angle / 180 * Math.PI)).toFloat()
        }
        if (!TextUtils.isEmpty(getBottomText())) {
            textPaint!!.textSize = bottomTextSize
            val bottomTextBaseline =
                height - arcBottomHeight - (textPaint!!.descent() + textPaint!!.ascent()) / 2
            canvas.drawText(
                getBottomText()!!,
                (width - textPaint!!.measureText(getBottomText())) / 2.0f,
                bottomTextBaseline,
                textPaint!!
            )
        }
        if (currentProgress < progress) {
            currentProgress++
            invalidate()
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState())
        bundle.putFloat(INSTANCE_STROKE_WIDTH, getStrokeWidth())
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_SIZE, getSuffixTextSize())
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_PADDING, getSuffixTextPadding())
        bundle.putFloat(INSTANCE_BOTTOM_TEXT_SIZE, getBottomTextSize())
        bundle.putString(INSTANCE_BOTTOM_TEXT, getBottomText())
        bundle.putFloat(INSTANCE_TEXT_SIZE, getTextSize())
        bundle.putInt(INSTANCE_TEXT_COLOR, getTextColor())
        bundle.putFloat(INSTANCE_PROGRESS, getProgress())
        bundle.putInt(INSTANCE_MAX, max)
        bundle.putInt(INSTANCE_FINISHED_STROKE_COLOR, getFinishedStrokeColor())
        bundle.putInt(INSTANCE_UNFINISHED_STROKE_COLOR, getUnfinishedStrokeColor())
        bundle.putFloat(INSTANCE_ARC_ANGLE, getArcAngle())
        bundle.putString(INSTANCE_SUFFIX, getSuffixText())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            strokeWidth = state.getFloat(INSTANCE_STROKE_WIDTH)
            suffixTextSize = state.getFloat(INSTANCE_SUFFIX_TEXT_SIZE)
            suffixTextPadding = state.getFloat(INSTANCE_SUFFIX_TEXT_PADDING)
            bottomTextSize = state.getFloat(INSTANCE_BOTTOM_TEXT_SIZE)
            bottomText = state.getString(INSTANCE_BOTTOM_TEXT)
            textSize = state.getFloat(INSTANCE_TEXT_SIZE)
            textColor = state.getInt(INSTANCE_TEXT_COLOR)
            max = state.getInt(INSTANCE_MAX)
            setProgress(state.getFloat(INSTANCE_PROGRESS))
            finishedStrokeColor = state.getInt(INSTANCE_FINISHED_STROKE_COLOR)
            unfinishedStrokeColor = state.getInt(INSTANCE_UNFINISHED_STROKE_COLOR)
            suffixText = state.getString(INSTANCE_SUFFIX)
            initPainters()
            super.onRestoreInstanceState(state.getParcelable(INSTANCE_STATE))
            return
        }
        super.onRestoreInstanceState(state)
    }

    companion object {
        private const val INSTANCE_STATE = "saved_instance"
        private const val INSTANCE_STROKE_WIDTH = "stroke_width"
        private const val INSTANCE_SUFFIX_TEXT_SIZE = "suffix_text_size"
        private const val INSTANCE_SUFFIX_TEXT_PADDING = "suffix_text_padding"
        private const val INSTANCE_BOTTOM_TEXT_SIZE = "bottom_text_size"
        private const val INSTANCE_BOTTOM_TEXT = "bottom_text"
        private const val INSTANCE_TEXT_SIZE = "text_size"
        private const val INSTANCE_TEXT_COLOR = "text_color"
        private const val INSTANCE_PROGRESS = "progress"
        private const val INSTANCE_MAX = "max"
        private const val INSTANCE_FINISHED_STROKE_COLOR = "finished_stroke_color"
        private const val INSTANCE_UNFINISHED_STROKE_COLOR = "unfinished_stroke_color"
        private const val INSTANCE_ARC_ANGLE = "arc_angle"
        private const val INSTANCE_SUFFIX = "suffix"
    }
}