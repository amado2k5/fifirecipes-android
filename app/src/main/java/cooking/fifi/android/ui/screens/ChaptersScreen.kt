package cooking.fifi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cooking.fifi.android.data.Route
import cooking.fifi.android.ui.LocalApp
import cooking.fifi.android.ui.LocalWidthClass
import cooking.fifi.android.ui.WidthClass
import cooking.fifi.android.ui.components.ChapterCardView
import cooking.fifi.android.ui.components.RecipeCardView
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

@Composable
fun ChaptersScreen(pad: PaddingValues) {
    val app = LocalApp.current
    val wide = LocalWidthClass.current != WidthClass.Compact
    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (wide) 300.dp else 240.dp),
        modifier = Modifier.fillMaxSize().testTag("chaptersScreen"),
        contentPadding = pad.plus(horizontal = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(app.chapters, key = { it.id }) { ch ->
            ChapterCardView(ch, onClick = { app.push(Route.ChapterDetail(ch.id)) })
        }
    }
}

/** A chapter's recipes — up to 300+ cards, virtualized by LazyVerticalGrid. */
@Composable
fun ChapterDetailScreen(chapterId: Int, pad: PaddingValues) {
    val app = LocalApp.current
    val wide = LocalWidthClass.current != WidthClass.Compact
    val cards = remember(app.index, chapterId) { app.index.values.filter { it.chapter == chapterId }.sortedBy { it.id } }
    val title = app.chapters.firstOrNull { it.id == chapterId }?.name ?: cards.firstOrNull()?.chapterName.orEmpty()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (wide) 200.dp else 160.dp),
        modifier = Modifier.fillMaxSize().testTag("chapterDetail"),
        contentPadding = pad.plus(horizontal = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = fifi(TS.Title, W.Heavy), color = Palette.ink, modifier = Modifier.weight(1f).semantics { heading() })
                Text(
                    "${cards.size}", style = fifi(TS.Callout, W.Medium), color = Palette.leafDeep,
                    modifier = Modifier.background(Palette.leafSoft, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
        items(cards, key = { it.id }) { card ->
            RecipeCardView(card, onClick = { app.push(Route.Recipe(card.id)) })
        }
    }
}
