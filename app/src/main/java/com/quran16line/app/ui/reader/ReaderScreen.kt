package com.quran16line.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quran16line.app.data.PageLine
import com.quran16line.app.ui.bookmarks.BookmarksSheet
import com.quran16line.app.ui.search.SearchSheet
import com.quran16line.app.ui.theme.AmiriQuran
import com.quran16line.app.ui.theme.Chrome
import com.quran16line.app.ui.theme.Highlight
import com.quran16line.app.ui.theme.Ink
import com.quran16line.app.ui.theme.Muted
import com.quran16line.app.ui.theme.Paper
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondBoundsPageCount = 1
                ) { pageIndex ->
                    val pageNumber = pageIndex + 1
                    MushafPage(
                        pageNumber = pageNumber,
                        lines = vm.pageLines(pageNumber),
                        highlightLine = state.highlight
                            ?.takeIf { it.page == pageNumber }
                            ?.lineIndex,
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
                    subtitle = state.pageLabel,
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
            Text("$page / $pageCount", color = Muted, style = MaterialTheme.typography.bodyMedium)
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
private fun MushafPage(
    pageNumber: Int,
    lines: List<PageLine>,
    highlightLine: Int?,
    onBlankTap: () -> Unit,
    onLineTap: (Int) -> Unit
) {
    val resolved = remember(pageNumber, lines) {
        val padded = lines.toMutableList()
        while (padded.size < 16) {
            padded.add(PageLine("empty", "", null, null, null))
        }
        padded.take(16)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Paper.copy(alpha = 0.35f), Parchment, Parchment)
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onBlankTap() }
            .padding(horizontal = 14.dp, vertical = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Rule)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "page $pageNumber · 16 lines",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = WarmGrey,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            resolved.forEachIndexed { index, line ->
                val selected = highlightLine == index
                val bg = if (selected) Highlight else Color.Transparent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(bg)
                        .semantics { this.selected = selected }
                        .clickable {
                            if (line.type != "empty" && line.text.isNotBlank()) {
                                onLineTap(index)
                            } else {
                                onBlankTap()
                            }
                        }
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (line.type) {
                        "header" -> FittedQuranLine(
                            text = line.text,
                            maxFontSp = 24f,
                            minFontSp = 14f,
                            boldHeader = true
                        )
                        "empty" -> Spacer(modifier = Modifier.fillMaxWidth())
                        else -> FittedQuranLine(
                            text = line.text,
                            maxFontSp = 20f,
                            minFontSp = 12f,
                            boldHeader = false
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Rule)
        )
    }
}

@Composable
private fun FittedQuranLine(
    text: String,
    maxFontSp: Float,
    minFontSp: Float,
    boldHeader: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val fontSize = remember(text, maxWidthPx, maxFontSp, minFontSp) {
            var size = maxFontSp
            while (size > minFontSp) {
                val result = measurer.measure(
                    text = text,
                    style = TextStyle(
                        fontFamily = AmiriQuran,
                        fontSize = size.sp,
                        textDirection = TextDirection.Rtl
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                if (result.size.width <= maxWidthPx) break
                size -= 0.5f
            }
            size.coerceAtLeast(minFontSp)
        }
        Text(
            text = text,
            fontFamily = AmiriQuran,
            fontSize = fontSize.sp,
            fontWeight = if (boldHeader) FontWeight.SemiBold else FontWeight.Normal,
            color = Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(textDirection = TextDirection.Rtl),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
