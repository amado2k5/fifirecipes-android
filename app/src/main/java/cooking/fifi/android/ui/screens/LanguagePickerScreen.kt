package cooking.fifi.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cooking.fifi.android.R
import cooking.fifi.android.data.LanguageInfo
import cooking.fifi.android.data.S
import cooking.fifi.android.ui.LocalApp
import cooking.fifi.android.ui.LocalStrings
import cooking.fifi.android.ui.components.focusRing
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import cooking.fifi.android.ui.theme.LocalFifiLang

/**
 * Language picker — first run and from Settings. Each tile renders its native
 * name in that language's own font and direction (Urdu in Nastaliq, etc.).
 */
@Composable
fun LanguagePickerScreen(onPicked: (String) -> Unit, modifier: Modifier = Modifier, pad: PaddingValues = PaddingValues()) {
    val app = LocalApp.current
    val s = LocalStrings.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(170.dp),
        modifier = modifier.fillMaxSize().testTag("languagePicker"),
        contentPadding = pad.plus(horizontal = 16.dp, top = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(painterResource(R.drawable.emblem), null, Modifier.size(84.dp))
                Text(s[S.appName], style = fifi(TS.LargeTitle, W.Heavy), color = Palette.leafDeep)
                Text(s[S.chooseLanguage], style = fifi(TS.Title3), color = Palette.inkDim, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            }
        }
        items(app.manifest?.languages.orEmpty(), key = { it.code }) { lang ->
            LanguageTile(lang, selected = lang.code == app.lang && app.phase == cooking.fifi.android.Phase.Ready) { onPicked(lang.code) }
        }
    }
}

@Composable
private fun LanguageTile(lang: LanguageInfo, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) Palette.leafSoft else Palette.card)
            .border(if (selected) 3.dp else 1.5.dp, if (selected) Palette.leaf else Palette.cardBorder, shape)
            .focusRing(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("lang-${lang.code}"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompositionLocalProvider(
                LocalFifiLang provides lang.code,
                LocalLayoutDirection provides if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Text(lang.nativeName, style = fifi(TS.Headline, W.Bold), color = Palette.ink, textAlign = TextAlign.Center)
            }
            if (lang.englishName != lang.nativeName) {
                Text(lang.englishName, style = fifi(TS.Footnote), color = Palette.inkDim, textAlign = TextAlign.Center)
            }
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, null, tint = Palette.leafDeep, modifier = Modifier.align(Alignment.TopEnd).size(20.dp))
        }
    }
}
