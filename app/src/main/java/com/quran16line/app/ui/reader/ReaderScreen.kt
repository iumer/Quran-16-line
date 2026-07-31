package com.quran16line.app.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quran16line.app.data.PdfMushafSource
import com.quran16line.app.ui.bookmarks.BookmarksSheet
import com.quran16line.app.ui.search.SearchSheet
import com.quran16line.app.ui.theme.Chrome
import com.quran16line.app.ui.theme.Highlight
import com.quran16line.app.ui.theme.Ink
import com.quran16line.app.ui.theme.Muted
import com.quran16line.app.ui.theme.Parchment
import com.quran16line.app.ui.theme.Rule
import com.quran16line.app.ui.theme.WarmGrey
import com.quran16line.app.viewmodel.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(vm: ReaderViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var chromeVisible by remember { mutableStateOf(true) }
    var chromePulse by remember { mutableStateOf(0) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    LaunchedEffect(chromeVisible, chromePulse, state.currentPage) {
        if (chromeVisible) {
            delay(2800)
            chromeVisible = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Parchment
    ) { padding ->
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

        val pagerState = rememberPagerState(
            initialPage = (state.currentPage - 1).coerceIn(0, state.pageCount - 1),
            pageCount = { state.pageCount }
        )

        LaunchedEffect(state.currentPage) {
            val target = state.currentPage - 1
            if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
                pagerState.scrollToPage(target)
            }
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }
                .distinctUntilChanged()
                .collect { pageIndex ->
                    vm.onPageChanged(pageIndex + 1)
                    chromePulse++
                }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Parchment)
        ) {
            var pageZoomed by remember { mutableStateOf(false) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondBoundsPageCount = 1,
                    userScrollEnabled = !pageZoomed
                ) { pageIndex ->
                    val pageNumber = pageIndex + 1
                    PdfMushafPage(
                        pageNumber = pageNumber,
                        highlightLine = state.highlight
                            ?.takeIf { it.page == pageNumber }
                            ?.lineIndex,
                        linesPerPage = state.linesPerPage,
                        render = { width -> vm.renderPage(pageNumber, width) },
                        onZoomChanged = { zoomed -> pageZoomed = zoomed },
                        onBlankTap = {
                            chromeVisible = !chromeVisible
                            chromePulse++
                        },
                        onLineTap = { line ->
                            vm.onLineTapped(pageNumber, line)
                            chromeVisible = true
                            chromePulse++
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopBar(
                    title = "Quran 16-Line",
                    subtitle = state.pageLabel.ifBlank { "Page ${state.currentPage}" },
                    bookmarked = state.isBookmarked,
                    onBookmark = {
                        vm.toggleBookmark()
                        chromePulse++
                    },
                    onShowBookmarks = {
                        vm.openBookmarks(true)
                        chromePulse++
                    },
                    onSearch = {
                        vm.openSearch(true)
                        chromePulse++
                    }
                )
            }

            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderBottomBar(
                    page = state.currentPage,
                    pageCount = state.pageCount,
                    onSeek = { page ->
                        scope.launch {
                            pagerState.scrollToPage(page - 1)
                            vm.onPageChanged(page)
                            chromePulse++
                        }
                    },
                    onInteract = { chromePulse++ }
                )
            }

            if (state.showSearch) {
                SearchSheet(
                    surahs = state.surahs,
                    pageCount = state.pageCount,
                    onDismiss = { vm.openSearch(false) },
                    onJumpPage = vm::jumpToPage,
                    onJumpSurah = vm::jumpToSurah,
                    onJumpAyah = vm::jumpToAyah,
                    totalVerses = vm::surahTotalVerses
                )
            }

            if (state.showBookmarks) {
                BookmarksSheet(
                    bookmarks = state.bookmarks,
                    onDismiss = { vm.openBookmarks(false) },
                    onOpen = vm::jumpToPage
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
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
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
    onInteract: () -> Unit
) {
    var sliderValue by remember(page) { mutableFloatStateOf(page.toFloat()) }

    Column(
        modifier = Modifier
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
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 4f)
            scale = newScale
            offset = if (newScale <= 1.01f) {
                Offset.Zero
            } else {
                offset + panChange
            }
            onZoomChanged(newScale > 1.01f)
        }

        LaunchedEffect(pageNumber, widthPx) {
            bitmap = render(widthPx)
            scale = 1f
            offset = Offset.Zero
            onZoomChanged(false)
        }

        val pageBitmap = bitmap
        if (pageBitmap == null || pageBitmap.isRecycled) {
            CircularProgressIndicator(color = Ink)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = transformState)
                    .pointerInput(pageNumber, highlightLine, linesPerPage, scale) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.01f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                    onZoomChanged(false)
                                } else {
                                    scale = 2.2f
                                    onZoomChanged(true)
                                }
                            },
                            onTap = { tapOffset: Offset ->
                                if (scale > 1.01f) return@detectTapGestures
                                val lineHeight = size.height / linesPerPage.toFloat()
                                if (lineHeight <= 0f) {
                                    onBlankTap()
                                    return@detectTapGestures
                                }
                                val line = (tapOffset.y / lineHeight)
                                    .toInt()
                                    .coerceIn(0, linesPerPage - 1)
                                onLineTap(line)
                            }
                        )
                    }
            ) {
                Image(
                    bitmap = pageBitmap.asImageBitmap(),
                    contentDescription = "Quran page $pageNumber",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                if (highlightLine != null && scale <= 1.01f) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(linesPerPage) { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(
                                        if (index == highlightLine) Highlight.copy(alpha = 0.38f)
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
