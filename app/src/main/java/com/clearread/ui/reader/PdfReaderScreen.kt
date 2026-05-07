package com.clearread.ui.reader

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Share
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalFocusManager
import com.clearread.ui.reader.ReadingMode
import com.clearread.ui.reader.ReaderUiState
import com.clearread.R
import com.clearread.data.local.PreferencesManager
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.clearread.ui.components.VerticalFastScroller

// ── Color filters ───────────────────────────────────────────────────────────

private val DarkColorFilter: ColorFilter = ColorFilter.tint(Color.White, BlendMode.Difference)

private val SepiaColorFilter: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(floatArrayOf(
        0.45f, 0.15f, 0.05f, 0f, 25f,
        0.10f, 0.35f, 0.09f, 0f, 15f,
        0.05f, 0.10f, 0.18f, 0f,  5f,
        0f,    0f,    0f,    1f,  0f
    ))
)

// ── System-bar helpers (WindowInsetsControllerCompat — works with enableEdgeToEdge) ──

private fun hideSystemBars(a: Activity) {
    val ctrl = WindowCompat.getInsetsController(a.window, a.window.decorView)
    ctrl.hide(WindowInsetsCompat.Type.systemBars())
    ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}

private fun showSystemBars(a: Activity) {
    val ctrl = WindowCompat.getInsetsController(a.window, a.window.decorView)
    ctrl.show(WindowInsetsCompat.Type.systemBars())
}

/** Clamp pan offset so zoomed content stays within viewport bounds. */
private fun clampPan(offset: Float, viewportSize: Int, scale: Float): Float {
    if (scale <= 1.001f) return 0f
    val bound = (viewportSize * (scale - 1f)) / 2f
    return offset.coerceIn(-bound, bound)
}

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    fileUri: String,
    initialPage: Int = -1,
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val viewConfiguration = LocalViewConfiguration.current
    val uiState by viewModel.uiState.collectAsState()
    val pageBitmaps by viewModel.pageBitmaps.collectAsState()
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx().toInt() }

    LaunchedEffect(fileUri, initialPage) { viewModel.loadPdf(fileUri, screenWidthPx, initialPage) }

    // Zoom/pan state — Animatable for smooth double-tap transitions
    val scaleAnim   = remember { Animatable(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val panJobHolder = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun animatePanTo(targetX: Float, targetY: Float, spec: androidx.compose.animation.core.AnimationSpec<Float> = androidx.compose.animation.core.tween(280)) {
        panJobHolder.value?.cancel()
        panJobHolder.value = scope.launch {
            launch { Animatable(offsetX).animateTo(targetX, spec) { offsetX = value } }
            launch { Animatable(offsetY).animateTo(targetY, spec) { offsetY = value } }
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var lastTapMs by remember { mutableLongStateOf(0L) }
    var lastTapX  by remember { mutableFloatStateOf(0f) }
    var lastTapY  by remember { mutableFloatStateOf(0f) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // Clear keyboard/focus when search is closed
    LaunchedEffect(uiState.isSearchOpen) {
        if (!uiState.isSearchOpen) {
            keyboardController?.hide()
            focusManager.clearFocus()
        } else {
            delay(300L) // Wait for animation
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Viewport size for pan clamping and dynamic scroll padding
    var vpW by remember { mutableIntStateOf(1) }
    var vpH by remember { mutableIntStateOf(1) }

    val importData = uiState.importDialogData
    if (importData != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* forced choice */ },
            title = { Text(stringResource(R.string.import_large_title)) },
            text = { 
                Column {
                    Text(stringResource(R.string.import_large_desc, com.clearread.utils.FileUtils.formatFileSize(importData.fileSize)))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.import_large_tip),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.handleImportDialog(true, screenWidthPx, initialPage) }) {
                    Text(stringResource(R.string.import_large_btn_copy))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleImportDialog(false, screenWidthPx, initialPage) }) {
                    Text(stringResource(R.string.import_large_btn_temp))
                }
            }
        )
    }

    // Fullscreen and System Bar Management
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Core visibility logic: Sync system bars with UI state and handle icon contrast
    val isAppDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // Calculate if the current screen background is dark
    val isScreenDark = when (uiState.readingMode) {
        ReadingMode.DARK -> true
        ReadingMode.NORMAL -> isAppDark
        ReadingMode.SEPIA -> false
    }

    LaunchedEffect(uiState.isFullscreen, showControls, isScreenDark) {
        val a = activity ?: return@LaunchedEffect
        val window = a.window
        val view = window.decorView
        val ctrl = WindowCompat.getInsetsController(window, view)
        
        if (uiState.isFullscreen) {
            if (showControls) {
                ctrl.show(WindowInsetsCompat.Type.systemBars())
                // Ensure icons have correct contrast on show
                ctrl.isAppearanceLightStatusBars = !isScreenDark
            } else {
                ctrl.hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            ctrl.show(WindowInsetsCompat.Type.systemBars())
            ctrl.isAppearanceLightStatusBars = !isScreenDark
        }
    }

    // Auto-hide controls (and thus system bars) when in fullscreen
    LaunchedEffect(showControls, uiState.isFullscreen) {
        if (uiState.isFullscreen && showControls) {
            delay(3500L) // Give user a bit more time to see the page number
            showControls = false
        }
    }

    // Ensure bars are shown when leaving the screen
    DisposableEffect(Unit) {
        onDispose { activity?.let { showSystemBars(it) } }
    }

    // Color filter
    val activeFilter: ColorFilter? = when (uiState.readingMode) {
        ReadingMode.NORMAL -> null; ReadingMode.DARK -> DarkColorFilter; ReadingMode.SEPIA -> SepiaColorFilter
    }
    val bgColor = when (uiState.readingMode) {
        ReadingMode.NORMAL -> MaterialTheme.colorScheme.background
        ReadingMode.DARK   -> Color(0xFF1C1C1C)
        ReadingMode.SEPIA  -> Color(0xFFBE9859)
    }


    Box(modifier = Modifier.fillMaxSize().background(bgColor).imePadding()) {
        // ── PDF content area ──
        val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
        val topPad = if (!uiState.isFullscreen) 64.dp + statusBarHeight else 0.dp

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
        } else if (uiState.errorMessage != null) {
            Column(modifier = Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text(uiState.errorMessage ?: stringResource(R.string.reader_error_unknown), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onNavigateBack) {
                    Text(stringResource(R.string.acc_back))
                }
            }
        } else if (uiState.textContent != null) {
            // ── Text/Markdown Viewer ──
            val scrollState = rememberScrollState()
            val textColor = when (uiState.readingMode) {
                ReadingMode.NORMAL -> MaterialTheme.colorScheme.onSurface
                ReadingMode.DARK -> Color.White
                ReadingMode.SEPIA -> Color(0xFF3E2723)
            }
            
            Box(modifier = Modifier.fillMaxSize().padding(top = topPad).windowInsetsPadding(WindowInsets.statusBars)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    Text(
                        text = uiState.textContent ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified // Let system decide or set fixed
                        ),
                        color = textColor
                    )
                    Spacer(Modifier.height(100.dp)) // Padding for bottom controls
                }
            }
        } else {
            // Keyed content ensures that when a new file loads, the list state is fresh
            key(fileUri) {
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = uiState.currentPage)
                val density = LocalDensity.current
                val imeInsets = WindowInsets.ime

                // Handle search scroll trigger (One-shot)
                LaunchedEffect(uiState.searchScrollTrigger) {
                    if (uiState.searchScrollTrigger == 0L) return@LaunchedEffect
                    
                    val activeMatch = uiState.searchResults.getOrNull(uiState.currentSearchIndex)
                    val targetPage = activeMatch?.pageIndex ?: uiState.currentPage
                    
                    // Fixed Position Logic: Put the match at 40% of the screen height (6/10 ratio from bottom)
                    // This is independent of keyboard height bugs and stays in the "Serbest" zone.
                    val viewportH = vpH.toFloat()
                    
                    // Calculate match position in page pixels
                    val aspectRatio = uiState.pageAspectRatios.getOrNull(targetPage) ?: 1.41f
                    val pageH = (vpW * aspectRatio)
                    
                    val targetOffset = if (activeMatch != null) {
                        val matchY = activeMatch.rect.top * pageH
                        // Put the match at 30% of the screen height (Plenty of space below for keyboard)
                        (matchY - (viewportH * 0.3f)).toInt()
                    } else 0
                    
                    // Loosen clamp to 95% to allow bottom-of-page words to reach the top half
                    val clampedOffset = targetOffset.coerceIn(0, (pageH * 0.95f).toInt())
                    
                    // Reset zoom before jumping
                    if (scaleAnim.value != 1f || offsetX != 0f || offsetY != 0f) {
                        launch { scaleAnim.animateTo(1f) }
                        animatePanTo(0f, 0f)
                    }
                    
                    listState.scrollToItem(targetPage, clampedOffset)
                }

                // Forced Jump (Manual navigation: bookmarks, slider, jump-to-page)
                LaunchedEffect(uiState.jumpScrollTrigger) {
                    if (uiState.jumpScrollTrigger == 0L) return@LaunchedEffect
                    
                    // Reset zoom before jumping to ensure correct positioning
                    if (scaleAnim.value != 1f || offsetX != 0f || offsetY != 0f) {
                        launch { scaleAnim.animateTo(1f) }
                        animatePanTo(0f, 0f)
                    }
                    
                    focusManager.clearFocus()
                    listState.scrollToItem(uiState.currentPage)
                }



                LaunchedEffect(listState, vpW, uiState.isSearchOpen, uiState.currentSearchIndex) {
                    // "Quiet Period": Wait for layout to completely settle before
                    // tracking page changes. This prevents the n-1 bug where the
                    // app briefly detects the wrong page during initial rendering.
                    delay(500L)
                    snapshotFlow { currentVisiblePage(listState, uiState) }
                        .distinctUntilChanged()
                        .collect { viewModel.onPageChanged(it, vpW) }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPad)
                        .clipToBounds()
                        .onSizeChanged { vpW = it.width; vpH = it.height }
                        .pointerInput(Unit) {
                            val velocityTracker = VelocityTracker()
                            var flingJob: kotlinx.coroutines.Job? = null
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                flingJob?.cancel()
                                panJobHolder.value?.cancel()
                                velocityTracker.resetTracking()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)
                                var zooming = false
                                var moved = false
                                val x0 = down.position.x
                                val y0 = down.position.y
                                var prevX = x0
                                var prevY = y0

                                while (true) {
                                    val isZoomed = scaleAnim.value > 1.01f
                                    val pass = if (isZoomed) PointerEventPass.Initial else PointerEventPass.Main
                                    val ev = awaitPointerEvent(pass)
                                    val anyDown = ev.changes.any { it.pressed }

                                    // EXCLUSIVE SCROLL: If not zoomed and it's a single finger,
                                    // immediately break the loop to give LazyColumn exclusive control.
                                    // But wait for a small movement to ensure it's not a tap.
                                    if (!isZoomed && ev.changes.size == 1 && !zooming) {
                                        val c = ev.changes.first()
                                        val dx = c.position.x - prevX
                                        val dy = c.position.y - prevY
                                        if (dx * dx + dy * dy > viewConfiguration.touchSlop * viewConfiguration.touchSlop) {
                                            moved = true // Mark as moved so we don't toggle controls
                                            break
                                        }
                                        // If it's a release (tap)
                                        if (!anyDown) break
                                    }

                                    if (ev.changes.size >= 2) {
                                        zooming = true
                                        val zoomRatio = ev.calculateZoom()
                                        val pan = ev.calculatePan()
                                        val centroid = ev.calculateCentroid()
                                        
                                        val prevS = scaleAnim.value
                                        val nextS = (prevS * zoomRatio).coerceIn(0.8f, 5.5f)
                                        val r = nextS / prevS
                                        val pivotX = vpW / 2f
                                        val pivotY = vpH / 2f
                                        var nx = (centroid.x - pivotX) * (1f - r) + (offsetX + pan.x) * r
                                        var ny = (centroid.y - pivotY) * (1f - r) + (offsetY + pan.y) * r

                                        nx = clampPan(nx, vpW, nextS)
                                        ny = clampPan(ny, vpH, nextS)

                                        scope.launch {
                                            scaleAnim.snapTo(nextS)
                                        }
                                        offsetX = nx
                                        offsetY = ny
                                        if (pass == PointerEventPass.Initial) {
                                            ev.changes.forEach { it.consume() }
                                        }
                                    } else if (ev.changes.size == 1 && !zooming) {
                                        val c = ev.changes.first()
                                        velocityTracker.addPosition(c.uptimeMillis, c.position)
                                        val dx = c.position.x - prevX
                                        val dy = c.position.y - prevY
                                        prevX = c.position.x; prevY = c.position.y

                                        if (!moved && dx * dx + dy * dy > viewConfiguration.touchSlop * viewConfiguration.touchSlop) moved = true

                                        if (moved && scaleAnim.value > 1.01f) {
                                            val s = scaleAnim.value
                                            val nextX = clampPan(offsetX + dx, vpW, s)
                                            val nextY = clampPan(offsetY + dy, vpH, s)
                                            
                                            val unconsumedX = (offsetX + dx) - nextX
                                            val unconsumedY = (offsetY + dy) - nextY

                                            offsetX = nextX
                                            offsetY = nextY
                                            
                                            val isVert = uiState.scrollDirection == PreferencesManager.SCROLL_VERTICAL
                                            
                                            val shouldPassToScroll = if (isVert) {
                                                abs(unconsumedY) > 0.1f && abs(dy) >= abs(dx)
                                            } else {
                                                abs(unconsumedX) > 0.1f && abs(dx) >= abs(dy)
                                            }
                                            
                                            if (!shouldPassToScroll) {
                                                c.consume()
                                            }
                                        }
                                    }
                                    if (!anyDown) break
                                }

                                val sFinal = scaleAnim.value
                                val velocity = velocityTracker.calculateVelocity()
                                val velocityX = velocity.x
                                val velocityY = velocity.y

                                if (zooming) {
                                    if (sFinal < 1f) scope.launch {
                                        launch { scaleAnim.animateTo(1f, tween(250)) }
                                        animatePanTo(0f, 0f, tween(250))
                                    } else {
                                        animatePanTo(clampPan(offsetX, vpW, sFinal), clampPan(offsetY, vpH, sFinal), tween(250))
                                    }
                                } else if (moved && sFinal > 1.01f && (abs(velocityX) > 200f || abs(velocityY) > 200f)) {
                                    flingJob = scope.launch {
                                        val decay = exponentialDecay<Float>()
                                        val isVert = uiState.scrollDirection == PreferencesManager.SCROLL_VERTICAL
                                        
                                        launch {
                                            var lastX = 0f
                                            var accumulatedX = offsetX
                                            AnimationState(initialValue = 0f, initialVelocity = velocityX).animateDecay(decay) {
                                                val delta = value - lastX
                                                lastX = value
                                                val nextX = clampPan(accumulatedX + delta, vpW, sFinal)
                                                val unconsumedX = (accumulatedX + delta) - nextX
                                                accumulatedX = nextX
                                                offsetX = nextX
                                                if (!isVert && abs(unconsumedX) > 0f) {
                                                    listState.dispatchRawDelta(-unconsumedX / sFinal)
                                                }
                                            }
                                        }
                                        launch {
                                            var lastY = 0f
                                            var accumulatedY = offsetY
                                            AnimationState(initialValue = 0f, initialVelocity = velocityY).animateDecay(decay) {
                                                val delta = value - lastY
                                                lastY = value
                                                val nextY = clampPan(accumulatedY + delta, vpH, sFinal)
                                                val unconsumedY = (accumulatedY + delta) - nextY
                                                accumulatedY = nextY
                                                offsetY = nextY
                                                if (isVert && abs(unconsumedY) > 0f) {
                                                    listState.dispatchRawDelta(-unconsumedY / sFinal)
                                                }
                                            }
                                        }
                                    }
                                } else if (!moved) {
                                    val now = System.currentTimeMillis()
                                    val dtx = x0 - lastTapX; val dty = y0 - lastTapY
                                    val slop = viewConfiguration.touchSlop
                                    if (now - lastTapMs < 350L && dtx * dtx + dty * dty < slop * slop * 9) {
                                        lastTapMs = 0L
                                        scope.launch {
                                            if (scaleAnim.value > 1.5f) {
                                                launch { scaleAnim.animateTo(1f, tween(280)) }
                                                animatePanTo(0f, 0f, tween(280))
                                            } else {
                                                val ts = 2.5f; val r2 = ts / scaleAnim.value
                                                val pivotX = vpW / 2f; val pivotY = vpH / 2f
                                                val nx2 = clampPan((x0 - pivotX) * (1f - r2) + offsetX * r2, vpW, ts)
                                                val ny2 = clampPan((y0 - pivotY) * (1f - r2) + offsetY * r2, vpH, ts)
                                                launch { scaleAnim.animateTo(ts, tween(280)) }
                                                animatePanTo(nx2, ny2, tween(280))
                                            }
                                        }
                                    } else {
                                        lastTapMs = now; lastTapX = x0; lastTapY = y0
                                        showControls = !showControls
                                    }
                                }
                            }
                        }
                ) {
                    PdfReaderList(
                        uiState = uiState,
                        listState = listState,
                        pageBitmaps = pageBitmaps,
                        activeFilter = activeFilter,
                        vpH = vpH,
                        vpW = vpW,
                        scaleAnim = scaleAnim,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                }

                // ── Vertical Fast Scroller Overlay ──
                if (uiState.pageCount > 1 && !uiState.isFullscreen) {
                    val thumbColor = when (uiState.readingMode) {
                        ReadingMode.NORMAL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        ReadingMode.DARK   -> Color.White.copy(alpha = 0.4f)
                        ReadingMode.SEPIA  -> Color(0xFF3E2723).copy(alpha = 0.4f)
                    }.toArgb()
                    
                    val bubbleColor = when (uiState.readingMode) {
                        ReadingMode.NORMAL -> MaterialTheme.colorScheme.primary
                        ReadingMode.DARK   -> Color.White
                        ReadingMode.SEPIA  -> Color(0xFF3E2723)
                    }.toArgb()
                    
                    val textColor = when (uiState.readingMode) {
                        ReadingMode.NORMAL -> MaterialTheme.colorScheme.onPrimary
                        ReadingMode.DARK   -> Color.Black
                        ReadingMode.SEPIA  -> Color(0xFFBE9859)
                    }.toArgb()

                    // Performance Optimization: Stabilize scroller progress calculation
                    val preciseProgress by remember {
                        derivedStateOf {
                            val total = uiState.pageCount
                            if (total > 1) {
                                val index = listState.firstVisibleItemIndex
                                val offset = listState.firstVisibleItemScrollOffset.toFloat()
                                val itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                                ((index + (offset / itemSize)) / (total - 1)).coerceIn(0f, 1f)
                            } else 0f
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            VerticalFastScroller(ctx).apply {
                                setOnScrollListener { progress ->
                                    scope.launch {
                                        // "Gear" Precision: Map scroller progress to LazyList indices + offsets
                                        val totalItems = listState.layoutInfo.totalItemsCount
                                        if (totalItems > 0) {
                                            val exactIndex = progress * (totalItems - 1)
                                            val index = exactIndex.toInt()
                                            // Calculate pixel offset within the page for "Window Scroll" feel
                                            val itemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                                            val offset = ((exactIndex - index) * itemHeight).toInt()
                                            
                                            listState.scrollToItem(index, offset)
                                        }
                                    }
                                }
                            }
                        },
                        update = { view ->
                            view.setPageCount(uiState.pageCount)
                            view.setScrollProgress(preciseProgress)
                            view.setColors(thumbColor, bubbleColor, textColor)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(100.dp) // Narrow hit area to prevent blocking reader gestures
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .padding(top = 64.dp, bottom = 16.dp)
                    )
                }
            }
        }

        // ── Overlays ──
        AnimatedVisibility(visible = !uiState.isFullscreen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)) {
            TopAppBar(
                title = { Text(uiState.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
                navigationIcon = { 
                    IconButton(onClick = onNavigateBack) { 
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") 
                    } 
                },
                actions = {
                    if (uiState.isSearchSupported) {
                        IconButton(onClick = { viewModel.setSearchOpen(!uiState.isSearchOpen) }) { 
                            Icon(Icons.Outlined.Search, stringResource(R.string.home_search)) 
                        }
                    }
                    ReadingModePicker(currentMode = uiState.readingMode, onModeSelected = { viewModel.setReadingMode(it) })
                    IconButton(onClick = { viewModel.toggleBookmark() }) { Icon(if (uiState.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, stringResource(R.string.home_bookmarks)) }
                    IconButton(onClick = { viewModel.toggleFullscreen() }) { Icon(Icons.Outlined.Fullscreen, stringResource(R.string.acc_settings)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets.statusBars
            )
        }



        // ── Search Bar Overlay ──
        AnimatedVisibility(visible = uiState.isSearchOpen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(8.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.search(it, vpW) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.home_search)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.totalMatchesCount > 0) {
                                Text(
                                    text = "${uiState.currentSearchIndex + 1} / ${uiState.totalMatchesCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            } else if (uiState.isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp).padding(horizontal = 8.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { viewModel.prevSearchResult(vpW) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, null)
                            }
                            IconButton(onClick = { viewModel.nextSearchResult(vpW) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, null)
                            }
                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                            IconButton(onClick = { viewModel.setSearchOpen(false) }) { 
                                Icon(Icons.Outlined.Close, null) 
                            } 
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        AnimatedVisibility(visible = showControls && uiState.pageCount > 0 && uiState.searchQuery.isEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars)) {
            Box(Modifier.padding(bottom = 16.dp).clip(CircleShape) // Modern rounded pill
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.nav_page_format, uiState.currentPage + 1, uiState.pageCount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.inverseOnSurface)
            }
        }

        if (uiState.isFullscreen && showControls) {
            IconButton(
                onClick = { viewModel.toggleFullscreen() }, 
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(top = 28.dp, end = 16.dp)
            ) {
                Box(modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.7f)).padding(8.dp)) {
                    Icon(Icons.Outlined.FullscreenExit, stringResource(R.string.action_close), tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PdfReaderList(
    uiState: ReaderUiState,
    listState: LazyListState,
    pageBitmaps: Map<Int, Bitmap>,
    activeFilter: ColorFilter?,
    vpH: Int,
    vpW: Int,
    scaleAnim: Animatable<Float, *>,
    offsetX: Float,
    offsetY: Float
) {
    val navPad = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val isVertical = uiState.scrollDirection == PreferencesManager.SCROLL_VERTICAL
    val screenHeightDp = with(LocalDensity.current) { vpH.toDp() }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val overscanFactor = 0.6f
    
    val extraPad = if (isVertical) {
        if (screenHeightDp > 0.dp) (screenHeightDp / overscanFactor) - screenHeightDp else 0.dp
    } else {
        if (screenWidthDp > 0.dp) (screenWidthDp / overscanFactor) - screenWidthDp else 0.dp
    }
    
    val startPad = if (!isVertical) extraPad / 2 else 0.dp
    val endPad = if (!isVertical) extraPad / 2 else 0.dp
    val topPadItems = if (isVertical) extraPad / 2 else 0.dp
    val bottomPadItems = if (isVertical) extraPad / 2 + navPad + 56.dp else navPad + 56.dp

    val layoutModifier = Modifier
        .fillMaxSize()
        .graphicsLayer(
            scaleX = scaleAnim.value,
            scaleY = scaleAnim.value,
            translationX = clampPan(offsetX, vpW, scaleAnim.value),
            translationY = clampPan(offsetY, vpH, scaleAnim.value)
        )
        .layout { measurable, constraints ->
            if (constraints.maxHeight == 0 || constraints.maxWidth == 0) {
                val p = measurable.measure(constraints)
                layout(p.width, p.height) { p.place(0, 0) }
            } else {
                val factor = overscanFactor
                if (isVertical) {
                    val expandedHeight = (constraints.maxHeight / factor).toInt()
                    val placeable = measurable.measure(constraints.copy(minHeight = expandedHeight, maxHeight = expandedHeight))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, -(placeable.height - constraints.maxHeight) / 2)
                    }
                } else {
                    val expandedWidth = (constraints.maxWidth / factor).toInt()
                    val placeable = measurable.measure(constraints.copy(minWidth = expandedWidth, maxWidth = expandedWidth))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(-(placeable.width - constraints.maxWidth) / 2, 0)
                    }
                }
            }
        }

    if (isVertical) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = topPadItems, bottom = bottomPadItems),
            modifier = layoutModifier,
            userScrollEnabled = true
        ) {
            items(uiState.pageCount) { pageIndex ->
                val bmp = pageBitmaps[pageIndex]
                
                if (bmp != null) PdfPage(
                    bitmap = bmp, 
                    colorFilter = activeFilter, 
                    isVertical = true,
                    pageNumber = pageIndex,
                    uiState = uiState
                )
                else {
                    val ratio = uiState.pageAspectRatios.getOrNull(pageIndex) ?: 1.414f
                    val h = screenWidthDp * ratio
                    Box(Modifier.fillMaxWidth().height(h).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    } else {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(start = startPad, end = endPad),
            modifier = layoutModifier,
            userScrollEnabled = true
        ) {
            items(uiState.pageCount) { pageIndex ->
                val bmp = pageBitmaps[pageIndex]

                if (bmp != null) PdfPage(
                    bitmap = bmp, 
                    colorFilter = activeFilter, 
                    isVertical = false,
                    pageNumber = pageIndex,
                    uiState = uiState
                )
                else {
                    val ratio = uiState.pageAspectRatios.getOrNull(pageIndex) ?: 1.414f
                    val w = screenHeightDp / ratio
                    Box(Modifier.fillMaxHeight().width(w).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun currentVisiblePage(listState: LazyListState, uiState: ReaderUiState): Int {
    val info = listState.layoutInfo
    val items = info.visibleItemsInfo
    if (items.isEmpty()) return uiState.currentPage

    // Viewport Calculations (accounting for 0.6x overscan expansion)
    val viewportSize = info.viewportEndOffset - info.viewportStartOffset
    val overscanFactor = 0.6f
    val physicalSize = (viewportSize * overscanFactor).toInt()
    val physicalStart = info.viewportStartOffset + (viewportSize - physicalSize) / 2
    
    // CENTER-POINT DETECTION:
    // We check which item is directly under the vertical center of the physical screen.
    val physicalCenter = physicalStart + (physicalSize / 2)
    
    // Asymmetric Hysteresis: If the current page is still covering the center point 
    // (with a small buffer), we strictly stay on it.
    val deadZone = 15 // 15px buffer to prevent jitter
    val currentItem = items.find { it.index == uiState.currentPage }
    if (currentItem != null) {
        val start = currentItem.offset
        val end = start + currentItem.size
        if (physicalCenter >= start - deadZone && physicalCenter <= end + deadZone) {
            return uiState.currentPage
        }
    }

    // Fallback: Find the new item that is under the center point
    return items.find { item ->
        val start = item.offset
        val end = start + item.size
        physicalCenter >= start && physicalCenter <= end
    }?.index ?: uiState.currentPage
}

@Composable
private fun ReadingModePicker(currentMode: ReadingMode, onModeSelected: (ReadingMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                when (currentMode) { ReadingMode.NORMAL -> Icons.Outlined.LightMode; ReadingMode.DARK -> Icons.Outlined.DarkMode; ReadingMode.SEPIA -> Icons.Outlined.LightMode },
                stringResource(R.string.settings_appearance), tint = if (currentMode == ReadingMode.SEPIA) Color(0xFFD4A056) else MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.reader_mode_normal)) }, onClick = { onModeSelected(ReadingMode.NORMAL); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.reader_mode_dark)) }, onClick = { onModeSelected(ReadingMode.DARK); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.reader_mode_sepia)) }, onClick = { onModeSelected(ReadingMode.SEPIA); expanded = false })
        }
    }
}

@Composable
fun PdfPage(
    modifier: Modifier = Modifier,
    bitmap: Bitmap, 
    colorFilter: ColorFilter?, 
    isVertical: Boolean,
    pageNumber: Int,
    uiState: ReaderUiState
) {
    val img = remember(bitmap) { bitmap.asImageBitmap() }
    val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
    
    Box(
        modifier = modifier
            .then(
                if (isVertical) Modifier.fillMaxWidth().aspectRatio(1f / aspectRatio)
                else Modifier.fillMaxHeight().aspectRatio(1f / aspectRatio)
            )
            .background(Color.Transparent)
    ) {
        Image(
            bitmap = img,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = colorFilter
        )
        
        // Highlights overlay
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val scaleX = size.width
            val scaleY = size.height
                    // Draw highlights for ALL visible pages that have matches
                // (Removed the if (uiState.currentPage == pageNumber) check)
                
                val activeGlobalIndex = uiState.currentSearchIndex
                val isTargetPage = activeGlobalIndex != -1 && uiState.occurrencePageIndices.getOrNull(activeGlobalIndex) == pageNumber
                
                val activeLocalIndex = if (isTargetPage) {
                    var localIdx = 0
                    for (i in 0 until activeGlobalIndex) {
                        if (uiState.occurrencePageIndices[i] == pageNumber) {
                            localIdx++
                        }
                    }
                    localIdx
                } else -1

                val pageMatches = uiState.searchResults.filter { it.pageIndex == pageNumber }
                
                pageMatches.forEachIndexed { index, match ->
                    val color = if (index == activeLocalIndex) {
                        Color.Blue.copy(alpha = 0.5f)
                    } else {
                        Color.Yellow.copy(alpha = 0.5f)
                    }
                    
                    drawRect(
                        color = color,
                        topLeft = Offset(match.rect.left * scaleX, match.rect.top * scaleY),
                        size = Size(
                            (match.rect.right - match.rect.left) * scaleX,
                            (match.rect.bottom - match.rect.top) * scaleY
                        )
                    )
                }
        }
    }
}

