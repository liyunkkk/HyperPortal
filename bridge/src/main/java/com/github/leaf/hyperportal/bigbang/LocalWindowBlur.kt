package com.github.leaf.hyperportal.bigbang

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.View

/**
 * Hardware SurfaceFlinger backdrop blur confined strictly to a view's rounded
 * rectangle via ViewRootImpl.createBackgroundBlurDrawable() (matching
 * TransferDockPanel / TransferDockCapsuleView), avoiding full-window
 * FLAG_BLUR_BEHIND compatibility issues.
 */
internal object LocalWindowBlur {
    fun installBackdrop(
        view: View,
        blurRadiusPx: Int,
        cornerRadiusPx: Float,
        onResult: (Boolean) -> Unit,
    ) {
        if (!view.isAttachedToWindow) {
            onResult(false)
            return
        }
        try {
            val blurDrawable = createOrUpdateBlurDrawable(view, blurRadiusPx, cornerRadiusPx)
                ?: run {
                    onResult(false)
                    return
                }
            if (view.background !== blurDrawable) {
                view.background = blurDrawable
            }
            onResult(true)
        } catch (_: Throwable) {
            view.background = null
            onResult(false)
        }
    }

    fun clear(view: View) {
        view.background = null
        view.tag = null
    }

    fun installLayeredBackground(
        view: View,
        blurRadiusPx: Int,
        cornerRadiusPx: Float,
        frostedOverlay: Drawable,
        fallbackOverlay: Drawable,
    ) {
        if (!view.isAttachedToWindow) {
            if (view.background !== fallbackOverlay) {
                view.background = fallbackOverlay
            }
            return
        }
        try {
            val blurDrawable = createOrUpdateBlurDrawable(view, blurRadiusPx, cornerRadiusPx)
            if (blurDrawable != null) {
                view.background = LayerDrawable(arrayOf(blurDrawable, frostedOverlay))
            } else {
                view.background = fallbackOverlay
            }
        } catch (_: Throwable) {
            view.background = fallbackOverlay
        }
    }

    fun attachLayeredBackground(
        view: View,
        blurRadiusPx: Int,
        cornerRadiusPx: Float,
        overlayProvider: () -> Pair<Drawable, Drawable>,
    ) {
        val applyNow = {
            val (frosted, fallback) = overlayProvider()
            installLayeredBackground(
                view = view,
                blurRadiusPx = blurRadiusPx,
                cornerRadiusPx = cornerRadiusPx,
                frostedOverlay = frosted,
                fallbackOverlay = fallback,
            )
        }
        if (view.isAttachedToWindow) {
            view.post {
                if (view.isAttachedToWindow) {
                    applyNow()
                }
            }
        } else {
            val (_, fallback) = overlayProvider()
            view.background = fallback
        }
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.post {
                    if (v.isAttachedToWindow) {
                        applyNow()
                    }
                }
            }

            override fun onViewDetachedFromWindow(v: View) {
                clear(v)
            }
        })
    }

    private fun createOrUpdateBlurDrawable(
        view: View,
        blurRadiusPx: Int,
        cornerRadiusPx: Float,
    ): Drawable? {
        val existing = view.tag as? BlurHandle
        val handle = existing ?: run {
            val getViewRootImpl = View::class.java.getDeclaredMethod("getViewRootImpl").apply {
                isAccessible = true
            }
            val viewRoot = getViewRootImpl.invoke(view) ?: return null
            val create = viewRoot.javaClass.getMethod("createBackgroundBlurDrawable")
            val drawable = create.invoke(viewRoot) as? Drawable ?: return null
            val setRadius = drawable.javaClass.getMethod("setBlurRadius", Int::class.javaPrimitiveType)
            val setCorner = drawable.javaClass.getMethod("setCornerRadius", Float::class.javaPrimitiveType)
            val setColor = drawable.javaClass.getMethod("setColor", Int::class.javaPrimitiveType)
            BlurHandle(drawable, setRadius, setCorner, setColor).also {
                view.tag = it
            }
        }
        if (handle.lastBlurRadiusPx != blurRadiusPx || handle.lastCornerRadiusPx != cornerRadiusPx) {
            handle.setBlurRadius.invoke(handle.drawable, blurRadiusPx)
            handle.setCornerRadius.invoke(handle.drawable, cornerRadiusPx)
            handle.setColor.invoke(handle.drawable, Color.TRANSPARENT)
            handle.lastBlurRadiusPx = blurRadiusPx
            handle.lastCornerRadiusPx = cornerRadiusPx
            handle.drawable.invalidateSelf()
        }
        return handle.drawable
    }

    private class BlurHandle(
        val drawable: Drawable,
        val setBlurRadius: java.lang.reflect.Method,
        val setCornerRadius: java.lang.reflect.Method,
        val setColor: java.lang.reflect.Method,
        var lastBlurRadiusPx: Int = -1,
        var lastCornerRadiusPx: Float = -1f,
    )
}
