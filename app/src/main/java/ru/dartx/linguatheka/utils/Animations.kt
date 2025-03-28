package ru.dartx.linguatheka.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.core.view.isGone

object Animations {
    fun expand(viewToExpand: View, viewToRotate: View, width: Int) {
        val shortAnimationDuration =
            viewToExpand.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        val height = viewToExpand.context.resources.displayMetrics.heightPixels
        val widthSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(width), MeasureSpec.AT_MOST)
        val heightSpec =
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(height), MeasureSpec.AT_MOST)

        viewToExpand.measure(widthSpec, heightSpec)
        val actualHeight = viewToExpand.measuredHeight
        viewToExpand.layoutParams.height = 0
        viewToExpand.visibility = View.VISIBLE
        val durationValue = (actualHeight / viewToExpand.context.resources.displayMetrics.density).toLong() * shortAnimationDuration / 40

        val animator = ValueAnimator.ofInt(0, actualHeight).apply {
            duration = durationValue
            addUpdateListener {
                viewToExpand.layoutParams.height = it.animatedValue as Int
                viewToExpand.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    viewToExpand.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    viewToExpand.requestLayout()
                }
            })
        }
        viewToRotate.animate().setDuration(durationValue).rotation(180F)
        animator.start()
    }

    fun collapse(viewToCollapse: View, viewToRotate: View) {
        val shortAnimationDuration =
            viewToCollapse.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        val actualHeight = viewToCollapse.measuredHeight
        val durationValue = (actualHeight / viewToCollapse.context.resources.displayMetrics.density).toLong() * shortAnimationDuration / 40
        val animator = ValueAnimator.ofInt(actualHeight, 0).apply {
            duration = durationValue
            addUpdateListener {
                viewToCollapse.layoutParams.height = it.animatedValue as Int
                viewToCollapse.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    viewToCollapse.isGone = true
                    viewToCollapse.requestLayout()
                }
            })
        }
        viewToRotate.animate().setDuration(durationValue).rotation(0F)
        animator.start()
    }
}