package ru.dartx.linguatheka.utils

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation

object Animations {
    fun expand(view: View) {
        val animation = expandAction(view)
        view.startAnimation(animation)
    }

    private fun expandAction(view: View): Animation {
        val shortAnimationDuration =
            view.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val actualHeight = view.measuredHeight
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE
        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                //super.applyTransformation(interpolatedTime, t)
                view.layoutParams.height =
                    if (interpolatedTime == 1F) ViewGroup.LayoutParams.WRAP_CONTENT
                    else (actualHeight * interpolatedTime).toInt()
                view.requestLayout()
            }
        }
        animation.duration =
            (actualHeight / view.context.resources.displayMetrics.density).toLong() * shortAnimationDuration / 40
        return animation
    }

    fun collapse(view: View) {
        val shortAnimationDuration =
            view.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        val actualHeight = view.measuredHeight
        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                //super.applyTransformation(interpolatedTime, t)
                if (interpolatedTime == 1F) view.visibility = View.GONE
                else {
                    view.layoutParams.height =
                        actualHeight - (actualHeight * interpolatedTime).toInt()
                    view.requestLayout()
                }
            }
        }
        animation.duration =
            (actualHeight / view.context.resources.displayMetrics.density).toLong() * shortAnimationDuration / 40
        view.startAnimation(animation)
    }
}