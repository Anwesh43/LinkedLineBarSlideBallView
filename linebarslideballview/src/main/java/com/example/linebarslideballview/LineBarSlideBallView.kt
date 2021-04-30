package com.example.linebarslideballview

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.content.Context
import android.app.Activity

val colors : Array<Int> = arrayOf(
    "#f44336",
    "#9C27B0",
    "#BF360C",
    "#00C853",
    "#1A237E"
).map {
    Color.parseColor(it)
}.toTypedArray()
val parts : Int = 4
val scGap : Float = 0.02f / parts
val strokeFactor : Float = 90f
val sizeFactor : Float = 3.9f
val rFactor : Float = 6.9f
val delay : Long = 20
val backColor : Int = Color.parseColor("#BDBDBD")

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawLineBarSlideBall(scale : Float, w : Float, h : Float, paint : Paint) {
    val size : Float = Math.min(w, h) / sizeFactor
    val r : Float = Math.min(w, h) / rFactor
    val sf : Float = scale.sinify()
    val sf1 : Float = sf.divideScale(0, parts)
    val sf2 : Float  = sf.divideScale(1, parts)
    val sf3 : Float = sf.divideScale(2, parts)
    val sf4 : Float = sf.divideScale(3, parts)
    save()
    translate(w / 2, h)
    for (j in 0..1) {
        save()
        scale(1f - 2 * j, 1f)
        drawLine(-r, 0f, -r, -size * sf1, paint)
        drawRect(RectF(-r - (w - r) * sf2, -size, -r, 0f), paint)
        restore()
    }
    save()
    translate(0f, -h + (h - size - r) * sf4)
    drawArc(RectF(-r, 0f, r, 2 * r), 0f, 360f * sf3, true, paint)
    restore()
    restore()
}

fun Canvas.drawLBSBNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    drawLineBarSlideBall(scale, w, h, paint)
}

class LineBarSlideBallView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class LBSBNode(var i : Int, val state : State = State()) {

        private var next : LBSBNode? = null
        private var prev : LBSBNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = LBSBNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawLBSBNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : LBSBNode {
            var curr : LBSBNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LineBarSlideBall(var i : Int) {

        private var curr : LBSBNode = LBSBNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : LineBarSlideBallView) {

        private val lbsb : LineBarSlideBall = LineBarSlideBall(0)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val animator : Animator = Animator(view)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            lbsb.draw(canvas, paint)
            animator.animate {
                lbsb.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            lbsb.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : LineBarSlideBallView {
            val view : LineBarSlideBallView = LineBarSlideBallView(activity)
            activity.setContentView(view)
            return view
        }
    }
}