package com.flatcode.littleplayer.Unit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.flatcode.littleplayer.R
import jp.wasabeef.glide.transformations.BlurTransformation

object VOID {
    fun IntentClear(context: Context, c: Class<*>?) {
        val intent = Intent(context, c)
        context.startActivity(intent)
    }

    fun Intent1(context: Context, c: Class<*>?) {
        val intent = Intent(context, c)
        context.startActivity(intent)
    }

    fun IntentExtra(context: Context, c: Class<*>?, key: String?, value: String?) {
        val intent = Intent(context, c)
        intent.putExtra(key, value)
        context.startActivity(intent)
    }

    fun IntentExtraInt(context: Context, c: Class<*>?, key: String?, value: Int) {
        val intent = Intent(context, c)
        intent.putExtra(key, value)
        context.startActivity(intent)
    }

    fun IntentExtra2(
        context: Context, c: Class<*>?, key: String?, value: String?, key2: String?, value2: String?
    ) {
        val intent = Intent(context, c)
        intent.putExtra(key, value)
        intent.putExtra(key2, value2)
        context.startActivity(intent)
    }

    fun IntentExtra2Int(
        context: Context, c: Class<*>?, key: String?, value: String?, key2: String?, value2: Int
    ) {
        val intent = Intent(context, c)
        intent.putExtra(key, value)
        intent.putExtra(key2, value2)
        context.startActivity(intent)
    }

    fun Glide(context: Context?, Url: String?, Image: ImageView) {
        try {
            Glide.with(context!!).load(Url).placeholder(R.color.image_profile).into(Image)
        } catch (e: Exception) {
            Image.setImageResource(R.color.image_profile)
        }
    }

    fun GlideByte(context: Context, Url: ByteArray?, Image: ImageView) {
        try {
            if (Url != null) Glide.with(context).load(Url).placeholder(R.color.image_profile)
                .into(Image)
            else Glide.with(context).load(R.drawable.logo).into(Image)
        } catch (_: java.lang.Exception) {
            Image.setImageResource(R.drawable.logo)
        }
    }

    fun GlideBitmap(context: Context, Url: Bitmap?, Image: ImageView) {
        try {
            if (Url != null) Glide.with(context).load(Url).placeholder(R.color.image_profile)
                .into(Image)
            else Glide.with(context).load(R.drawable.logo).into(Image)
        } catch (_: java.lang.Exception) {
            Image.setImageResource(R.drawable.logo)
        }
    }

    fun GlideBlurBitmap(context: Context, Url: Bitmap?, Image: ImageView, level: Int) {
        try {
            if (Url != null) Glide.with(context).load(Url).placeholder(R.color.image_profile)
                .apply(bitmapTransform(BlurTransformation(level))).into(Image)
            else Glide.with(context).load(R.drawable.logo)
                .apply(bitmapTransform(BlurTransformation(level))).into(Image)
        } catch (_: java.lang.Exception) {
            Image.setImageResource(R.drawable.logo)
        }
    }

    fun GlideBlurByte(context: Context, Url: ByteArray?, Image: ImageView, level: Int) {
        try {
            if (Url != null) Glide.with(context).load(Url).placeholder(R.color.image_profile)
                .apply(bitmapTransform(BlurTransformation(level))).into(Image)
            else Glide.with(context).load(R.drawable.logo)
                .apply(bitmapTransform(BlurTransformation(level))).into(Image)
        } catch (_: java.lang.Exception) {
            Image.setImageResource(R.drawable.logo)
        }
    }
}