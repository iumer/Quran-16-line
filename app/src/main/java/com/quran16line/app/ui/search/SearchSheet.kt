package com.quran16line.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quran16line.app.data.SurahInfo
import com.quran16line.app.ui.theme.Chrome
import com.quran16line.app.ui.theme.Ink
import com.quran16line.app.ui.theme.Muted
import com.quran16line.app.ui.theme.Paper
import com.quran16line.app.ui.theme.Parchment
import com.quran16line.app.ui.theme.Rule

private enum class SearchMode { Page, Surah, Ayat }

private val SoftBlack = Color(0xFF2A2218)
private val OnSoftBlack = Color(0xFFFFF8E8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    surahs: List<SurahInfo>,
    pageCount: Int,
    onDismiss: () -> Unit,
    onJumpPage: (Int) -> Unit,
    onJumpSurah: (Int) -> Unit,
    onJumpAyah: (Int, Int) -> Unit,
    totalVerses: (Int) -> Int,
    previewPageLabel: (Int) -> String = { "Page $it" },
    resolvePageQuery: (raw: Int, preferPrinted: Boolean) -> Int? = { raw, _ ->
        raw.takeIf { it in 1..pageCount }
    },
    pageForAyahPreview: (Int, Int) -> Int? = { _, _ -> null }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(SearchMode.Page) }
    var pageText by remember { mutableStateOf("") }
    var usePrintedPage by remember { mutableStateOf(false) }
    var selectedSurah by remember { mutableIntStateOf(surahs.firstOrNull()?.id ?: 1) }
    var surahQuery by remember { mutableStateOf("") }
    var ayahText by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }

    val typedPage = pageText.toIntOrNull()
    val resolvedPage = typedPage?.let { resolvePageQuery(it, usePrintedPage) }
    val typedAyah = ayahText.toIntOrNull()
    val ayahTargetPage = typedAyah?.let { pageForAyahPreview(selectedSurah, it) }
    val selectedSurahInfo = surahs.firstOrNull { it.id == selectedSurah }

    fun go() {
        error = null
        when (mode) {
            SearchMode.Page -> {
                val raw = pageText.toIntOrNull()
                if (raw == null) {
                    error = "Enter a page number"
                    return
                }
                val page = resolvePageQuery(raw, usePrintedPage)
                if (page == null) {
                    error = if (usePrintedPage) {
                        "Printed page must be between 1 and 549"
                    } else {
                        "Enter an app page between 1 and $pageCount"
                    }
                } else {
                    onJumpPage(page)
                }
            }
            SearchMode.Surah -> {
                if (selectedSurahInfo == null) {
                    error = "Select a surah"
                } else {
                    onJumpSurah(selectedSurah)
                }
            }
            SearchMode.Ayat -> {
                val ayah = ayahText.toIntOrNull()
                val max = totalVerses(selectedSurah)
                when {
                    selectedSurahInfo == null -> error = "Select a surah"
                    ayah == null -> error = "Enter an ayat number"
                    ayah !in 1..max -> error = "Enter an ayat between 1 and $max"
                    pageForAyahPreview(selectedSurah, ayah) == null -> error = "Ayat not found in index"
                    else -> onJumpAyah(selectedSurah, ayah)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Paper,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Paper)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(text = "Search", color = Ink, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Jump by page, surah, or ayat.",
                color = Muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            ModeSegmentedBar(
                selected = mode,
                onSelect = { mode = it; error = null }
            )
            Spacer(modifier = Modifier.height(12.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ink,
                unfocusedBorderColor = Rule,
                focusedContainerColor = Paper,
                unfocusedContainerColor = Paper,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                cursorColor = Ink,
                focusedLabelColor = Muted,
                unfocusedLabelColor = Muted
            )

            when (mode) {
                SearchMode.Page -> {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it.filter { ch -> ch.isDigit() }; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                if (usePrintedPage) "Printed mushaf page (1–549)"
                                else "App page number (1–$pageCount)"
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { go() }),
                        singleLine = true,
                        colors = fieldColors
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { usePrintedPage = !usePrintedPage }
                    ) {
                        Checkbox(
                            checked = usePrintedPage,
                            onCheckedChange = { usePrintedPage = it; error = null }
                        )
                        Text(
                            text = "I entered the printed page number from the book",
                            color = Muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (resolvedPage != null) {
                        PreviewCard(
                            title = "Opens app page $resolvedPage",
                            subtitle = previewPageLabel(resolvedPage)
                        )
                    }
                }
                SearchMode.Surah -> {
                    SurahSearchPicker(
                        surahs = surahs,
                        selected = selectedSurah,
                        query = surahQuery,
                        onQueryChange = { surahQuery = it; error = null },
                        onSelect = { selectedSurah = it; error = null },
                        fieldColors = fieldColors
                    )
                    selectedSurahInfo?.let {
                        PreviewCard(
                            title = "Opens app page ${it.page}",
                            subtitle = previewPageLabel(it.page)
                        )
                    }
                }
                SearchMode.Ayat -> {
                    SurahSearchPicker(
                        surahs = surahs,
                        selected = selectedSurah,
                        query = surahQuery,
                        onQueryChange = { surahQuery = it; error = null },
                        onSelect = { selectedSurah = it; error = null },
                        fieldColors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ayahText,
                        onValueChange = { ayahText = it.filter { ch -> ch.isDigit() }; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ayat (1–${totalVerses(selectedSurah)})") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { go() }),
                        singleLine = true,
                        colors = fieldColors
                    )
                    if (ayahTargetPage != null && typedAyah != null) {
                        PreviewCard(
                            title = "Opens app page $ayahTargetPage",
                            subtitle = "${selectedSurahInfo?.transliteration ?: "Surah"} · Ayah $typedAyah · ${previewPageLabel(ayahTargetPage)}"
                        )
                    }
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color(0xFF8B3A2A))
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { go() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftBlack,
                    contentColor = OnSoftBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Go to reading",
                    color = OnSoftBlack,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PreviewCard(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Chrome, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ModeSegmentedBar(
    selected: SearchMode,
    onSelect: (SearchMode) -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .border(1.dp, Rule, shape)
            .background(Chrome)
    ) {
        SearchMode.entries.forEach { item ->
            val isSelected = selected == item
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) SoftBlack else Color.Transparent)
                    .clickable { onSelect(item) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name,
                    color = if (isSelected) OnSoftBlack else Ink,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SurahSearchPicker(
    surahs: List<SurahInfo>,
    selected: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (Int) -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors
) {
    val current = surahs.firstOrNull { it.id == selected }
    val filtered = remember(query, surahs) { rankSurahMatches(surahs, query) }

    Text(
        text = current?.let { "Selected: ${it.id} — ${it.transliteration} — ${it.name} · p.${it.page}" } ?: "",
        color = Muted,
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Search surah by number or name") },
        singleLine = true,
        colors = fieldColors
    )
    Spacer(modifier = Modifier.height(6.dp))
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .background(Parchment, RoundedCornerShape(10.dp))
            .padding(vertical = 4.dp)
    ) {
        items(filtered, key = { it.id }) { surah ->
            val isSelected = surah.id == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) Rule.copy(alpha = 0.45f) else Color.Transparent)
                    .clickable { onSelect(surah.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${surah.id} — ${surah.transliteration} (${surah.name})",
                    color = Ink,
                    modifier = Modifier.weight(1f)
                )
                Text("p.${surah.page}", color = Muted)
            }
            HorizontalDivider(color = Rule)
        }
    }
}

internal fun rankSurahMatches(surahs: List<SurahInfo>, query: String): List<SurahInfo> {
    val q = query.trim()
    if (q.isEmpty()) return surahs
    return surahs
        .map { it to scoreSurah(it, q) }
        .filter { it.second > 0 }
        .sortedWith(compareByDescending<Pair<SurahInfo, Int>> { it.second }.thenBy { it.first.id })
        .map { it.first }
}

internal fun scoreSurah(surah: SurahInfo, query: String): Int {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return 0
    if (surah.id.toString() == q) return 1000

    val names = buildList {
        add(surah.transliteration)
        add(surah.name)
        surah.aliases.orEmpty().forEach { add(it) }
        add(surah.transliteration.replace("-", " "))
        add(surah.transliteration.replace("'", ""))
        add(surah.transliteration.substringAfterLast('-'))
        add(surah.transliteration.substringAfterLast(' '))
    }.map { it.lowercase() }.distinct()

    if (names.any { it == q }) return 950
    if (names.any { it == "an-$q" || it == "al-$q" || it == "ash-$q" || it == "ad-$q" || it == "at-$q" }) return 920
    if (names.any { it.endsWith("-$q") || it.endsWith(" $q") || (it.endsWith(q) && it.length <= q.length + 4) }) return 880
    if (names.any { Regex("""(^|[\s\-'])${Regex.escape(q)}($|[\s\-'])""").containsMatchIn(it) }) return 750
    if (names.any { it.contains(q) }) return 400
    return 0
}
