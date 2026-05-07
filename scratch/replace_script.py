import os
import re

file_path = r"c:\Users\talha\Desktop\ClearRead\app\src\main\java\com\clearread\ui\reader\PdfReaderScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. State definitions
content = re.sub(
    r'val scaleAnim\s*=\s*remember \{ Animatable\(1f\) \}\n\s*val offsetXAnim = remember \{ Animatable\(0f\) \}\n\s*val offsetYAnim = remember \{ Animatable\(0f\) \}',
    r'val scaleAnim   = remember { Animatable(1f) }\n    var offsetX by remember { mutableFloatStateOf(0f) }\n    var offsetY by remember { mutableFloatStateOf(0f) }\n    val panJobHolder = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }\n\n    fun animatePanTo(targetX: Float, targetY: Float, spec: androidx.compose.animation.core.AnimationSpec<Float> = androidx.compose.animation.core.tween(280)) {\n        panJobHolder.value?.cancel()\n        panJobHolder.value = scope.launch {\n            launch { Animatable(offsetX).animateTo(targetX, spec) { offsetX = value } }\n            launch { Animatable(offsetY).animateTo(targetY, spec) { offsetY = value } }\n        }\n    }',
    content
)

# 2. Search Jump 1 (line 401)
content = re.sub(
    r'if \(scaleAnim\.value != 1f \|\| offsetXAnim\.value != 0f \|\| offsetYAnim\.value != 0f\) \{\n\s*launch \{ scaleAnim\.animateTo\(1f\) \}\n\s*launch \{ offsetXAnim\.animateTo\(0f\) \}\n\s*launch \{ offsetYAnim\.animateTo\(0f\) \}\n\s*\}',
    r'if (scaleAnim.value != 1f || offsetX != 0f || offsetY != 0f) {\n                        launch { scaleAnim.animateTo(1f) }\n                        animatePanTo(0f, 0f)\n                    }',
    content
)

# 5. Pointer Input (line 462)
content = re.sub(
    r'val down = awaitFirstDown\(requireUnconsumed = false\)\n\s*flingJob\?\.cancel\(\)\n\s*velocityTracker\.resetTracking\(\)\n\s*velocityTracker\.addPosition\(down\.uptimeMillis, down\.position\)\n\s*var zooming = false\n\s*var moved = false\n\s*val x0 = down\.position\.x\n\s*val y0 = down\.position\.y\n\s*var prevX = x0\n\s*var prevY = y0\n\n\s*scope\.launch \{\n\s*offsetXAnim\.updateBounds\(null, null\)\n\s*offsetYAnim\.updateBounds\(null, null\)\n\s*\}',
    r'val down = awaitFirstDown(requireUnconsumed = false)\n                                flingJob?.cancel()\n                                panJobHolder.value?.cancel()\n                                velocityTracker.resetTracking()\n                                velocityTracker.addPosition(down.uptimeMillis, down.position)\n                                var zooming = false\n                                var moved = false\n                                val x0 = down.position.x\n                                val y0 = down.position.y\n                                var prevX = x0\n                                var prevY = y0',
    content
)

# 6. Zoom calculations (lines 508-518)
content = re.sub(
    r'var nx = \(centroid\.x - pivotX\) \* \(1f - r\) \+ \(offsetXAnim\.value \+ pan\.x\) \* r\n\s*var ny = \(centroid\.y - pivotY\) \* \(1f - r\) \+ \(offsetYAnim\.value \+ pan\.y\) \* r\n\n\s*nx = clampPan\(nx, vpW, nextS\)\n\s*ny = clampPan\(ny, vpH, nextS\)\n\n\s*scope\.launch \{\n\s*scaleAnim\.snapTo\(nextS\)\n\s*offsetXAnim\.snapTo\(nx\)\n\s*offsetYAnim\.snapTo\(ny\)\n\s*\}',
    r'var nx = (centroid.x - pivotX) * (1f - r) + (offsetX + pan.x) * r\n                                        var ny = (centroid.y - pivotY) * (1f - r) + (offsetY + pan.y) * r\n\n                                        nx = clampPan(nx, vpW, nextS)\n                                        ny = clampPan(ny, vpH, nextS)\n\n                                        scope.launch {\n                                            scaleAnim.snapTo(nextS)\n                                        }\n                                        offsetX = nx\n                                        offsetY = ny',
    content
)

# 7. Panning (lines 533-543)
content = re.sub(
    r'val nextX = clampPan\(offsetXAnim\.value \+ dx, vpW, s\)\n\s*val nextY = clampPan\(offsetYAnim\.value \+ dy, vpH, s\)\n\s*scope\.launch \{\n\s*offsetXAnim\.snapTo\(nextX\)\n\s*offsetYAnim\.snapTo\(nextY\)\n\s*\}\n\s*val isVert = uiState\.scrollDirection == PreferencesManager\.SCROLL_VERTICAL\n\s*val unconsumedX = \(offsetXAnim\.value \+ dx\) - nextX\n\s*val unconsumedY = \(offsetYAnim\.value \+ dy\) - nextY',
    r'val nextX = clampPan(offsetX + dx, vpW, s)\n                                            val nextY = clampPan(offsetY + dy, vpH, s)\n                                            \n                                            val unconsumedX = (offsetX + dx) - nextX\n                                            val unconsumedY = (offsetY + dy) - nextY\n\n                                            offsetX = nextX\n                                            offsetY = nextY\n                                            \n                                            val isVert = uiState.scrollDirection == PreferencesManager.SCROLL_VERTICAL',
    content
)

# 8. Fling start & zoom double tap
content = re.sub(
    r'if \(sFinal < 1f\) scope\.launch \{\n\s*launch \{ scaleAnim\.animateTo\(1f, tween\(250\)\) \}\n\s*launch \{ offsetXAnim\.animateTo\(0f, tween\(250\)\) \}\n\s*launch \{ offsetYAnim\.animateTo\(0f, tween\(250\)\) \}\n\s*\} else scope\.launch \{\n\s*launch \{ offsetXAnim\.animateTo\(clampPan\(offsetXAnim\.value, vpW, sFinal\), tween\(250\)\) \}\n\s*launch \{ offsetYAnim\.animateTo\(clampPan\(offsetYAnim\.value, vpH, sFinal\), tween\(250\)\) \}\n\s*\}',
    r'if (sFinal < 1f) scope.launch {\n                                        launch { scaleAnim.animateTo(1f, tween(250)) }\n                                        animatePanTo(0f, 0f, tween(250))\n                                    } else {\n                                        animatePanTo(clampPan(offsetX, vpW, sFinal), clampPan(offsetY, vpH, sFinal), tween(250))\n                                    }',
    content
)

# 9. Fling body
content = re.sub(
    r'launch \{\n\s*var lastX = 0f\n\s*AnimationState\(initialValue = 0f, initialVelocity = velocityX\)\.animateDecay\(decay\) \{\n\s*val delta = value - lastX\n\s*lastX = value\n\s*val nextX = clampPan\(offsetXAnim\.value \+ delta, vpW, sFinal\)\n\s*val unconsumedX = delta - \(nextX - offsetXAnim\.value\)\n\s*scope\.launch \{ offsetXAnim\.snapTo\(nextX\) \}\n\s*if \(!isVert && abs\(unconsumedX\) > 0f\) \{\n\s*listState\.dispatchRawDelta\(-unconsumedX / sFinal\)\n\s*\}\n\s*\}\n\s*\}\n\s*launch \{\n\s*var lastY = 0f\n\s*AnimationState\(initialValue = 0f, initialVelocity = velocityY\)\.animateDecay\(decay\) \{\n\s*val delta = value - lastY\n\s*lastY = value\n\s*val nextY = clampPan\(offsetYAnim\.value \+ delta, vpH, sFinal\)\n\s*val unconsumedY = delta - \(nextY - offsetYAnim\.value\)\n\s*scope\.launch \{ offsetYAnim\.snapTo\(nextY\) \}\n\s*if \(isVert && abs\(unconsumedY\) > 0f\) \{\n\s*listState\.dispatchRawDelta\(-unconsumedY / sFinal\)\n\s*\}\n\s*\}\n\s*\}',
    r'launch {\n                                            var lastX = 0f\n                                            var accumulatedX = offsetX\n                                            AnimationState(initialValue = 0f, initialVelocity = velocityX).animateDecay(decay) {\n                                                val delta = value - lastX\n                                                lastX = value\n                                                val nextX = clampPan(accumulatedX + delta, vpW, sFinal)\n                                                val unconsumedX = (accumulatedX + delta) - nextX\n                                                accumulatedX = nextX\n                                                offsetX = nextX\n                                                if (!isVert && abs(unconsumedX) > 0f) {\n                                                    listState.dispatchRawDelta(-unconsumedX / sFinal)\n                                                }\n                                            }\n                                        }\n                                        launch {\n                                            var lastY = 0f\n                                            var accumulatedY = offsetY\n                                            AnimationState(initialValue = 0f, initialVelocity = velocityY).animateDecay(decay) {\n                                                val delta = value - lastY\n                                                lastY = value\n                                                val nextY = clampPan(accumulatedY + delta, vpH, sFinal)\n                                                val unconsumedY = (accumulatedY + delta) - nextY\n                                                accumulatedY = nextY\n                                                offsetY = nextY\n                                                if (isVert && abs(unconsumedY) > 0f) {\n                                                    listState.dispatchRawDelta(-unconsumedY / sFinal)\n                                                }\n                                            }\n                                        }',
    content
)

# 10. Double tap
content = re.sub(
    r'if \(scaleAnim\.value > 1\.5f\) \{\n\s*launch \{ scaleAnim\.animateTo\(1f, tween\(280\)\) \}\n\s*launch \{ offsetXAnim\.animateTo\(0f, tween\(280\)\) \}\n\s*launch \{ offsetYAnim\.animateTo\(0f, tween\(280\)\) \}\n\s*\} else \{\n\s*val ts = 2\.5f; val r2 = ts / scaleAnim\.value\n\s*val pivotX = vpW / 2f; val pivotY = vpH / 2f\n\s*val nx2 = clampPan\(\(x0 - pivotX\) \* \(1f - r2\) \+ offsetXAnim\.value \* r2, vpW, ts\)\n\s*val ny2 = clampPan\(\(y0 - pivotY\) \* \(1f - r2\) \+ offsetYAnim\.value \* r2, vpH, ts\)\n\s*launch \{ scaleAnim\.animateTo\(ts, tween\(280\)\) \}\n\s*launch \{ offsetXAnim\.animateTo\(nx2, tween\(280\)\) \}\n\s*launch \{ offsetYAnim\.animateTo\(ny2, tween\(280\)\) \}\n\s*\}',
    r'if (scaleAnim.value > 1.5f) {\n                                                launch { scaleAnim.animateTo(1f, tween(280)) }\n                                                animatePanTo(0f, 0f, tween(280))\n                                            } else {\n                                                val ts = 2.5f; val r2 = ts / scaleAnim.value\n                                                val pivotX = vpW / 2f; val pivotY = vpH / 2f\n                                                val nx2 = clampPan((x0 - pivotX) * (1f - r2) + offsetX * r2, vpW, ts)\n                                                val ny2 = clampPan((y0 - pivotY) * (1f - r2) + offsetY * r2, vpH, ts)\n                                                launch { scaleAnim.animateTo(ts, tween(280)) }\n                                                animatePanTo(nx2, ny2, tween(280))\n                                            }',
    content
)

# 11. PdfReaderList call
content = re.sub(
    r'offsetXAnim = offsetXAnim,\n\s*offsetYAnim = offsetYAnim',
    r'offsetX = offsetX,\n                        offsetY = offsetY',
    content
)

# 12. PdfReaderList signature
content = re.sub(
    r'offsetXAnim: Animatable<Float, \*>,\n\s*offsetYAnim: Animatable<Float, \*>',
    r'offsetX: Float,\n    offsetY: Float',
    content
)

# 13. PdfReaderList graphicsLayer
content = re.sub(
    r'translationX = offsetXAnim\.value,\n\s*translationY = offsetYAnim\.value',
    r'translationX = clampPan(offsetX, vpW, scaleAnim.value),\n            translationY = clampPan(offsetY, vpH, scaleAnim.value)',
    content
)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Replacement complete. Let's verify changes.")
