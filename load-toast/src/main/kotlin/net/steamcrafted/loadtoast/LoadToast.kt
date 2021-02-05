package net.steamcrafted.loadtoast

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.nineoldandroids.animation.Animator
import com.nineoldandroids.animation.AnimatorListenerAdapter
import com.nineoldandroids.view.ViewHelper
import com.nineoldandroids.view.ViewPropertyAnimator

class LoadToast {
    private val mView: LoadToastView
    private val mParentView: ViewGroup
    private var mText = ""
    private var mTranslationY = 0
    private var mShowCalled = false
    private var mToastCanceled = false
    private var mInflated = false
    private var mVisible = false
    private var mReAttached = false

    constructor(context: Context?, parentView: View) {
        mView = LoadToastView(context)
        mParentView = parentView as ViewGroup
    }

    constructor(activity: Activity) {
        mView = LoadToastView(activity)
        mParentView = activity.window.decorView as ViewGroup
    }

    private fun cleanup() {
        val childCount = mParentView.childCount
        for (i in childCount downTo 0) {
            if (mParentView.getChildAt(i) is LoadToastView) {
                val ltv = mParentView.getChildAt(i) as LoadToastView
                ltv.cleanup()
                mParentView.removeViewAt(i)
            }
        }
        mInflated = false
        mToastCanceled = false
    }

    fun setTranslationY(pixels: Int): LoadToast {
        mTranslationY = pixels
        return this
    }

    fun setText(message: String): LoadToast {
        mText = message
        mView.setText(mText)
        return this
    }

    fun setTextColor(color: Int): LoadToast {
        mView.setTextColor(color)
        return this
    }

    fun setTextTypeface(typeface: Typeface?): LoadToast {
        mView.setTextTypeface(typeface)
        return this
    }

    fun setTextSizeSp(size: Int): LoadToast {
        mView.setTextSizeSp(size)
        return this
    }

    fun setBackgroundColor(color: Int): LoadToast {
        mView.setBackgroundColor(color)
        return this
    }

    fun setProgressColor(color: Int): LoadToast {
        mView.setProgressColor(color)
        return this
    }

    fun setTextDirection(isLeftToRight: Boolean): LoadToast {
        mView.setTextDirection(isLeftToRight)
        return this
    }

    fun setBorderColor(color: Int): LoadToast {
        mView.setBorderColor(color)
        return this
    }

    fun setBorderWidthPx(width: Int): LoadToast {
        mView.setBorderWidthPx(width)
        return this
    }

    fun setBorderWidthRes(resourceId: Int): LoadToast {
        mView.setBorderWidthRes(resourceId)
        return this
    }

    fun setBorderWidthDp(width: Int): LoadToast {
        mView.setBorderWidthDp(width)
        return this
    }

    fun show(): LoadToast {
        mShowCalled = true
        attach()
        return this
    }

    private fun showInternal() {
        mView.show()
        ViewHelper.setTranslationX(mView, ((mParentView.width - mView.width) / 2).toFloat())
        ViewHelper.setAlpha(mView, 0f)
        ViewHelper.setTranslationY(mView, (-mView.height + mTranslationY).toFloat())
        //mView.setVisibility(View.VISIBLE);
        ViewPropertyAnimator.animate(mView).alpha(1f).translationY((25 + mTranslationY).toFloat())
            .setInterpolator(DecelerateInterpolator())
            .setListener(null)
            .setDuration(300).setStartDelay(0).start()
        mVisible = true
    }

    private fun attach() {
        cleanup()
        mReAttached = true
        mParentView.addView(mView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        ViewHelper.setAlpha(mView, 0f)
        mParentView.postDelayed({
            ViewHelper.setTranslationX(mView, ((mParentView.width - mView.width) / 2).toFloat())
            ViewHelper.setTranslationY(mView, (-mView.height + mTranslationY).toFloat())
            mInflated = true
            if (!mToastCanceled && mShowCalled) showInternal()
        }, 1)
    }

    fun success() {
        if (!mInflated) {
            mToastCanceled = true
            return
        }
        if (mReAttached) {
            mView.success()
            slideUp()
        }
    }

    fun error() {
        if (!mInflated) {
            mToastCanceled = true
            return
        }
        if (mReAttached) {
            mView.error()
            slideUp()
        }
    }

    fun hide() {
        if (!mInflated) {
            mToastCanceled = true
            return
        }
        if (mReAttached) {
            slideUp(0)
        }
    }

    private fun slideUp(startDelay: Int = 1000) {
        mReAttached = false
        ViewPropertyAnimator.animate(mView).setStartDelay(startDelay.toLong()).alpha(0f)
            .translationY((-mView.height + mTranslationY).toFloat())
            .setInterpolator(AccelerateInterpolator())
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    if (!mReAttached) {
                        cleanup()
                    }
                }
            })
            .start()
        mVisible = false
    }
}