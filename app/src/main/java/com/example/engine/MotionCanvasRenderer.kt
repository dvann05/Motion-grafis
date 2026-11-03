package com.example.engine

import android.graphics.*
import com.example.model.*
import kotlin.math.*

object MotionCanvasRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rectF = RectF()

    data class SceneTimingInfo(
        val sceneIndex: Int,
        val scene: MotionScene,
        val nextScene: MotionScene?,
        val localTimeSec: Float,
        val sceneDurationSec: Float,
        val progress: Float,
        val transitionFraction: Float
    )

    fun resolveSceneAtTime(project: MotionProject, globalTimeSec: Float): SceneTimingInfo {
        val scenes = project.scenes.ifEmpty { defaultSampleScenes() }
        val total = project.totalDurationSeconds.toFloat().coerceAtLeast(1f)
        val clampedTime = globalTimeSec.coerceIn(0f, total - 0.001f)

        var accumulated = 0f
        for (i in scenes.indices) {
            val s = scenes[i]
            val dur = s.durationSeconds.toFloat().coerceAtLeast(2f)
            if (clampedTime < accumulated + dur || i == scenes.lastIndex) {
                val local = (clampedTime - accumulated).coerceIn(0f, dur)
                val prog = (local / dur).coerceIn(0f, 1f)
                val transitionWindow = 0.65f.coerceAtMost(dur * 0.22f)
                val timeRemaining = dur - local
                val transFrac = if (timeRemaining < transitionWindow && i < scenes.lastIndex && s.transitionToNext != SceneTransition.CUT) {
                    (1f - (timeRemaining / transitionWindow)).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val next = if (i < scenes.lastIndex) scenes[i + 1] else null
                return SceneTimingInfo(
                    sceneIndex = i,
                    scene = s,
                    nextScene = next,
                    localTimeSec = local,
                    sceneDurationSec = dur,
                    progress = prog,
                    transitionFraction = transFrac
                )
            }
            accumulated += dur
        }
        val first = scenes.first()
        return SceneTimingInfo(0, first, scenes.getOrNull(1), 0f, first.durationSeconds.toFloat(), 0f, 0f)
    }

    fun renderFrame(
        canvas: Canvas,
        width: Float,
        height: Float,
        project: MotionProject,
        globalTimeSec: Float
    ) {
        val timing = resolveSceneAtTime(project, globalTimeSec)
        val style = project.animationStyle

        canvas.save()
        if (timing.transitionFraction > 0f && timing.nextScene != null) {
            renderTransitioningScenes(
                canvas = canvas,
                w = width,
                h = height,
                project = project,
                currentScene = timing.scene,
                nextScene = timing.nextScene,
                localTimeSec = timing.localTimeSec,
                progress = timing.progress,
                transFrac = timing.transitionFraction,
                style = style
            )
        } else {
            renderSingleScene(
                canvas = canvas,
                w = width,
                h = height,
                project = project,
                scene = timing.scene,
                sceneIndex = timing.sceneIndex,
                totalScenes = project.scenes.size,
                localTimeSec = timing.localTimeSec,
                progress = timing.progress,
                style = style
            )
        }

        // Studio Watermark / Frame HUD Badge at top-right
        drawTopBrandBadge(canvas, width, height, style, timing.sceneIndex + 1, project.scenes.size)
        canvas.restore()
    }

    private fun renderTransitioningScenes(
        canvas: Canvas,
        w: Float,
        h: Float,
        project: MotionProject,
        currentScene: MotionScene,
        nextScene: MotionScene,
        localTimeSec: Float,
        progress: Float,
        transFrac: Float,
        style: AnimationStyle
    ) {
        when (currentScene.transitionToNext) {
            SceneTransition.WIPE_LEFT -> {
                renderSingleScene(canvas, w, h, project, currentScene, 0, project.scenes.size, localTimeSec, progress, style)
                canvas.save()
                val wipeX = w * (1f - transFrac)
                canvas.clipRect(wipeX, 0f, w, h)
                renderSingleScene(canvas, w, h, project, nextScene, 1, project.scenes.size, 0.2f, 0.05f, style)
                paint.style = Paint.Style.FILL
                paint.color = parseHexColor(nextScene.primaryColorHex, Color.CYAN)
                canvas.drawRect(wipeX - w * 0.015f, 0f, wipeX + w * 0.015f, h, paint)
                canvas.restore()
            }
            SceneTransition.CIRCLE_IRIS -> {
                renderSingleScene(canvas, w, h, project, currentScene, 0, project.scenes.size, localTimeSec, progress, style)
                canvas.save()
                val maxRadius = hypot(w.toDouble(), h.toDouble()).toFloat() * 0.6f
                path.reset()
                path.addCircle(w * 0.5f, h * 0.5f, maxRadius * transFrac, Path.Direction.CW)
                canvas.clipPath(path)
                renderSingleScene(canvas, w, h, project, nextScene, 1, project.scenes.size, 0.2f, 0.05f, style)
                canvas.restore()
            }
            SceneTransition.GLITCH_FLASH -> {
                val chosen = if (transFrac < 0.5f) currentScene else nextScene
                val jitterX = sin(transFrac * 40f) * w * 0.04f
                canvas.save()
                canvas.translate(jitterX, 0f)
                renderSingleScene(canvas, w, h, project, chosen, 0, project.scenes.size, localTimeSec, progress, style)
                canvas.restore()
                // Glitch scanline bars
                paint.style = Paint.Style.FILL
                paint.color = Color.argb((180 * sin(transFrac * PI).toFloat()).toInt().coerceIn(0, 255), 0, 229, 255)
                for (i in 0..5) {
                    val barY = ((i * 0.17f + transFrac * 0.5f) % 1f) * h
                    canvas.drawRect(0f, barY, w, barY + h * 0.025f, paint)
                }
            }
            SceneTransition.ZOOM_THROUGH -> {
                canvas.save()
                val scale = 1f + transFrac * 1.4f
                canvas.scale(scale, scale, w * 0.5f, h * 0.5f)
                renderSingleScene(canvas, w, h, project, currentScene, 0, project.scenes.size, localTimeSec, progress, style)
                canvas.restore()
                if (transFrac > 0.45f) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.argb(((transFrac - 0.45f) * 380f).toInt().coerceIn(0, 220), 255, 255, 255)
                    canvas.drawRect(0f, 0f, w, h, paint)
                }
            }
            else -> {
                renderSingleScene(canvas, w, h, project, currentScene, 0, project.scenes.size, localTimeSec, progress, style)
                paint.style = Paint.Style.FILL
                paint.color = Color.argb((transFrac * 220).toInt().coerceIn(0, 255), 9, 12, 18)
                canvas.drawRect(0f, 0f, w, h, paint)
            }
        }
    }

    private fun renderSingleScene(
        canvas: Canvas,
        w: Float,
        h: Float,
        project: MotionProject,
        scene: MotionScene,
        sceneIndex: Int,
        totalScenes: Int,
        localTimeSec: Float,
        progress: Float,
        style: AnimationStyle
    ) {
        val primaryColor = parseHexColor(scene.primaryColorHex, Color.parseColor("#00E5FF"))
        val secondaryColor = parseHexColor(scene.secondaryColorHex, Color.parseColor("#8B5CF6"))
        val bgBaseColor = parseHexColor(scene.backgroundPreset.primaryHex, Color.parseColor("#0B1026"))

        // Quantize time for Pixel Art style (stepped 10fps feel)
        val effectiveTime = if (style == AnimationStyle.PIXEL_ART) {
            floor(localTimeSec * 10f) / 10f
        } else {
            localTimeSec
        }

        // 1. Background Environment
        drawSceneBackground(
            canvas = canvas,
            w = w,
            h = h,
            preset = scene.backgroundPreset,
            bgBaseColor = bgBaseColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            time = effectiveTime,
            style = style
        )

        // 2. Style-Specific Atmospheric Layer (Infographic HUD / Kinetic BG / Pixel Grid)
        drawStyleOverlayLayer(
            canvas = canvas,
            w = w,
            h = h,
            style = style,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            time = effectiveTime,
            progress = progress,
            sceneIndex = sceneIndex
        )

        // 3. Moving Objects Layer
        if (scene.objectType != MovingObjectType.NONE) {
            drawMovingObjects(
                canvas = canvas,
                w = w,
                h = h,
                objectType = scene.objectType,
                objectMotion = scene.objectMotion,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                time = effectiveTime,
                style = style
            )
        }

        // 4. Expressive Animated Character Layer
        if (scene.characterType != CharacterType.NONE) {
            drawAnimatedCharacter(
                canvas = canvas,
                w = w,
                h = h,
                characterType = scene.characterType,
                motion = scene.characterMotion,
                speed = scene.characterSpeed,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                time = effectiveTime,
                progress = progress,
                style = style
            )
        }

        // 5. Kinetic Headline & Visual Tag with Entrance/Exit Animations
        drawAnimatedHeadline(
            canvas = canvas,
            w = w,
            h = h,
            scene = scene,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            time = effectiveTime,
            progress = progress,
            style = style
        )

        // 6. Auto-Synced Word-by-Word Subtitles at Lower Third
        if (project.autoSubtitles && scene.narrationText.isNotBlank()) {
            drawSyncedSubtitles(
                canvas = canvas,
                w = w,
                h = h,
                narration = scene.narrationText,
                progress = progress,
                primaryColor = primaryColor,
                style = style
            )
        }

        // 7. Bottom Scene Progress Indicator Bar
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(60, 255, 255, 255)
        canvas.drawRect(0f, h - h * 0.008f, w, h, paint)
        paint.color = primaryColor
        canvas.drawRect(0f, h - h * 0.008f, w * progress, h, paint)
    }

    private fun drawSceneBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        preset: BackgroundPreset,
        bgBaseColor: Int,
        primaryColor: Int,
        secondaryColor: Int,
        time: Float,
        style: AnimationStyle
    ) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                bgBaseColor,
                blendColors(bgBaseColor, secondaryColor, 0.28f),
                Color.parseColor("#070A12")
            ),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Radial glow orb behind center stage
        val glowX = w * (0.5f + 0.12f * sin(time * 0.9f))
        val glowY = h * (0.46f + 0.06f * cos(time * 0.7f))
        paint.shader = RadialGradient(
            glowX,
            glowY,
            w * 0.65f,
            intArrayOf(
                withAlpha(primaryColor, if (style == AnimationStyle.MINIMALIST) 45 else 85),
                withAlpha(secondaryColor, 30),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(glowX, glowY, w * 0.65f, paint)
        paint.shader = null

        when (preset) {
            BackgroundPreset.CYBER_CITY -> {
                // Parallax skyline silhouettes at bottom
                val horizonY = h * 0.74f
                paint.style = Paint.Style.FILL
                for (layer in 0..1) {
                    val speed = if (layer == 0) 18f else 36f
                    val buildingCount = 9
                    val bw = w / buildingCount
                    paint.color = if (layer == 0) {
                        withAlpha(secondaryColor, 42)
                    } else {
                        Color.argb(170, 12, 18, 34)
                    }
                    for (i in -1..buildingCount) {
                        val seed = (i * 37 + layer * 19).absoluteValue
                        val bh = h * (0.12f + (seed % 18) * 0.012f)
                        val offsetX = ((time * speed) % bw)
                        val left = i * bw - offsetX
                        canvas.drawRect(left, horizonY - bh, left + bw * 0.88f, horizonY + h * 0.3f, paint)
                    }
                }
            }
            BackgroundPreset.NEON_GRID -> {
                val horizonY = h * 0.68f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (w * 0.003f).coerceAtLeast(1.5f)
                paint.color = withAlpha(primaryColor, 90)
                // Perspective lines
                for (i in -6..6) {
                    val topX = w * 0.5f + i * (w * 0.04f)
                    val botX = w * 0.5f + i * (w * 0.22f)
                    canvas.drawLine(topX, horizonY, botX, h, paint)
                }
                // Moving horizontal grid lines
                val shift = (time * 0.6f) % 1f
                for (j in 0..7) {
                    val f = ((j + shift) / 8f).pow(1.7f)
                    val y = horizonY + f * (h - horizonY)
                    canvas.drawLine(0f, y, w, y, paint)
                }
            }
            BackgroundPreset.SUNSET_HORIZON -> {
                val sunY = h * 0.52f
                val sunR = w * 0.28f
                paint.style = Paint.Style.FILL
                paint.shader = LinearGradient(
                    0f, sunY - sunR, 0f, sunY + sunR,
                    primaryColor, secondaryColor, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(w * 0.5f, sunY, sunR, paint)
                paint.shader = null
                // Retro synth sun stripes
                paint.color = bgBaseColor
                for (s in 0..4) {
                    val sy = sunY + sunR * (0.15f + s * 0.17f)
                    canvas.drawRect(w * 0.15f, sy, w * 0.85f, sy + sunR * (0.04f + s * 0.015f), paint)
                }
            }
            BackgroundPreset.DEEP_SPACE -> {
                paint.style = Paint.Style.FILL
                for (i in 0 until 28) {
                    val sx = ((i * 73 + (time * (8f + (i % 4) * 5f)).toInt()) % 1000) / 1000f * w
                    val sy = ((i * 139) % 1000) / 1000f * h
                    val twinkle = (0.4f + 0.6f * sin(time * 3f + i).absoluteValue)
                    paint.color = Color.argb((twinkle * 210).toInt(), 220, 240, 255)
                    val r = w * (0.003f + (i % 3) * 0.002f)
                    canvas.drawCircle(sx, sy, r, paint)
                }
            }
            BackgroundPreset.STUDIO_MINIMAL -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (w * 0.002f).coerceAtLeast(1f)
                paint.color = withAlpha(primaryColor, 45)
                val step = w / 8f
                for (x in 1..7) {
                    canvas.drawLine(x * step, 0f, x * step, h, paint)
                }
                for (y in 1..14) {
                    canvas.drawLine(0f, y * step, w, y * step, paint)
                }
            }
            BackgroundPreset.DATA_MATRIX -> {
                paint.style = Paint.Style.FILL
                val cols = 12
                val colW = w / cols
                for (c in 0 until cols) {
                    val dropY = ((time * (120f + (c % 5) * 40f) + c * 170f) % h)
                    paint.color = withAlpha(primaryColor, 95)
                    canvas.drawRoundRect(
                        c * colW + colW * 0.35f,
                        dropY,
                        c * colW + colW * 0.65f,
                        dropY + h * 0.06f,
                        6f, 6f, paint
                    )
                }
            }
        }
    }

    private fun drawStyleOverlayLayer(
        canvas: Canvas,
        w: Float,
        h: Float,
        style: AnimationStyle,
        primaryColor: Int,
        secondaryColor: Int,
        time: Float,
        progress: Float,
        sceneIndex: Int
    ) {
        when (style) {
            AnimationStyle.MOTION_INFOGRAPHIC -> {
                // Draw live animated infographic telemetry card & bar chart
                val cardLeft = w * 0.08f
                val cardTop = h * 0.30f
                val cardRight = w * 0.92f
                val cardBot = h * 0.44f
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(145, 14, 22, 38)
                rectF.set(cardLeft, cardTop, cardRight, cardBot)
                canvas.drawRoundRect(rectF, w * 0.03f, w * 0.03f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (w * 0.003f).coerceAtLeast(2f)
                paint.color = withAlpha(primaryColor, 160)
                canvas.drawRoundRect(rectF, w * 0.03f, w * 0.03f, paint)

                // Animated bars inside infographic panel
                paint.style = Paint.Style.FILL
                val barCount = 6
                val chartW = (cardRight - cardLeft) * 0.52f
                val barW = chartW / (barCount * 1.6f)
                for (b in 0 until barCount) {
                    val animGrow = (progress * 1.6f).coerceIn(0.15f, 1f)
                    val targetRatio = 0.35f + ((b + sceneIndex) % 4) * 0.16f + 0.1f * sin(time * 3f + b)
                    val bh = (cardBot - cardTop) * 0.68f * targetRatio * animGrow
                    val bx = cardLeft + w * 0.05f + b * (barW * 1.55f)
                    val by = cardBot - h * 0.02f
                    paint.color = if (b % 2 == 0) primaryColor else secondaryColor
                    canvas.drawRoundRect(bx, by - bh, bx + barW, by, 6f, 6f, paint)
                }

                // Live KPI Counter on right side of HUD
                textPaint.color = Color.WHITE
                textPaint.textSize = w * 0.068f
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textPaint.textAlign = Paint.Align.RIGHT
                val kpiValue = ((progress * 100f) + sceneIndex * 25).toInt()
                canvas.drawText("+$kpiValue%", cardRight - w * 0.05f, cardTop + (cardBot - cardTop) * 0.54f, textPaint)
                textPaint.textSize = w * 0.026f
                textPaint.color = withAlpha(primaryColor, 220)
                canvas.drawText("LIVE METRIC INDEX", cardRight - w * 0.05f, cardTop + (cardBot - cardTop) * 0.78f, textPaint)
            }
            AnimationStyle.KINETIC_TYPOGRAPHY -> {
                // Bold diagonal kinetic rhythm ribbons
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = w * 0.008f
                paint.color = withAlpha(primaryColor, 110)
                val offset = (time * 90f) % (w * 0.25f)
                canvas.drawLine(0f, h * 0.27f, w, h * 0.23f, paint)
                canvas.drawLine(offset, h * 0.74f, w, h * 0.70f, paint)
            }
            AnimationStyle.PIXEL_ART -> {
                // Subtle scanline + pixel corner frame
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(28, 0, 0, 0)
                val step = (h / 80f).coerceAtLeast(4f)
                var y = 0f
                while (y < h) {
                    canvas.drawRect(0f, y, w, y + step * 0.45f, paint)
                    y += step
                }
            }
            AnimationStyle.MINIMALIST -> {
                // Fine rotating geometric Bauhaus rings
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (w * 0.003f).coerceAtLeast(1.5f)
                paint.color = withAlpha(primaryColor, 130)
                canvas.drawCircle(w * 0.5f, h * 0.54f, w * (0.28f + 0.02f * sin(time * 2f)), paint)
                paint.color = withAlpha(secondaryColor, 90)
                canvas.drawCircle(w * 0.5f, h * 0.54f, w * 0.36f, paint)
            }
            AnimationStyle.CARTOON_2D -> {
                // Playful comic ground curve at bottom
                paint.style = Paint.Style.FILL
                paint.color = blendColors(primaryColor, Color.parseColor("#0F172A"), 0.75f)
                rectF.set(-w * 0.2f, h * 0.69f, w * 1.2f, h * 1.08f)
                canvas.drawOval(rectF, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = w * 0.008f
                paint.color = Color.BLACK
                canvas.drawOval(rectF, paint)
            }
            AnimationStyle.FLAT_VECTOR -> {
                // Smooth layered vector wave hills
                path.reset()
                val baseWaveY = h * 0.72f
                path.moveTo(0f, h)
                path.lineTo(0f, baseWaveY)
                var x = 0f
                val step = w / 16f
                while (x <= w + step) {
                    val wy = baseWaveY + sin(x * 0.012f + time * 2.2f) * (h * 0.022f)
                    path.lineTo(x, wy)
                    x += step
                }
                path.lineTo(w, h)
                path.close()
                paint.style = Paint.Style.FILL
                paint.color = withAlpha(secondaryColor, 65)
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawMovingObjects(
        canvas: Canvas,
        w: Float,
        h: Float,
        objectType: MovingObjectType,
        objectMotion: ObjectMotion,
        primaryColor: Int,
        secondaryColor: Int,
        time: Float,
        style: AnimationStyle
    ) {
        val count = 5
        for (i in 0 until count) {
            val phase = i * (2f * PI.toFloat() / count)
            val (ox, oy) = when (objectMotion) {
                ObjectMotion.ORBIT -> {
                    val angle = time * 1.4f + phase
                    val rx = w * 0.34f
                    val ry = h * 0.14f
                    (w * 0.5f + cos(angle) * rx) to (h * 0.54f + sin(angle) * ry)
                }
                ObjectMotion.SLIDE_ACROSS -> {
                    val fx = ((time * 0.28f + i * 0.21f) % 1.2f) - 0.1f
                    val fy = 0.34f + (i % 3) * 0.12f + 0.02f * sin(time * 3f + i)
                    (w * fx) to (h * fy)
                }
                ObjectMotion.FLOAT_UP -> {
                    val fy = 0.78f - ((time * 0.22f + i * 0.19f) % 0.55f)
                    val fx = 0.16f + i * 0.17f + 0.03f * sin(time * 2.5f + i)
                    (w * fx) to (h * fy)
                }
                ObjectMotion.SPIN_PULSE -> {
                    val pulse = 1f + 0.22f * sin(time * 4f + i)
                    val angle = phase + time * 0.7f
                    (w * 0.5f + cos(angle) * w * 0.28f * pulse) to (h * 0.53f + sin(angle) * h * 0.12f * pulse)
                }
                ObjectMotion.PARTICLE_RAIN -> {
                    val fy = ((time * 0.32f + i * 0.23f) % 0.75f) + 0.15f
                    val fx = 0.15f + i * 0.175f
                    (w * fx) to (h * fy)
                }
            }

            val size = w * (0.042f + 0.008f * sin(time * 3f + i))
            val color = if (i % 2 == 0) primaryColor else secondaryColor
            paint.style = Paint.Style.FILL
            paint.color = color

            when (objectType) {
                MovingObjectType.ROCKET_SHIP -> {
                    canvas.save()
                    canvas.translate(ox, oy)
                    canvas.rotate(-35f + 8f * sin(time * 4f + i))
                    rectF.set(-size * 0.5f, -size, size * 0.5f, size)
                    canvas.drawOval(rectF, paint)
                    paint.color = Color.parseColor("#F59E0B")
                    canvas.drawCircle(0f, size * 1.15f, size * 0.35f, paint)
                    canvas.restore()
                }
                MovingObjectType.GROWTH_CHART -> {
                    rectF.set(ox - size, oy - size * 0.7f, ox + size, oy + size * 0.7f)
                    paint.color = withAlpha(color, 190)
                    canvas.drawRoundRect(rectF, 10f, 10f, paint)
                    paint.color = Color.WHITE
                    paint.strokeWidth = (w * 0.004f).coerceAtLeast(2f)
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(ox - size * 0.6f, oy + size * 0.3f, ox, oy - size * 0.1f, paint)
                    canvas.drawLine(ox, oy - size * 0.1f, ox + size * 0.6f, oy - size * 0.4f, paint)
                }
                MovingObjectType.IDEA_BULBS -> {
                    paint.color = withAlpha(color, 75)
                    canvas.drawCircle(ox, oy, size * 1.4f, paint)
                    paint.color = Color.parseColor("#FBBF24")
                    canvas.drawCircle(ox, oy, size * 0.75f, paint)
                }
                MovingObjectType.DATA_ORBS -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = (w * 0.004f).coerceAtLeast(2f)
                    paint.color = color
                    canvas.drawCircle(ox, oy, size, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(ox, oy, size * 0.42f, paint)
                }
                MovingObjectType.FLOATING_COINS -> {
                    val scaleX = cos(time * 3.5f + i).absoluteValue.coerceAtLeast(0.25f)
                    rectF.set(ox - size * scaleX, oy - size, ox + size * scaleX, oy + size)
                    paint.color = Color.parseColor("#F59E0B")
                    canvas.drawOval(rectF, paint)
                }
                else -> {
                    // Spark stars / diamond particles
                    path.reset()
                    path.moveTo(ox, oy - size)
                    path.lineTo(ox + size * 0.35f, oy - size * 0.35f)
                    path.lineTo(ox + size, oy)
                    path.lineTo(ox + size * 0.35f, oy + size * 0.35f)
                    path.lineTo(ox, oy + size)
                    path.lineTo(ox - size * 0.35f, oy + size * 0.35f)
                    path.lineTo(ox - size, oy)
                    path.lineTo(ox - size * 0.35f, oy - size * 0.35f)
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun drawAnimatedCharacter(
        canvas: Canvas,
        w: Float,
        h: Float,
        characterType: CharacterType,
        motion: CharacterMotion,
        speed: Float,
        primaryColor: Int,
        secondaryColor: Int,
        time: Float,
        progress: Float,
        style: AnimationStyle
    ) {
        val t = time * speed.coerceIn(0.5f, 2.5f)
        var cx = w * 0.5f
        var cy = h * 0.56f
        var scale = 1.0f
        var tiltDeg = 0f

        when (motion) {
            CharacterMotion.WALK_IN -> {
                val enter = (progress * 3.2f).coerceIn(0f, 1f)
                cx = w * (0.18f + 0.32f * easeOutBack(enter)) + sin(t * 2.2f) * (w * 0.04f)
                cy += abs(sin(t * 6f)) * (-h * 0.018f)
                tiltDeg = sin(t * 6f) * 4f
            }
            CharacterMotion.BOUNCE -> {
                cy += -abs(sin(t * 4.5f)) * (h * 0.055f)
                scale = 1f + sin(t * 9f) * 0.04f
            }
            CharacterMotion.FLOAT_WAVE -> {
                cx += sin(t * 1.6f) * (w * 0.05f)
                cy += cos(t * 2.4f) * (h * 0.025f)
                tiltDeg = sin(t * 1.8f) * 5f
            }
            CharacterMotion.DANCE_PULSE -> {
                cx += sin(t * 4f) * (w * 0.045f)
                cy += abs(cos(t * 8f)) * (-h * 0.025f)
                tiltDeg = sin(t * 4f) * 9f
                scale = 1f + 0.06f * sin(t * 8f)
            }
            CharacterMotion.ZOOM_HERO -> {
                val enter = (progress * 2.8f).coerceIn(0f, 1f)
                scale = 0.55f + 0.50f * easeOutBack(enter)
                cy += sin(t * 2.2f) * (h * 0.015f)
            }
            CharacterMotion.STATIC_IDLE -> {
                scale = 1f + 0.025f * sin(t * 2f)
            }
        }

        val charRadius = w * 0.115f
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(tiltDeg)
        canvas.scale(scale, scale)

        // Shadow below character
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(95, 0, 0, 0)
        rectF.set(-charRadius * 0.9f, charRadius * 1.45f, charRadius * 0.9f, charRadius * 1.72f)
        canvas.drawOval(rectF, paint)

        val armAngle = sin(t * 5.2f) * 28f
        val legOffset = sin(t * 5.2f) * (charRadius * 0.22f)

        // Legs
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = charRadius * 0.22f
        paint.color = secondaryColor
        canvas.drawLine(-charRadius * 0.32f, charRadius * 0.75f, -charRadius * 0.36f, charRadius * 1.42f + legOffset, paint)
        canvas.drawLine(charRadius * 0.32f, charRadius * 0.75f, charRadius * 0.36f, charRadius * 1.42f - legOffset, paint)

        // Arms (animated wave)
        canvas.save()
        canvas.translate(-charRadius * 0.75f, charRadius * 0.1f)
        canvas.rotate(-25f + armAngle)
        canvas.drawLine(0f, 0f, -charRadius * 0.55f, charRadius * 0.35f, paint)
        canvas.restore()

        canvas.save()
        canvas.translate(charRadius * 0.75f, charRadius * 0.1f)
        canvas.rotate(25f - armAngle * 1.3f)
        canvas.drawLine(0f, 0f, charRadius * 0.58f, -charRadius * 0.35f, paint)
        canvas.restore()

        // Torso
        paint.style = Paint.Style.FILL
        paint.color = blendColors(primaryColor, Color.parseColor("#1E293B"), 0.35f)
        rectF.set(-charRadius * 0.72f, -charRadius * 0.1f, charRadius * 0.72f, charRadius * 0.92f)
        val corner = if (style == AnimationStyle.PIXEL_ART) 4f else charRadius * 0.32f
        canvas.drawRoundRect(rectF, corner, corner, paint)

        // Chest emblem
        paint.color = primaryColor
        canvas.drawCircle(0f, charRadius * 0.38f, charRadius * 0.20f, paint)

        // Head based on CharacterType
        paint.color = when (characterType) {
            CharacterType.ROBOT_AI -> Color.parseColor("#E2E8F0")
            CharacterType.ASTRONAUT -> Color.WHITE
            CharacterType.PIXEL_CAT -> primaryColor
            else -> Color.parseColor("#F8FAFC")
        }
        rectF.set(-charRadius * 0.82f, -charRadius * 1.38f, charRadius * 0.82f, -charRadius * 0.05f)
        canvas.drawRoundRect(rectF, corner * 1.2f, corner * 1.2f, paint)

        // Cartoon bold outline if 2D Cartoon
        if (style == AnimationStyle.CARTOON_2D) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = charRadius * 0.08f
            paint.color = Color.BLACK
            canvas.drawRoundRect(rectF, corner * 1.2f, corner * 1.2f, paint)
            paint.style = Paint.Style.FILL
        }

        // Character specific headwear / antenna / ears
        when (characterType) {
            CharacterType.ROBOT_AI -> {
                paint.color = primaryColor
                canvas.drawLine(0f, -charRadius * 1.38f, 0f, -charRadius * 1.75f, paint)
                canvas.drawCircle(0f, -charRadius * 1.78f, charRadius * 0.14f, paint)
            }
            CharacterType.PIXEL_CAT -> {
                path.reset()
                path.moveTo(-charRadius * 0.7f, -charRadius * 1.3f)
                path.lineTo(-charRadius * 0.45f, -charRadius * 1.75f)
                path.lineTo(-charRadius * 0.15f, -charRadius * 1.35f)
                path.close()
                canvas.drawPath(path, paint)

                path.reset()
                path.moveTo(charRadius * 0.7f, -charRadius * 1.3f)
                path.lineTo(charRadius * 0.45f, -charRadius * 1.75f)
                path.lineTo(charRadius * 0.15f, -charRadius * 1.35f)
                path.close()
                canvas.drawPath(path, paint)
            }
            CharacterType.ASTRONAUT -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = charRadius * 0.1f
                paint.color = secondaryColor
                canvas.drawCircle(0f, -charRadius * 0.72f, charRadius * 0.88f, paint)
                paint.style = Paint.Style.FILL
            }
            else -> {
                // Stylish visor/hair bar
                paint.color = secondaryColor
                rectF.set(-charRadius * 0.84f, -charRadius * 1.42f, charRadius * 0.84f, -charRadius * 1.08f)
                canvas.drawRoundRect(rectF, corner, corner, paint)
            }
        }

        // Digital Visor & Blinking Expressive Eyes
        paint.color = Color.parseColor("#0F172A")
        rectF.set(-charRadius * 0.62f, -charRadius * 1.02f, charRadius * 0.62f, -charRadius * 0.36f)
        canvas.drawRoundRect(rectF, corner * 0.6f, corner * 0.6f, paint)

        val isBlinking = (sin(t * 2.7f) > 0.94f)
        paint.color = primaryColor
        if (isBlinking) {
            canvas.drawRect(-charRadius * 0.42f, -charRadius * 0.72f, -charRadius * 0.14f, -charRadius * 0.66f, paint)
            canvas.drawRect(charRadius * 0.14f, -charRadius * 0.72f, charRadius * 0.42f, -charRadius * 0.66f, paint)
        } else {
            canvas.drawCircle(-charRadius * 0.28f, -charRadius * 0.70f, charRadius * 0.13f, paint)
            canvas.drawCircle(charRadius * 0.28f, -charRadius * 0.70f, charRadius * 0.13f, paint)
        }

        canvas.restore()
    }

    private fun drawAnimatedHeadline(
        canvas: Canvas,
        w: Float,
        h: Float,
        scene: MotionScene,
        primaryColor: Int,
        secondaryColor: Int,
        time: Float,
        progress: Float,
        style: AnimationStyle
    ) {
        val enterFrac = (progress / 0.20f).coerceIn(0f, 1f)
        val exitFrac = ((progress - 0.84f) / 0.16f).coerceIn(0f, 1f)

        var alpha = 255
        var offsetY = 0f
        var offsetX = 0f
        var scale = 1f
        var visibleCharsRatio = 1f

        // Entrance animation
        when (scene.textAnimIn) {
            TextAnimIn.TYPEWRITER -> {
                visibleCharsRatio = enterFrac
            }
            TextAnimIn.SLIDE_UP -> {
                offsetY = (1f - easeOutCubic(enterFrac)) * (h * 0.08f)
                alpha = (enterFrac * 255).toInt()
            }
            TextAnimIn.POP_ELASTIC -> {
                scale = easeOutBack(enterFrac).coerceAtLeast(0.05f)
                alpha = (enterFrac * 255).toInt()
            }
            TextAnimIn.GLITCH_IN -> {
                if (enterFrac < 1f) {
                    offsetX = sin(time * 55f) * (w * 0.035f) * (1f - enterFrac)
                }
                alpha = (enterFrac * 255).toInt()
            }
            TextAnimIn.FADE_SCALE -> {
                scale = 0.75f + 0.25f * easeOutCubic(enterFrac)
                alpha = (enterFrac * 255).toInt()
            }
        }

        // Exit animation
        if (exitFrac > 0f) {
            when (scene.textAnimOut) {
                TextAnimOut.FADE_OUT -> {
                    alpha = ((1f - exitFrac) * alpha).toInt()
                }
                TextAnimOut.SLIDE_DOWN -> {
                    offsetY += exitFrac * (h * 0.07f)
                    alpha = ((1f - exitFrac) * alpha).toInt()
                }
                TextAnimOut.ZOOM_OUT -> {
                    scale *= (1f + exitFrac * 0.45f)
                    alpha = ((1f - exitFrac) * alpha).toInt()
                }
            }
        }

        alpha = alpha.coerceIn(0, 255)
        val fullText = scene.headlineText.ifBlank { scene.title }.uppercase()
        val charCount = (fullText.length * visibleCharsRatio).toInt().coerceIn(1, fullText.length)
        val displayHeadline = fullText.substring(0, charCount)

        val centerY = if (style == AnimationStyle.KINETIC_TYPOGRAPHY) h * 0.22f else h * 0.17f

        canvas.save()
        canvas.translate(w * 0.5f + offsetX, centerY + offsetY)
        canvas.scale(scale, scale)

        // Category / Scene Pill above Headline
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.textSize = w * 0.027f
        textPaint.textAlign = Paint.Align.CENTER
        val pillLabel = scene.title.uppercase()
        val pillW = (textPaint.measureText(pillLabel) + w * 0.08f).coerceAtMost(w * 0.84f)
        val pillH = h * 0.032f
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(primaryColor, (alpha * 0.22f).toInt())
        rectF.set(-pillW * 0.5f, -pillH * 2.1f, pillW * 0.5f, -pillH * 1.1f)
        canvas.drawRoundRect(rectF, pillH * 0.5f, pillH * 0.5f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (w * 0.0025f).coerceAtLeast(1.5f)
        paint.color = withAlpha(primaryColor, alpha)
        canvas.drawRoundRect(rectF, pillH * 0.5f, pillH * 0.5f, paint)

        textPaint.color = withAlpha(primaryColor, alpha)
        canvas.drawText(pillLabel, 0f, -pillH * 1.42f, textPaint)

        // Wrap headline into max 2 balanced lines
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = if (style == AnimationStyle.KINETIC_TYPOGRAPHY) w * 0.072f else w * 0.058f
        val lines = wrapTextIntoLines(displayHeadline, textPaint, w * 0.84f, maxLines = 2)

        val lineHeight = textPaint.textSize * 1.22f
        for (i in lines.indices) {
            val ly = i * lineHeight
            // Subtle shadow
            textPaint.color = Color.argb((alpha * 0.75f).toInt(), 0, 0, 0)
            canvas.drawText(lines[i], 0f, ly + 4f, textPaint)
            textPaint.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawText(lines[i], 0f, ly, textPaint)
        }

        // Accent underline bar
        val barHalfW = w * 0.14f * enterFrac
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(secondaryColor, alpha)
        val barTop = lines.size * lineHeight - lineHeight * 0.45f
        canvas.drawRoundRect(-barHalfW, barTop, barHalfW, barTop + h * 0.006f, 6f, 6f, paint)

        canvas.restore()
    }

    private fun drawSyncedSubtitles(
        canvas: Canvas,
        w: Float,
        h: Float,
        narration: String,
        progress: Float,
        primaryColor: Int,
        style: AnimationStyle
    ) {
        val words = narration.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return

        val activeWordIndex = (progress * words.size).toInt().coerceIn(0, words.lastIndex)
        // Group words into chunks of 6 words so subtitles stay super readable on 9:16 mobile video
        val chunkSize = 6
        val chunkIndex = activeWordIndex / chunkSize
        val startWord = chunkIndex * chunkSize
        val endWord = (startWord + chunkSize).coerceAtMost(words.size)
        val chunkWords = words.subList(startWord, endWord)
        val activeInChunk = activeWordIndex - startWord

        val boxTop = h * 0.80f
        val boxBot = h * 0.91f
        val boxLeft = w * 0.07f
        val boxRight = w * 0.93f

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(205, 8, 12, 22)
        rectF.set(boxLeft, boxTop, boxRight, boxBot)
        val corner = if (style == AnimationStyle.PIXEL_ART) 4f else w * 0.035f
        canvas.drawRoundRect(rectF, corner, corner, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (w * 0.003f).coerceAtLeast(1.5f)
        paint.color = withAlpha(primaryColor, 130)
        canvas.drawRoundRect(rectF, corner, corner, paint)

        // Render 2 lines of max 3 words each with active word highlight pill
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = w * 0.039f
        textPaint.textAlign = Paint.Align.LEFT

        val row1 = chunkWords.take(3)
        val row2 = chunkWords.drop(3)
        drawSubtitleRow(canvas, row1, activeInChunk, w * 0.5f, boxTop + (boxBot - boxTop) * 0.42f, primaryColor, w)
        if (row2.isNotEmpty()) {
            drawSubtitleRow(canvas, row2, activeInChunk - 3, w * 0.5f, boxTop + (boxBot - boxTop) * 0.78f, primaryColor, w)
        }
    }

    private fun drawSubtitleRow(
        canvas: Canvas,
        rowWords: List<String>,
        activeIdxInRow: Int,
        centerX: Float,
        baselineY: Float,
        primaryColor: Int,
        canvasW: Float
    ) {
        if (rowWords.isEmpty()) return
        val spaceW = textPaint.measureText(" ") * 1.15f
        val widths = rowWords.map { textPaint.measureText(it) }
        val totalW = widths.sum() + spaceW * (rowWords.size - 1).coerceAtLeast(0)
        var curX = centerX - totalW * 0.5f

        for (i in rowWords.indices) {
            val word = rowWords[i]
            val ww = widths[i]
            val isCurrent = (i == activeIdxInRow)
            if (isCurrent) {
                paint.style = Paint.Style.FILL
                paint.color = primaryColor
                rectF.set(
                    curX - canvasW * 0.014f,
                    baselineY - textPaint.textSize * 0.92f,
                    curX + ww + canvasW * 0.014f,
                    baselineY + textPaint.textSize * 0.24f
                )
                canvas.drawRoundRect(rectF, 8f, 8f, paint)
                textPaint.color = Color.parseColor("#06090F")
            } else {
                textPaint.color = Color.WHITE
            }
            canvas.drawText(word, curX, baselineY, textPaint)
            curX += ww + spaceW
        }
    }

    private fun drawTopBrandBadge(
        canvas: Canvas,
        w: Float,
        h: Float,
        style: AnimationStyle,
        sceneNum: Int,
        totalScenes: Int
    ) {
        val badgeText = "MOTION LAB AI • ${style.badgeTag} • $sceneNum/$totalScenes"
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.textSize = w * 0.023f
        textPaint.textAlign = Paint.Align.CENTER

        val bw = textPaint.measureText(badgeText) + w * 0.06f
        val bh = h * 0.028f
        val top = h * 0.032f
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(150, 9, 12, 18)
        rectF.set(w * 0.5f - bw * 0.5f, top, w * 0.5f + bw * 0.5f, top + bh)
        canvas.drawRoundRect(rectF, bh * 0.5f, bh * 0.5f, paint)

        textPaint.color = Color.argb(210, 154, 168, 199)
        canvas.drawText(badgeText, w * 0.5f, top + bh * 0.68f, textPaint)
    }

    private fun wrapTextIntoLines(
        text: String,
        paint: Paint,
        maxWidth: Float,
        maxLines: Int
    ): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = candidate
            } else {
                lines.add(current)
                current = word
                if (lines.size >= maxLines - 1) break
            }
        }
        val remainingStart = lines.joinToString(" ").split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        if (remainingStart < words.size) {
            lines.add(words.subList(remainingStart, words.size).joinToString(" "))
        }
        return lines.take(maxLines)
    }

    private fun easeOutCubic(x: Float): Float {
        val f = (1f - x.coerceIn(0f, 1f))
        return 1f - f * f * f
    }

    private fun easeOutBack(x: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val p = x.coerceIn(0f, 1f) - 1f
        return 1f + c3 * p * p * p + c1 * p * p
    }

    fun parseHexColor(hex: String, fallback: Int): Int {
        return try {
            Color.parseColor(hex.trim())
        } catch (_: Exception) {
            fallback
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        val ir = 1f - r
        return Color.rgb(
            (Color.red(c1) * ir + Color.red(c2) * r).toInt(),
            (Color.green(c1) * ir + Color.green(c2) * r).toInt(),
            (Color.blue(c1) * ir + Color.blue(c2) * r).toInt()
        )
    }
}
