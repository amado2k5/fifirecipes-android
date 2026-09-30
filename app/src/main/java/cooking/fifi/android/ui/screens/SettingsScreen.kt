package cooking.fifi.android.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cooking.fifi.android.BuildConfig
import cooking.fifi.android.data.S
import cooking.fifi.android.ui.LocalApp
import cooking.fifi.android.ui.LocalStrings
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(pad: PaddingValues) {
    val app = LocalApp.current
    val s = LocalStrings.current
    val context = LocalContext.current
    var showPicker by rememberSaveable { mutableStateOf(false) }
    fun open(url: String) = try { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } catch (_: ActivityNotFoundException) {}

    LazyColumn(
        Modifier.fillMaxSize().testTag("settingsScreen"),
        contentPadding = pad.plus(horizontal = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SettingsRow(Icons.Filled.Language, onClick = { showPicker = true }, modifier = Modifier.contentWidth(760.dp).testTag("settingsLanguageRow")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s[S.language], style = fifi(TS.Headline, W.Bold), color = Palette.ink, modifier = Modifier.weight(1f))
                    Text(app.langInfo?.let { if (it.nativeName == it.englishName) it.nativeName else "${it.nativeName} (${it.englishName})" } ?: app.lang, style = fifi(TS.Subheadline, W.Medium), color = Palette.leafDeep)
                }
            }
        }
        item {
            SettingsRow(Icons.Filled.Info, modifier = Modifier.contentWidth(760.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s[S.about], style = fifi(TS.Headline, W.Bold), color = Palette.ink)
                    Text(s[S.aboutText], style = fifi(TS.Body), color = Palette.inkDim)
                    Text(
                        "fifi.cooking", style = fifi(TS.Subheadline, W.Bold), color = Palette.leafDeep,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { open("https://fifi.cooking") }.heightIn(min = 48.dp).padding(vertical = 12.dp),
                    )
                }
            }
        }
        item {
            SettingsRow(Icons.Filled.Lock, onClick = { open("https://android.fifi.cooking/privacy.html") }, modifier = Modifier.contentWidth(760.dp)) {
                Text("${s[S.privacy]} · android.fifi.cooking", style = fifi(TS.Subheadline, W.Bold), color = Palette.leafDeep)
            }
        }
        item {
            SettingsRow(Icons.Filled.Inventory2, modifier = Modifier.contentWidth(760.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s[S.version], style = fifi(TS.Headline, W.Bold), color = Palette.ink, modifier = Modifier.weight(1f))
                        Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = fifi(TS.Subheadline), color = Palette.inkDim)
                    }
                    Text("fifi.cooking · ${app.manifest?.version ?: "—"}", style = fifi(TS.Footnote), color = Palette.inkDim)
                    Text("Fonts: SIL Open Font License 1.1", style = fifi(TS.Caption), color = Palette.inkDim)
                }
            }
        }
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Palette.paper,
        ) {
            LanguagePickerScreen(onPicked = { showPicker = false; app.setLanguage(it) })
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .clip(shape)
            .background(Palette.card)
            .border(1.5.dp, Palette.cardBorder, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 72.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(44.dp).background(Palette.leafSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Palette.leafDeep)
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp)) { content() }
    }
}
