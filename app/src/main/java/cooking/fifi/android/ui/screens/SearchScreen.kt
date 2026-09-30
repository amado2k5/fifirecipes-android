package cooking.fifi.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cooking.fifi.android.data.RecipeSearch
import cooking.fifi.android.data.Route
import cooking.fifi.android.data.S
import cooking.fifi.android.ui.LocalApp
import cooking.fifi.android.ui.LocalStrings
import cooking.fifi.android.ui.LocalWidthClass
import cooking.fifi.android.ui.WidthClass
import cooking.fifi.android.ui.components.RecipeCardView
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

/**
 * On-device search with the system keyboard (the TV on-screen keyboard is a
 * TV-only pattern). Same rule as the TV app: every term must match.
 */
@Composable
fun SearchScreen(pad: PaddingValues) {
    val app = LocalApp.current
    val s = LocalStrings.current
    val wide = LocalWidthClass.current != WidthClass.Compact
    var query by rememberSaveable { mutableStateOfString() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val haystack by produceState<Map<String, String>?>(null, app.lang) {
        value = runCatching { app.api.search(app.lang) }.getOrDefault(emptyMap())
    }
    val limit = if (wide) 96 else 48
    val results = remember(query, haystack, app.index) {
        haystack?.let { RecipeSearch.match(it, query, app.index.keys, limit) }.orEmpty()
    }
    LaunchedEffect(Unit) { if (query.isEmpty()) runCatching { focus.requestFocus() } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (wide) 200.dp else 160.dp),
        modifier = Modifier.fillMaxSize().testTag("searchScreen"),
        contentPadding = pad.plus(horizontal = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "field") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.contentWidth(760.dp).focusRequester(focus).testTag("searchField"),
                    singleLine = true,
                    textStyle = fifi(TS.Body, W.Medium),
                    placeholder = { Text(s[S.searchTitle], style = fifi(TS.Body)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, s[S.clearSearch]) }
                    },
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Palette.card, unfocusedContainerColor = Palette.card,
                        focusedBorderColor = Palette.leafDeep, unfocusedBorderColor = Palette.cardBorder,
                    ),
                )
                val q = query.trim()
                val status = when {
                    q.isEmpty() -> s[S.searchHint]
                    haystack == null -> s[S.loading]
                    results.isEmpty() -> s[S.noResults]
                    else -> "${s[S.resultsFor]} “$q” · ${results.size}"
                }
                Text(
                    status,
                    style = fifi(if (q.isNotEmpty() && results.isNotEmpty()) TS.Headline else TS.Body, if (q.isNotEmpty() && results.isNotEmpty()) W.Bold else W.Medium),
                    color = if (q.isNotEmpty() && results.isNotEmpty()) Palette.ink else Palette.inkDim,
                    // Announce result counts to TalkBack as they change.
                    modifier = Modifier.contentWidth(760.dp).padding(horizontal = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }.testTag("searchStatus"),
                )
            }
        }
        items(results, key = { it }) { id ->
            app.index[id]?.let { card -> RecipeCardView(card, onClick = { keyboard?.hide(); app.push(Route.Recipe(id)) }) }
        }
    }
}

private fun mutableStateOfString() = androidx.compose.runtime.mutableStateOf("")
