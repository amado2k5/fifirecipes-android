package cooking.fifi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cooking.fifi.android.data.ApiClient
import cooking.fifi.android.data.FeedRow
import cooking.fifi.android.data.Route
import cooking.fifi.android.data.S
import cooking.fifi.android.ui.LocalApp
import cooking.fifi.android.ui.LocalCompactHeight
import cooking.fifi.android.ui.LocalStrings
import cooking.fifi.android.ui.LocalWidthClass
import cooking.fifi.android.ui.WidthClass
import cooking.fifi.android.ui.components.ChipTone
import cooking.fifi.android.ui.components.InteractiveCard
import cooking.fifi.android.ui.components.KidsCardView
import cooking.fifi.android.ui.components.MetaChip
import cooking.fifi.android.ui.components.RecipeCardView
import cooking.fifi.android.ui.components.RemoteImage
import cooking.fifi.android.ui.theme.LocalKidsFont
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

/** Home: hero from the "featured" row, then one horizontal rail per feed row. */
@Composable
fun HomeScreen(pad: PaddingValues) {
    val app = LocalApp.current
    val wide = LocalWidthClass.current != WidthClass.Compact
    LazyColumn(
        Modifier.fillMaxSize().testTag("homeScreen"),
        contentPadding = pad.plus(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        item(key = "hero") { Hero(wide) }
        items(app.feed.rows, key = { it.key }) { row -> Rail(row, wide) }
    }
}

@Composable
private fun Hero(wide: Boolean) {
    val app = LocalApp.current
    val s = LocalStrings.current
    val id = app.feed.rows.firstOrNull { it.key == "featured" }?.items?.firstOrNull() ?: return
    val card = app.index[id] ?: return
    val info = app.images[id]
    InteractiveCard(
        label = card.title,
        onClick = { app.push(Route.Recipe(id)) },
        shape = RoundedCornerShape(24.dp),
        share = card.title to ApiClient.shareUrl(id),
        modifier = Modifier.contentWidth().padding(horizontal = 16.dp),
        testTag = "hero",
    ) {
        Box(Modifier.fillMaxWidth().height(if (LocalCompactHeight.current) 220.dp else if (wide) 380.dp else 300.dp)) {
            RemoteImage(app.assetUrl(info?.full2x ?: info?.full ?: card.image), Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.25f to Color.Transparent, 1f to Palette.ink.copy(alpha = 0.8f))))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s[S.tagline], style = fifi(TS.Footnote, W.Medium), color = Color.White.copy(alpha = 0.92f))
                Text(card.title, style = fifi(TS.Title, W.Heavy), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.prepTime?.let { MetaChip(it, tone = ChipTone.Leaf, label = s[S.prep]) }
                    card.cookTime?.let { MetaChip(it, tone = ChipTone.Tomato, label = s[S.cook]) }
                }
            }
        }
    }
}

@Composable
private fun Rail(row: FeedRow, wide: Boolean) {
    val app = LocalApp.current
    if (row.items.isEmpty()) return
    val cardWidth = if (wide) 240.dp else 200.dp
    Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            row.title, style = fifi(TS.Title2, W.Bold), color = Palette.ink,
            modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.testTag("rail-${row.key}"),
        ) {
            items(row.items, key = { it }) { id ->
                if (row.key == "kids") {
                    app.kids[id]?.let { card ->
                        androidx.compose.runtime.CompositionLocalProvider(LocalKidsFont provides true) {
                            KidsCardView(card, onClick = { app.push(Route.KidsReady(id)) }, modifier = Modifier.width(cardWidth))
                        }
                    }
                } else {
                    app.index[id]?.let { card ->
                        RecipeCardView(card, onClick = { app.push(Route.Recipe(id)) }, modifier = Modifier.width(cardWidth))
                    }
                }
            }
        }
    }
}
