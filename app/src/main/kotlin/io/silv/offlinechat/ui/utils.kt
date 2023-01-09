package io.silv.offlinechat.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import java.lang.reflect.Field

fun TextView.setCursorDrawableColor(@ColorInt color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        textCursorDrawable = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(color, color))
            .apply { setSize(2.spToPx(context).toInt(), textSize.toInt()) }
        return
    }

    try {
        val editorField = TextView::class.java.getFieldByName("mEditor")
        val editor = editorField?.get(this) ?: this
        val editorClass: Class<*> = if (editorField != null) editor.javaClass else TextView::class.java
        val cursorRes = TextView::class.java.getFieldByName("mCursorDrawableRes")?.get(this) as? Int ?: return

        val tintedCursorDrawable = ContextCompat.getDrawable(context, cursorRes)?.tinted(color) ?: return

        val cursorField = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            editorClass.getFieldByName("mDrawableForCursor")
        } else {
            null
        }
        if (cursorField != null) {
            cursorField.set(editor, tintedCursorDrawable)
        } else {
            editorClass.getFieldByName("mCursorDrawable", "mDrawableForCursor")
                ?.set(editor, arrayOf(tintedCursorDrawable, tintedCursorDrawable))
        }
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

fun Class<*>.getFieldByName(vararg name: String): Field? {
    name.forEach {
        runCatching {
            return this.getDeclaredField(it).apply { isAccessible = true }
        }
    }
    return null
}

fun Drawable.tinted(@ColorInt color: Int): Drawable = when (this) {
    is VectorDrawableCompat -> {
        this.apply { setTintList(ColorStateList.valueOf(color)) }
    }
    is VectorDrawable -> {
        this.apply { setTintList(ColorStateList.valueOf(color)) }
    }
    else -> {
        DrawableCompat.wrap(this)
            .also { DrawableCompat.setTint(it, color) }
            .let { DrawableCompat.unwrap(it) }
    }
}

fun Number.spToPx(context: Context? = null): Float {
    val res = context?.resources ?: android.content.res.Resources.getSystem()
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), res.displayMetrics)
}

