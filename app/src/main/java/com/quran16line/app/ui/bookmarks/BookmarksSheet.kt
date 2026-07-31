package com.quran16line.app.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quran16line.app.data.Bookmark
import com.quran16line.app.ui.theme.Ink
import com.quran16line.app.ui.theme.Muted
import com.quran16line.app.ui.theme.Paper
import com.quran16line.app.ui.theme.Rule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksSheet(
    bookmarks: List<Bookmark>,
    onDismiss: () -> Unit,
    onOpen: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Paper,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("Bookmarks", color = Ink, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Text(
                "Saved pages for quick return.",
                color = Muted,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (bookmarks.isEmpty()) {
                Text("No bookmarks yet. Tap the star while reading.", color = Muted)
                Spacer(modifier = Modifier.height(28.dp))
            } else {
                LazyColumn {
                    items(bookmarks, key = { it.page to it.createdAt }) { bookmark ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(bookmark.page) }
                                .padding(vertical = 12.dp)
                        ) {
                            Text("Page ${bookmark.page}", color = Ink)
                            Text(bookmark.label, color = Muted)
                        }
                        HorizontalDivider(color = Rule)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
