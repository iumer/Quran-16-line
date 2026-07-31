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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(vm: ReaderViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var chromeVisible by remember { mutableStateOf(true) }

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
                }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(listOf(Chrome, Parchment, Parchment))
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut()) {
                    ReaderTopBar(
                        title = "Quran 16-Line",
                        subtitle = state.pageLabel,
                        bookmarked = state.isBookmarked,
                        onBookmark = vm::toggleBookmark,
                        onShowBookmarks = { vm.openBookmarks(true) },
                        onSearch = { vm.openSearch(true) }
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        beyondBoundsPageCount = 1
                    ) { pageIndex ->
                        val pageNumber = pageIndex + 1
                        MushafPage(
                            pageNumber = pageNumber,
                            lines = vm.pageLines(pageNumber),
                            highlightLine = state.highlight
                                ?.takeIf { it.page == pageNumber }
                                ?.lineIndex,
                            onToggleChrome = { chromeVisible = !chromeVisible },
                            onLineTap = { line -> vm.onLineTapped(pageNumber, line) }
                        )
                    }
                }

                AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut()) {
                    ReaderBottomBar(
                        page = state.currentPage,
                        pageCount = state.pageCount,
                        onSeek = { page ->
                            scope.launch {
                                pagerState.scrollToPage(page - 1)
                                vm.onPageChanged(page)
                            }
                        }
                    )
                }
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
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        ChromeIcon(onClick = onBookmark) {
            Icon(
                imageVector = if (bookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Bookmark page",
                tint = Ink
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        ChromeIcon(onClick = onShowBookmarks) {
            Icon(Icons.Filled.Bookmarks, contentDescription = "Bookmarks", tint = Ink)
        }
        Spacer(modifier = Modifier.size(8.dp))
        ChromeIcon(onClick = onSearch) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = Ink)
        }
    }
}

@Composable
private fun ChromeIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Paper)
            .border(1.dp, Rule, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ReaderBottomBar(page: Int, pageCount: Int, onSeek: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Page $page", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Text("$page / $pageCount", color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = page.toFloat(),
            onValueChange = { onSeek(it.toInt().coerceIn(1, pageCount)) },
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
    onToggleChrome: () -> Unit,
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
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Paper.copy(alpha = 0.55f), Parchment, Parchment)
                )
            )
            .border(1.dp, Rule, RoundedCornerShape(6.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggleChrome() }
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Text(
            text = "— page $pageNumber · 16 lines —",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = WarmGrey,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
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
                val bg = if (highlightLine == index) Highlight else Color.Transparent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(bg)
                        .clickable {
                            if (line.type != "empty" && line.text.isNotBlank()) {
                                onLineTap(index)
                            } else {
                                onToggleChrome()
                            }
                        }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (line.type) {
                        "header" -> Text(
                            text = line.text,
                            fontFamily = AmiriQuran,
                            fontSize = 22.sp,
                            color = Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        "empty" -> Spacer(modifier = Modifier.fillMaxHeight())
                        else -> Text(
                            text = line.text,
                            fontFamily = AmiriQuran,
                            fontSize = 18.sp,
                            color = Ink,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
