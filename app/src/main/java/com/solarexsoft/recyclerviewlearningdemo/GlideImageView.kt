/*
 * Copyright (C) 2018 Johnny Shieh Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.solarexsoft.recyclerviewlearningdemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.*
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.*
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.util.Util
import java.nio.ByteBuffer
import java.security.MessageDigest

@SuppressLint("CheckResult")
open class GlideImageView : AppCompatImageView {

    companion object {
        const val TAG = "GlideImageView"
    }

    var isCircular = false
    var cornerRadius = 0
    var cornerType = CornerType.ALL

    var decodeFormat = Bitmap.Config.ARGB_8888
    var skipCache = false
    /**
     * When glide load image, it must use placeholder,
     * so if enablePlaceholder is false, will use deep copy current drawable as placeholder
     */
    var enablePlaceholder = true
    var transition: TransitionOptions<DrawableTransitionOptions, Drawable>? = DrawableTransitionOptions.withCrossFade()
    var listener: ImageLoadListener? = null
    var extraOption: ((options: RequestOptions) -> Unit)? = null

    @ColorInt
    var placeHolderColor = 0
        set(value) {
            field = value
            if (value != 0) {
                placeHolderDrawableRes = 0
            }
        }
    @ColorInt
    var errorHolderColor = 0
        set(value) {
            field = value
            if (value != 0) {
                errorHolderDrawableRes = 0
            }
        }
    @DrawableRes
    var placeHolderDrawableRes = 0
        set(value) {
            field = value
            if (value != 0) {
                placeHolderColor = 0
            }
        }
    @DrawableRes
    var errorHolderDrawableRes = 0
        set(value) {
            field = value
            if (value != 0) {
                errorHolderColor = 0
            }
        }


    private var uri: Any? = null
    private var canceled = false

//    private var blurBitmapTransform: BlurBitmapTransform? = null

    interface ImageLoadListener {
        fun onLoadFailed()
        fun onLoadSuccess(resource: Drawable?)
    }

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GlideImageView)
        placeHolderColor = typedArray.getColor(R.styleable.GlideImageView_placeHolderColor, 0)
        placeHolderDrawableRes = typedArray.getResourceId(R.styleable.GlideImageView_placeHolderSrc, 0)
        errorHolderColor = typedArray.getColor(R.styleable.GlideImageView_errorHolderColor, 0)
        errorHolderDrawableRes = typedArray.getResourceId(R.styleable.GlideImageView_errorHolderSrc, 0)
        cornerRadius = typedArray.getDimensionPixelOffset(R.styleable.GlideImageView_cornerRadius, 0)
        isCircular = typedArray.getBoolean(R.styleable.GlideImageView_isCircular, false)
        skipCache = typedArray.getBoolean(R.styleable.GlideImageView_skipCache, false)
        val format = typedArray.getInteger(R.styleable.GlideImageView_decodeFormat, 0)
        decodeFormat = when (format) {
            1 -> Bitmap.Config.RGB_565
            else -> Bitmap.Config.ARGB_8888
        }
        val type = typedArray.getInteger(R.styleable.GlideImageView_cornerType, 0)
        cornerType = when (type) {
            1 -> CornerType.LEFT_TOP
            2 -> CornerType.LEFT_BOTTOM
            3 -> CornerType.RIGHT_TOP
            4 -> CornerType.RIGHT_BOTTOM
            5 -> CornerType.LEFT
            6 -> CornerType.TOP
            7 -> CornerType.RIGHT
            8 -> CornerType.BOTTOM
            else -> CornerType.ALL
        }

        typedArray.recycle()
    }


    /**
     * uri can be the following type:
     * 1) "https://xxx.jpg"
     * 2) R.drawable.pic
     * 3) File(imagePath)
     * 4) "file:///android_asset/f001.gif"
     * 5) "android.resource://packeagename/${R.drawable.fa}"
     * 6) byteArray
     * 7) CustomGlideUrl()
     */
    fun load(uri: Any?, size: Point? = null) {
        if (this.uri == uri) return
        if (null == uri) {
            setImageDrawable(null)
            return
        }
        canceled = false
        this.uri = uri
        loadInternal(uri, size)
    }

    fun cancel() {
        canceled = true
    }

    fun enableBlur(radius: Int) {
//        blurBitmapTransform = BlurBitmapTransform(radius)
    }

    private fun loadInternal(uri: Any, size: Point?) {
        val rb = Glide.with(context).load(uri)
        handleListener(rb)

        val option = RequestOptions()

        if (size != null) {
            option.override(size.x, size.y)
        } else if (measuredWidth != 0 && measuredHeight != 0) {
            option.override(measuredWidth, measuredHeight)
        }

        // placeholder & errorholder
        handleHolder(option)

        // skipCache
        handleSkipCache(option)

        // decodeFormat
        handleDecodeFormat(option)

        // transformation
        handleTransformation(option)

        // extra config
        extraOption?.invoke(option)

        rb.apply(option)

        transition?.let { rb.transition(it) }
        rb.into(this)
    }

    private fun handleHolder(option: RequestOptions) {
        var placeHolder: Drawable? = null
        var errorHolder: Drawable? = null
        if (enablePlaceholder && placeHolderColor != 0) {
            placeHolder = GradientDrawable().apply { setColor(placeHolderColor) }
        }
        if (errorHolderColor != 0) {
            errorHolder = GradientDrawable().apply { setColor(errorHolderColor) }
        }
        if (enablePlaceholder && placeHolderDrawableRes != 0) {
            placeHolder = RoundedBitmapDrawableFactory.create(
                resources,
                BitmapFactory.decodeResource(resources, placeHolderDrawableRes)
            )
        }
        if (errorHolderDrawableRes != 0) {
            errorHolder = RoundedBitmapDrawableFactory.create(
                resources,
                BitmapFactory.decodeResource(resources, errorHolderDrawableRes)
            )
        }

        if (cornerRadius > 0 && cornerType == CornerType.ALL) {
            // if holder is drawable res, cornerRadius may be useful because of scale type
            when (placeHolder) {
                is GradientDrawable -> placeHolder.cornerRadius = cornerRadius.toFloat()
            }
            when (errorHolder) {
                is GradientDrawable -> errorHolder.cornerRadius = cornerRadius.toFloat()
            }
        }
        if (isCircular) {
            when (placeHolder) {
                is GradientDrawable -> placeHolder.shape = GradientDrawable.OVAL
                is RoundedBitmapDrawable -> placeHolder.isCircular = true
            }
            when (errorHolder) {
                is GradientDrawable -> errorHolder.shape = GradientDrawable.OVAL
                is RoundedBitmapDrawable -> errorHolder.isCircular = true
            }
        }
        if (!enablePlaceholder) {
            /*
            为决定某个资源是否正在被使用，以及什么时候可以安全地被重用，Glide 为每个资源保持了一个引用计数。

            增加引用计数：每次调用 into() 来加载一个资源，这个资源的引用计数会被加一。如果相同的资源被加载到两个不同的 Target，则在两个加载都完成后，它的引用计数将会为二。
            减少引用计数：
                1. 在加载资源的 View 或 Target 上调用 clear() 。
                2. 在这个View 或 Target 上调用对另一个资源请求的 into 方法。

            所以不能用上一个 drawable 作为 TransitionDrawable 的 first layer，因为此时它的引用计数为 0，使用它是不安全，会导致 Canvas: trying to use a recycled bitmap
            这里的解决方式是：根据上一个 drawable，创建一个新的 BitmapDrawable
            */
            val currentDrawable = drawable
//            BitmapUtils.drawableToBitmap(currentDrawable)?.let { placeHolder = BitmapDrawable(resources, it) }
        }

        if (null != placeHolder) {
            option.placeholder(placeHolder)
        }
        if (null != errorHolder) {
            option.error(errorHolder)
        }
    }

    private fun handleSkipCache(option: RequestOptions) {
        if (skipCache) {
            option.skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
        }
    }

    private fun handleDecodeFormat(option: RequestOptions) {
        when (decodeFormat) {
            Bitmap.Config.ARGB_8888 -> option.format(DecodeFormat.DEFAULT)
            else -> {
                option.format(DecodeFormat.PREFER_RGB_565)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    option.disallowHardwareConfig()
                }
            }
        }
    }

    private fun handleTransformation(option: RequestOptions) {
        val transformationList = mutableListOf<Transformation<Bitmap>>()
        when (scaleType) {
            ScaleType.CENTER_CROP -> transformationList.add(CenterCrop())
            ScaleType.CENTER_INSIDE, ScaleType.FIT_XY -> transformationList.add(CenterInside())
            ScaleType.FIT_CENTER, ScaleType.FIT_START, ScaleType.FIT_END -> transformationList.add(FitCenter())
            else -> {
            }
        }
        if (isCircular) {
            transformationList.clear()
            transformationList.add(CircleCrop())
        } else if (cornerRadius > 0) {
            if (cornerType == CornerType.ALL) {
                transformationList.add(RoundedCorners(cornerRadius))
            } else {
                transformationList.add(AllRoundCorners(cornerRadius, cornerType))
            }
        }
//        blurBitmapTransform?.let {
//            transformationList.add(it)
//        }

        if (transformationList.isNotEmpty()) {
            option.transform(MultiTransformation(transformationList))
        }
    }

    private fun handleListener(rb: RequestBuilder<Drawable>) {
        rb.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                Log.d(TAG, "${this@GlideImageView.uri} -> ${this@GlideImageView.canceled} load failed")
                listener?.onLoadFailed()
                if (canceled) return true
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                Log.d(TAG, "${this@GlideImageView.uri} -> ${this@GlideImageView.canceled} load success")
                listener?.onLoadSuccess(resource)
                if (canceled) return true
                return false
            }
        })
    }

    enum class CornerType {
        /** 所有角  */
        ALL,
        /** 左上  */
        LEFT_TOP,
        /** 左下  */
        LEFT_BOTTOM,
        /** 右上  */
        RIGHT_TOP,
        /** 右下  */
        RIGHT_BOTTOM,
        /** 左侧  */
        LEFT,
        /** 右侧  */
        RIGHT,
        /** 下侧  */
        BOTTOM,
        /** 上侧  */
        TOP
    }

    /**
    支持 [CornerType] 的圆角变换
     */
    class AllRoundCorners(
        @IntRange(from = 1) val radius: Int,
        val type: CornerType
    ) : BitmapTransformation() {

        companion object {
            private const val ID = "com.johnnyshieh.bitmap.AllRoundCorners"
            private val ID_BTYES = ID.toByteArray(Key.CHARSET)

            private fun getAlphaSafeConfig(bitmap: Bitmap): Bitmap.Config {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Afun short circuiting the sdk check.
                    if (Bitmap.Config.RGBA_F16 == bitmap.config) { // NOPMD
                        return Bitmap.Config.RGBA_F16
                    }
                }

                return Bitmap.Config.ARGB_8888
            }

            private fun getAlphaSafeBitmap(pool: BitmapPool, maybeAlphaSafe: Bitmap): Bitmap {
                val safeConfig = getAlphaSafeConfig(maybeAlphaSafe)
                if (safeConfig == maybeAlphaSafe.config) {
                    return maybeAlphaSafe
                }

                val argbBitmap = pool.get(maybeAlphaSafe.width, maybeAlphaSafe.height, safeConfig)
                Canvas(argbBitmap).drawBitmap(
                    maybeAlphaSafe,
                    0f /*left*/,
                    0f /*top*/,
                    null /*paint*/
                )

                // We now own this Bitmap. It's our responsibility to replace it in the pool outside this method
                // when we're finished with it.
                return argbBitmap
            }
        }

        private val fRadius = radius.toFloat()
        private val diameter = 2 * fRadius

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update(ID_BTYES)
            messageDigest.update(ByteBuffer.allocate(4).putInt(radius).array())
            messageDigest.update(type.name.toByteArray(Key.CHARSET))
        }

        override fun transform(
            pool: BitmapPool,
            inBitmap: Bitmap,
            outWidth: Int,
            outHeight: Int
        ): Bitmap {
            val safeConfig = getAlphaSafeConfig(inBitmap)
            val toTransform = getAlphaSafeBitmap(pool, inBitmap)
            val result = pool.get(toTransform.width, toTransform.height, safeConfig)

            result.setHasAlpha(true)
            val shader = BitmapShader(
                toTransform, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
            val paint = Paint()
            paint.isAntiAlias = true
            paint.shader = shader
            val canvas = Canvas(result)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val width = result.width.toFloat()
            val height = result.height.toFloat()
            when (type) {
                CornerType.LEFT_TOP -> drawLeftTopCorner(canvas, paint, width, height)
                CornerType.LEFT_BOTTOM -> drawLeftBottomCorner(canvas, paint, width, height)
                CornerType.RIGHT_TOP -> drawRightTopCorner(canvas, paint, width, height)
                CornerType.RIGHT_BOTTOM -> drawRightBottomCorner(canvas, paint, width, height)
                CornerType.LEFT -> drawLeftCorner(canvas, paint, width, height)
                CornerType.TOP -> drawTopCorner(canvas, paint, width, height)
                CornerType.BOTTOM -> drawBottomCorner(canvas, paint, width, height)
                CornerType.RIGHT -> drawRightCorner(canvas, paint, width, height)
                else -> canvas.drawRoundRect(RectF(0f, 0f, width, height), fRadius, fRadius, paint)
            }
            canvas.setBitmap(null)
            if (toTransform != inBitmap) {
                pool.put(toTransform)
            }

            return result
        }

        /**
         * 画左上角
         */
        private fun drawLeftTopCorner(canvas: Canvas, paint: Paint, width: Float, height: Float) {
            canvas.drawRect(RectF(fRadius, 0f, width, height), paint)
            canvas.drawRect(RectF(0f, fRadius, fRadius, height), paint)
            canvas.drawArc(RectF(0f, 0f, diameter, diameter), 180f, 90f, true, paint)
        }

        /**
         * 画左下角
         */
        private fun drawLeftBottomCorner(canvas: Canvas, paint: Paint, width: Float, height: Float) {
            canvas.drawRect(RectF(0f, 0f, width, height - fRadius), paint)
            canvas.drawRect(RectF(fRadius, height - fRadius, width, height), paint)
            canvas.drawArc(RectF(0f, height - diameter, diameter, height), 90f, 90f, true, paint)
        }

        /**
         * 画右上角
         */
        private fun drawRightTopCorner(canvas: Canvas, paint: Paint, width: Float, height: Float) {
            canvas.drawRect(RectF(0f, 0f, width - fRadius, height), paint)
            canvas.drawRect(RectF(width - fRadius, fRadius, width, height), paint)
            canvas.drawArc(RectF(width - diameter, 0f, width, diameter), 270f, 90f, true, paint)
        }

        /**
         * 画右下角
         */
        private fun drawRightBottomCorner(canvas: Canvas, paint: Paint, width: Float, height: Float) {
            canvas.drawRect(RectF(0f, 0f, width, height - fRadius), paint)
            canvas.drawRect(RectF(0f, height - fRadius, width - fRadius, height), paint)
            canvas.drawArc(RectF(width - diameter, height - diameter, width, height), 0f, 90f, true, paint)
        }

        /**
         * 画左角
         */
        private fun drawLeftCorner(canvas: Canvas, paint: Paint, width: Float, height: Float) {
            canvas.drawRect(RectF(fRadius, 0f, width, height), paint)
            canvas.drawRect(RectF(0f, fRadius, fRadius, height - fRadius), paint)
            canvas.drawArc(RectF(0f, 0f, diameter, diameter), 180f, 90f, true, paint)
            canvas.drawArc(RectF(0f, height - diameter, diameter, height), 90f, 90f, true, paint)
        }

        /**
         * 画上角
         */
        private fun drawTopCorner(canvas: Canvas, paint: Paint, width: Float, height: Float) {
            canvas.drawRect(RectF(0f, fRadius, width, height), paint)
            canvas.drawRect(RectF(fRadius, 0f, width - fRadius, fRadius), paint)
            canvas.drawArc(RectF(0f, 0f, diameter, diameter), 180f, 90f, true, paint)
            canvas.drawArc(RectF(width - diameter, 0f, width, diameter), 270f, 90f, true, paint)
        }

        /**
         * 画下角
         */
        private fun drawBottomCorner(canvas: Canvas, paint: Paint, width: Float, height: Float) {
            canvas.drawRect(RectF(0f, 0f, width, height - fRadius), paint)
            canvas.drawRect(RectF(fRadius, height - fRadius, width - fRadius, height), paint)
            canvas.drawArc(RectF(0f, height - diameter, diameter, height), 90f, 90f, true, paint)
            canvas.drawArc(RectF(width - diameter, height - diameter, width, height), 0f, 90f, true, paint)
        }

        /**
         * 画右角
         */
        private fun drawRightCorner(canvas: Canvas, paint: Paint, width: Float, height: Float) {
            canvas.drawRect(RectF(0f, 0f, width - fRadius, height), paint)
            canvas.drawRect(RectF(width - fRadius, fRadius, width, height - fRadius), paint)
            canvas.drawArc(RectF(width - diameter, 0f, width, diameter), 270f, 90f, true, paint)
            canvas.drawArc(RectF(width - diameter, height - diameter, width, height), 0f, 90f, true, paint)
        }

        override fun equals(other: Any?): Boolean {
            if (other is AllRoundCorners) {
                return radius == other.radius && type == other.type
            }
            return false
        }

        override fun hashCode(): Int {
            var result = Util.hashCode(ID.hashCode())
            result = 31 * result + radius
            result = 31 * result + type.hashCode()
            return result
        }
    }

//    private class BlurBitmapTransform(
//        private val radius: Int
//    ) : BitmapTransformation() {
//        companion object {
//            private val ID = "com.wumii.android.athena.BlurBitmapTransform".toByteArray(Key.CHARSET)
//        }
//
//        override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
//            return DBlur.source(AppHolder.app, toTransform)
//                .mode(BlurConfig.MODE_NATIVE)
//                .radius(radius)
//                .sampling(4)
//                .build()
//                .doBlurSync()
//        }
//
//        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
//            messageDigest.update(ID)
//            messageDigest.update(ByteBuffer.allocate(4).putInt(radius).array())
//        }
//
//        override fun hashCode(): Int {
//            var result = Util.hashCode(ID.hashCode())
//            result = 31 * result + radius
//            return result
//        }
//    }
}