package com.cocido.morfipolo.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.cocido.morfipolo.R
import java.util.Random

class ConfettiView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val random = Random()
    private var animator: ValueAnimator? = null

    private val colors = listOf(
        ContextCompat.getColor(context, R.color.primary),
        ContextCompat.getColor(context, R.color.accent), // Tertiary/Accent
        ContextCompat.getColor(context, R.color.warning)
    )

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun fireConfetti() {
        particles.clear()
        val centerX = width / 2f
        val centerY = height / 2f

        for (i in 0 until 24) {
            particles.add(
                Particle(
                    x = centerX,
                    y = centerY,
                    color = colors[random.nextInt(colors.size)],
                    type = random.nextInt(3), // 0: circle, 1: rect, 2: star
                    angle = Math.toRadians((random.nextInt(120) + 210).toDouble()).toFloat(), // upward
                    speed = random.nextFloat() * 40f + 20f,
                    rotationSpeed = random.nextFloat() * 20f - 10f,
                    size = random.nextFloat() * 20f + 15f
                )
            )
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                val progress = it.animatedValue as Float
                updateParticles(progress)
                invalidate()
            }
            start()
        }
    }

    private fun updateParticles(progress: Float) {
        for (p in particles) {
            p.x += Math.cos(p.angle.toDouble()).toFloat() * p.speed * (1f - progress)
            p.y += Math.sin(p.angle.toDouble()).toFloat() * p.speed * (1f - progress) + (progress * 10f) // gravity
            p.rotation += p.rotationSpeed
            p.alpha = ((1f - progress) * 255).toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in particles) {
            p.paint.alpha = p.alpha
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            
            when (p.type) {
                0 -> canvas.drawCircle(0f, 0f, p.size / 2, p.paint)
                1 -> canvas.drawRect(-p.size/2, -p.size/2, p.size/2, p.size/2, p.paint)
                2 -> drawStar(canvas, p.paint, p.size)
            }
            canvas.restore()
        }
    }

    private fun drawStar(canvas: Canvas, paint: Paint, size: Float) {
        val path = Path()
        val halfSize = size / 2
        path.moveTo(0f, -halfSize)
        for (i in 1 until 5) {
            val angle = i * Math.PI * 0.8
            path.lineTo(
                (Math.sin(angle) * halfSize).toFloat(),
                (-Math.cos(angle) * halfSize).toFloat()
            )
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private inner class Particle(
        var x: Float,
        var y: Float,
        val color: Int,
        val type: Int,
        val angle: Float,
        val speed: Float,
        val rotationSpeed: Float,
        val size: Float
    ) {
        var rotation: Float = 0f
        var alpha: Int = 255
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = this@Particle.color
            style = Paint.Style.FILL
        }
    }
}
