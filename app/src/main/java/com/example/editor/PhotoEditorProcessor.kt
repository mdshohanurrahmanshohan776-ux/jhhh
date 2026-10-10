package com.example.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.model.CameraFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EditAdjustments(
    val exposure: Float = 0f,     // -100 to 100
    val contrast: Float = 0f,     // -100 to 100
    val saturation: Float = 0f,   // -100 to 100
    val warmth: Float = 0f,       // -100 to 100
    val tint: Float = 0f,         // -100 to 100
    val vignette: Float = 0f,     // 0 to 100
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val selectedFilter: CameraFilter = CameraFilter.NORMAL
)

object PhotoEditorProcessor {

    suspend fun processImage(
        original: Bitmap,
        adjustments: EditAdjustments
    ): Bitmap = withContext(Dispatchers.Default) {
        // 1. Handle orientation / rotation / flip
        val matrix = Matrix()
        if (adjustments.rotationDegrees != 0) {
            matrix.postRotate(adjustments.rotationDegrees.toFloat())
        }
        if (adjustments.flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }

        val orientedBitmap = if (!matrix.isIdentity) {
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        } else {
            original
        }

        // 2. Build Composite ColorMatrix
        val finalColorMatrix = ColorMatrix()

        // Apply Preset Filter first
        applyFilterMatrix(finalColorMatrix, adjustments.selectedFilter)

        // Apply Exposure (Brightness shift)
        if (adjustments.exposure != 0f) {
            val brightnessShift = adjustments.exposure * 1.5f
            val brightnessMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, brightnessShift,
                    0f, 1f, 0f, 0f, brightnessShift,
                    0f, 0f, 1f, 0f, brightnessShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            finalColorMatrix.postConcat(brightnessMatrix)
        }

        // Apply Contrast
        if (adjustments.contrast != 0f) {
            // scale from 0.0 to 2.0 (0 -> 1.0)
            val scale = (adjustments.contrast + 100f) / 100f
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            finalColorMatrix.postConcat(contrastMatrix)
        }

        // Apply Saturation
        if (adjustments.saturation != 0f) {
            val satVal = (adjustments.saturation + 100f) / 100f
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(satVal.coerceIn(0f, 3f))
            finalColorMatrix.postConcat(satMatrix)
        }

        // Apply Warmth (Temperature: shift red up, blue down)
        if (adjustments.warmth != 0f) {
            val w = adjustments.warmth * 0.8f
            val warmthMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, w,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, -w,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            finalColorMatrix.postConcat(warmthMatrix)
        }

        // Apply Tint (shift green up / magenta down)
        if (adjustments.tint != 0f) {
            val t = adjustments.tint * 0.8f
            val tintMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, t * 0.5f,
                    0f, 1f, 0f, 0f, -t,
                    0f, 0f, 1f, 0f, t * 0.5f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            finalColorMatrix.postConcat(tintMatrix)
        }

        // 3. Render into output bitmap
        val outputBitmap = Bitmap.createBitmap(
            orientedBitmap.width,
            orientedBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(outputBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(finalColorMatrix)
        }

        canvas.drawBitmap(orientedBitmap, 0f, 0f, paint)

        // 4. Apply Vignette if requested
        if (adjustments.vignette > 0f) {
            val vignetteAlpha = (adjustments.vignette / 100f * 180f).toInt().coerceIn(0, 220)
            val radius = (kotlin.math.hypot(outputBitmap.width.toDouble(), outputBitmap.height.toDouble()) / 2f).toFloat()
            val vignetteShader = RadialGradient(
                outputBitmap.width / 2f,
                outputBitmap.height / 2f,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(vignetteAlpha, 0, 0, 0)),
                floatArrayOf(0.45f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = vignetteShader
            }
            canvas.drawRect(0f, 0f, outputBitmap.width.toFloat(), outputBitmap.height.toFloat(), vignettePaint)
        }

        outputBitmap
    }

    fun applyUltraWideOpticalField(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val meshW = 16
        val meshH = 16
        val count = (meshW + 1) * (meshH + 1)
        val verts = FloatArray(count * 2)

        val cx = w / 2f
        val cy = h / 2f

        var index = 0
        for (y in 0..meshH) {
            val fy = y.toFloat() / meshH * h
            for (x in 0..meshW) {
                val fx = x.toFloat() / meshW * w

                val dx = (fx - cx) / cx
                val dy = (fy - cy) / cy
                val r = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Wide-angle optical lens distortion factor
                val factor = 1f + 0.12f * (r * r)

                val newX = cx + dx * cx * factor
                val newY = cy + dy * cy * factor

                verts[index * 2] = newX.coerceIn(0f, w.toFloat())
                verts[index * 2 + 1] = newY.coerceIn(0f, h.toFloat())
                index++
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmapMesh(source, meshW, meshH, verts, 0, null, 0, paint)
        return output
    }

    private fun applyFilterMatrix(matrix: ColorMatrix, filter: CameraFilter) {
        when (filter) {
            CameraFilter.NORMAL -> {
                // Identity
            }
            CameraFilter.VIVID_WIDE -> {
                // Boost saturation and contrast for landscape ultra-wide shots
                val sat = ColorMatrix()
                sat.setSaturation(1.4f)
                val boost = ColorMatrix(
                    floatArrayOf(
                        1.05f, 0f, 0f, 0f, 5f,
                        0f, 1.10f, 0f, 0f, 5f,
                        0f, 0f, 1.15f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(sat)
                matrix.postConcat(boost)
            }
            CameraFilter.GOLDEN_HOUR -> {
                // Warm amber glow
                val golden = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 30f,
                        0f, 1.05f, 0f, 0f, 15f,
                        0f, 0f, 0.85f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(golden)
            }
            CameraFilter.CINEMATIC -> {
                // Teal & Orange Hollywood look
                val cinematic = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, 15f,
                        0f, 1.02f, 0f, 0f, 0f,
                        0f, 0f, 1.25f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val sat = ColorMatrix()
                sat.setSaturation(1.15f)
                matrix.postConcat(cinematic)
                matrix.postConcat(sat)
            }
            CameraFilter.NOIR -> {
                // High contrast Black & White
                val bw = ColorMatrix()
                bw.setSaturation(0f)
                val highContrast = ColorMatrix(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, -35f,
                        0f, 1.35f, 0f, 0f, -35f,
                        0f, 0f, 1.35f, 0f, -35f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(bw)
                matrix.postConcat(highContrast)
            }
            CameraFilter.CYBERPUNK -> {
                // Neon magenta and cyan highlights
                val cyber = ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0.2f, 0f, 20f,
                        0f, 0.9f, 0f, 0f, -10f,
                        0.2f, 0.1f, 1.4f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(cyber)
            }
            CameraFilter.VINTAGE -> {
                // Faded retro sepia tone
                val sepia = ColorMatrix(
                    floatArrayOf(
                        0.9f, 0.2f, 0.1f, 0f, 25f,
                        0.15f, 0.85f, 0.1f, 0f, 15f,
                        0.1f, 0.15f, 0.7f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(sepia)
            }
            CameraFilter.MOODY_FOREST -> {
                // Deep shadows, emerald greens
                val forest = ColorMatrix(
                    floatArrayOf(
                        0.85f, 0f, 0f, 0f, -15f,
                        0f, 1.25f, 0f, 0f, 10f,
                        0f, 0f, 0.9f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(forest)
            }
        }
    }
}
