package com.quran16line.app.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quran16line.app.data.PdfMushafSource
import com.quran16line.app.ui.bookmarks.BookmarksSheet
import com.quran16line.app.ui.home.HomeScreen
import com.quran16line.app.ui.search.SearchSheet
import com.quran16line.app.ui.theme.Chrome
import com.quran16line.app.ui.theme.Highlight
import com.quran16line.app.ui.theme.Ink
import com.quran16line.app.ui.theme.Muted
import com.quran16line.app.ui.theme.Parchment
import com.quran16line.app.ui.theme.Rule
import com.quran16line.app.ui.theme.WarmGrey
import com.quran16line.app.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(vm: ReaderViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.persistReadingPosition()
                vm.prepareLaunchChooser()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Parchment
    ) { padding ->
        if (state.loadError != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.loadError ?: "",
                    color = Ink,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Scaffold
        }

        if (!state.ready || state.pageCount == 0) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Ink)
            }
            return@Scaffold
        }

        if (state.showHome) {
            Box(Modifier.padding(padding)) {
                HomeScreen(
                    resumeLabel = state.resumeLabel,
                    onResume = vm::resumeReading,
                    onStartFromFirstPage = vm::startFromFirstPage,
                    onSearch = vm::openSearchFromHome
                )
                if (state.showSearch) {
                    SearchSheet(
                        surahs = state.surahs,
                        pageCount = state.pageCount,
                        onDismiss = { vm.openSearch(false) },
                        onJumpPage = { page ->
                            vm.jumpToPage(page)
                        },
                        onJumpSurah = { id ->
                            vm.jumpToSurah(id)
                        },
                        onJumpAyah = { sid, ayah ->
                            vm.jumpToAyah(sid, ayah)
                        },
                        totalVerses = vm::surahTotalVerses,
                        previewPageLabel = vm::previewPageLabel,
                        resolvePageQuery = vm::resolvePageQuery,
                        pageForAyahPreview = vm::pageForAyah
                    )
                }
            }
            return@Scaffold
        }

        val pagerState = rememberPagerState(
            initialPage = (state.currentPage - 1).coerceIn(0, state.pageCount - 1),
            pageCount = { state.pageCount }
        )
        var pageZoomed by remember { mutableStateOf(false) }
        // Only programmatically sync pager for jumps (search/slider), not user swipes.
        var pendingJumpPage by remember { mutableStateOf<Int?>(null) }

        LaunchedEffect(pendingJumpPage) {
            val jump = pendingJumpPage ?: return@LaunchedEffect
            val target = (jump - 1).coerceIn(0, state.pageCount - 1)
            if (pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
            pendingJumpPage = null
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }
                .distinctUntilChanged()
                .collect { pageIndex ->
                    vm.onPageChanged(pageIndex + 1)
                }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Parchment)
        ) {
            // RTL pager: swipe right → next mushaf page (same as a physical mushaf).
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondBoundsPageCount = 1,
                    userScrollEnabled = !pageZoomed,
                    key = { it }
                ) { pageIndex ->
                    val pageNumber = pageIndex + 1
                    PdfMushafPage(
                        pageNumber = pageNumber,
                        highlightLine = state.highlight
                            ?.takeIf { it.page == pageNumber }
                            ?.lineIndex,
                        linesPerPage = state.linesPerPage,
                        isActive = pagerState.settledPage == pageIndex,
                        render = { width -> vm.renderPage(pageNumber, width) },
                        onZoomChanged = { zoomed ->
                            if (pagerState.settledPage == pageIndex) {
                                pageZoomed = zoomed
                            }
                        },
                        onBlankTap = { },
                        onLineTap = { line ->
                            vm.onLineTapped(pageNumber, line)
                        }
                    )
                }
            }

            ReaderTopBar(
                title = "Quran Pak 16 Lines",
                subtitle = state.pageLabel.ifBlank { "Page ${state.currentPage}" },
                bookmarked = state.isBookmarked,
                onBookmark = { vm.toggleBookmark() },
                onShowBookmarks = { vm.openBookmarks(true) },
                onSearch = { vm.openSearch(true) },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            ReaderBottomBar(
                page = state.currentPage,
                pageCount = state.pageCount,
                onSeek = { page ->
                    pendingJumpPage = page
                    vm.onPageChanged(page)
                },
                onInteract = { },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (state.showSearch) {
                SearchSheet(
                    surahs = state.surahs,
                    pageCount = state.pageCount,
                    onDismiss = { vm.openSearch(false) },
                    onJumpPage = { page ->
                        pendingJumpPage = page
                        vm.jumpToPage(page)
                    },
                    onJumpSurah = { id ->
                        val page = state.surahs.firstOrNull { it.id == id }?.page
                        if (page != null) pendingJumpPage = page
                        vm.jumpToSurah(id)
                    },
                    onJumpAyah = { sid, ayah ->
                        val page = vm.pageForAyah(sid, ayah)
                        if (page != null) pendingJumpPage = page
                        vm.jumpToAyah(sid, ayah)
                    },
                    totalVerses = vm::surahTotalVerses,
                    previewPageLabel = vm::previewPageLabel,
                    resolvePageQuery = vm::resolvePageQuery,
                    pageForAyahPreview = vm::pageForAyah
                )
            }

            if (state.showBookmarks) {
                BookmarksSheet(
                    bookmarks = state.bookmarks,
                    onDismiss = { vm.openBookmarks(false) },
                    onOpen = { page ->
                        pendingJumpPage = page
                        vm.jumpToPage(page)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    subtitle: String,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onShowBookmarks: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Chrome.copy(alpha = 0.96f), Chrome.copy(alpha = 0.75f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Ink
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onBookmark,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        ) {
            Icon(
                imageVector = if (bookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (bookmarked) "Remove bookmark" else "Add bookmark",
                tint = if (bookmarked) Color(0xFFB0892E) else Ink
            )
        }
        IconButton(
            onClick = onShowBookmarks,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        ) {
            Icon(Icons.Filled.Bookmarks, contentDescription = "Open bookmarks", tint = Ink)
        }
        IconButton(
            onClick = onSearch,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Search Quran", tint = Ink)
        }
    }
}

@Composable
private fun ReaderBottomBar(
    page: Int,
    pageCount: Int,
    onSeek: (Int) -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(page) { mutableFloatStateOf(page.toFloat()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Chrome.copy(alpha = 0.8f), Chrome.copy(alpha = 0.96f))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Page ${sliderValue.toInt()}", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Text("$page / $pageCount · 16 lines", color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = sliderValue.coerceIn(1f, pageCount.toFloat()),
            onValueChange = {
                sliderValue = it
                onInteract()
            },
            onValueChangeFinished = {
                onSeek(sliderValue.toInt().coerceIn(1, pageCount))
            },
            valueRange = 1f..pageCount.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = WarmGrey,
                inactiveTrackColor = Rule
            )
        )
    }
}

@Composable
private fun PdfMushafPage(
    pageNumber: Int,
    highlightLine: Int?,
    linesPerPage: Int,
    isActive: Boolean,
    render: suspend (widthPx: Int) -> Bitmap?,
    onZoomChanged: (Boolean) -> Unit,
    onBlankTap: () -> Unit,
    onLineTap: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        var bitmap by remember(pageNumber, widthPx) { mutableStateOf<Bitmap?>(null) }
        var scale by remember(pageNumber) { mutableFloatStateOf(1f) }
        var offset by remember(pageNumber) { mutableStateOf(Offset.Zero) }
        val zoomed = scale > 1.01f

        LaunchedEffect(pageNumber, widthPx) {
            bitmap = render(widthPx)
        }

        LaunchedEffect(isActive) {
            if (!isActive && scale != 1f) {
                scale = 1f
                offset = Offset.Zero
            }
            if (isActive) onZoomChanged(scale > 1.01f)
        }

        val pageBitmap = bitmap
        if (pageBitmap == null || pageBitmap.isRecycled) {
            CircularProgressIndicator(color = Ink)
        } else {
            // Size the interactive page to the fitted bitmap so highlight/taps match the PDF,
            // not the full screen (avoids overflow into parchment margins).
            val containerW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val containerH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val bmpW = pageBitmap.width.toFloat().coerceAtLeast(1f)
            val bmpH = pageBitmap.height.toFloat().coerceAtLeast(1f)
            val fit = min(containerW / bmpW, containerH / bmpH)
            val drawWpx = (bmpW * fit).toInt().coerceAtLeast(1)
            val drawHpx = (bmpH * fit).toInt().coerceAtLeast(1)
            val drawW = with(density) { drawWpx.toDp() }
            val drawH = with(density) { drawHpx.toDp() }

            Box(
                modifier = Modifier
                    .width(drawW)
                    .height(drawH)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    // One continuous gesture detector: pinch always works; pan only when zoomed.
                    // Avoids swapping modifiers mid-gesture (which made the first pinch feel stuck).
                    .pointerInput(pageNumber) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.size >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    if (zoomChange != 1f || panChange != Offset.Zero) {
                                        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                                        scale = newScale
                                        offset = if (newScale <= 1.01f) {
                                            Offset.Zero
                                        } else {
                                            offset + panChange
                                        }
                                        if (isActive) onZoomChanged(newScale > 1.01f)
                                        pressed.forEach { it.consume() }
                                    }
                                } else if (scale > 1.01f && pressed.size == 1) {
                                    val panChange = event.calculatePan()
                                    if (panChange != Offset.Zero) {
                                        offset += panChange
                                        pressed.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(pageNumber, highlightLine, linesPerPage, zoomed) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (zoomed) {
                                    scale = 1f
                                    offset = Offset.Zero
                                    if (isActive) onZoomChanged(false)
                                } else {
                                    scale = 2.2f
                                    if (isActive) onZoomChanged(true)
                                }
                            },
                            onTap = { tapOffset: Offset ->
                                if (zoomed) return@detectTapGestures
                                val line = lineIndexForTap(
                                    tapY = tapOffset.y,
                                    pageHeight = size.height.toFloat(),
                                    linesPerPage = linesPerPage
                                )
                                if (line == null) onBlankTap() else onLineTap(line)
                            }
                        )
                    }
            ) {
                // Keep page chrome LTR so highlight/tap bands are full-width regardless of pager RTL.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Image(
                        bitmap = pageBitmap.asImageBitmap(),
                        contentDescription = "Quran page $pageNumber",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    if (highlightLine != null && !zoomed) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = drawW * PAGE_CONTENT_INSET_X,
                                    vertical = 0.dp
                                )
                                .padding(
                                    top = drawH * PAGE_CONTENT_INSET_TOP,
                                    bottom = drawH * PAGE_CONTENT_INSET_BOTTOM
                                )
                        ) {
                            repeat(linesPerPage) { index ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(
                                            if (index == highlightLine) Highlight.copy(alpha = 0.40f)
                                            else Color.Transparent
                                        )
                                        .semantics { selected = index == highlightLine }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Fractions of the fitted PDF page outside the 16 text-line grid (measured from Taj pages). */
private const val PAGE_CONTENT_INSET_X = 0.110f
private const val PAGE_CONTENT_INSET_TOP = 0.072f
private const val PAGE_CONTENT_INSET_BOTTOM = 0.056f

internal fun lineIndexForTap(
    tapY: Float,
    pageHeight: Float,
    linesPerPage: Int,
    insetTop: Float = PAGE_CONTENT_INSET_TOP,
    insetBottom: Float = PAGE_CONTENT_INSET_BOTTOM
): Int? {
    if (pageHeight <= 0f || linesPerPage < 1) return null
    val top = pageHeight * insetTop
    val bottom = pageHeight * (1f - insetBottom)
    if (tapY < top || tapY > bottom) return null
    val band = (bottom - top) / linesPerPage.toFloat()
    if (band <= 0f) return null
    return ((tapY - top) / band).toInt().coerceIn(0, linesPerPage - 1)
}
