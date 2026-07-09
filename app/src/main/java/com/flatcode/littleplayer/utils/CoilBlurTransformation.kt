@file:Suppress("DEPRECATION")

package com.flatcode.littleplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.core.graphics.createBitmap
import coil.size.Size
import coil.transform.Transformation

class CoilBlurTransformation(private val context: Context, private val radius: Float = 10f) :
    Transformation {

    override val cacheKey: String = "CoilBlurTransformation-$radius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val output =
            createBitmap(input.width, input.height, input.config ?: Bitmap.Config.ARGB_8888)
        val rs = RenderScript.create(context)
        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val allIn = Allocation.createFromBitmap(rs, input)
        val allOut = Allocation.createFromBitmap(rs, output)

        blurScript.setRadius(radius.coerceIn(1f, 25f))
        blurScript.setInput(allIn)
        blurScript.forEach(allOut)
        allOut.copyTo(output)

        rs.destroy()
        return output
    }
}