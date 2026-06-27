package com.flatcode.littleplayer.Unit

import android.content.Context
import android.content.Intent
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.flatcode.littleplayer.R

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

    fun Glide(context: Context?, Url: String?, Image: ImageView) {
        try {
            Glide.with(context!!).load(Url).placeholder(R.color.image_profile).into(Image)
        } catch (e: Exception) {
            Image.setImageResource(R.color.image_profile)
        }
    }
}