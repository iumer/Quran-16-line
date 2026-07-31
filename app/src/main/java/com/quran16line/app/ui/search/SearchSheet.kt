package com.quran16line.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quran16line.app.data.SurahInfo
import com.quran16line.app.ui.theme.Chrome
import com.quran16line.app.ui.theme.Ink
import com.quran16line.app.ui.theme.Muted
import com.quran16line.app.ui.theme.Paper
import com.quran16line.app.ui.theme.Rule

private enum class SearchMode { Page, Surah, Ayat }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    surahs: List<SurahInfo>,
    pageCount: Int,
    onDismiss: () -> Unit,
    onJumpPage: (Int) -> Unit,
    onJumpSurah: (Int) -> Unit,
    onJumpAyah: (Int, Int) -> Unit,
    totalVerses: (Int) -> Int
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(SearchMode.Page) }
    var pageText by remember { mutableStateOf("") }
    var selectedSurah by remember { mutableIntStateOf(surahs.firstOrNull()?.id ?: 1) }
    var ayahText by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }
    var showSurahList by remember { mutableStateOf(false) }

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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(text = "Search", color = Ink, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Text(
                text = "Jump by page, surah, or ayat.",
                color = Muted,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { mode = item; error = null; showSurahList = false },
                        label = { Text(item.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Ink,
                            selectedLabelColor = Paper,
                            containerColor = Chrome,
                            labelColor = Ink
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ink,
                unfocusedBorderColor = Rule,
                focusedContainerColor = Paper,
                unfocusedContainerColor = Paper
            )

            when (mode) {
                SearchMode.Page -> {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Page number (1–$pageCount)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors
                    )
                }
                SearchMode.Surah -> {
                    SurahPicker(surahs, selectedSurah, showSurahList, { showSurahList = it }) { selectedSurah = it }
                }
                SearchMode.Ayat -> {
                    SurahPicker(surahs, selectedSurah, showSurahList, { showSurahList = it }) { selectedSurah = it }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ayahText,
                        onValueChange = { ayahText = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ayat (1–${totalVerses(selectedSurah)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors
                    )
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = androidx.compose.ui.graphics.Color(0xFF8B3A2A))
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    when (mode) {
                        SearchMode.Page -> {
                            val page = pageText.toIntOrNull()
                            if (page == null || page !in 1..pageCount) {
                                error = "Enter a page between 1 and $pageCount"
                            } else {
                                onJumpPage(page)
                            }
                        }
                        SearchMode.Surah -> onJumpSurah(selectedSurah)
                        SearchMode.Ayat -> {
                            val ayah = ayahText.toIntOrNull()
                            val max = totalVerses(selectedSurah)
                            if (ayah == null || ayah !in 1..max) {
                                error = "Enter an ayat between 1 and $max"
                            } else {
                                onJumpAyah(selectedSurah, ayah)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go to reading")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SurahPicker(
    surahs: List<SurahInfo>,
    selected: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit
) {
    val current = surahs.firstOrNull { it.id == selected }
    OutlinedTextField(
        value = current?.let { "${it.id} — ${it.transliteration}" } ?: "",
        onValueChange = {},
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) },
        enabled = false,
        label = { Text("Surah") },
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = Rule,
            disabledTextColor = Ink,
            disabledLabelColor = Muted
        )
    )
    if (expanded) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .background(Chrome, RoundedCornerShape(10.dp))
                .padding(vertical = 4.dp)
        ) {
            items(surahs, key = { it.id }) { surah ->
                Text(
                    text = "${surah.id} — ${surah.transliteration} (${surah.name})",
                    color = Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(surah.id)
                            onExpandedChange(false)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
                HorizontalDivider(color = Rule)
            }
        }
    }
}
