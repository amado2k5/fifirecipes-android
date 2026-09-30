package cooking.fifi.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cooking.fifi.android.R
import cooking.fifi.android.data.S
import cooking.fifi.android.ui.LocalStrings
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

/** Full-screen error state with retry — every endpoint failure, like the TV ErrorScreen. */
@Composable
fun ErrorView(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Column(
        modifier.fillMaxSize().padding(32.dp).testTag("errorScreen"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painterResource(R.drawable.emblem), null, Modifier.size(88.dp),
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.6f) }),
        )
        Text(s[S.errorTitle], style = fifi(TS.Title2, W.Bold), color = Palette.ink, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(s[S.errorBody], style = fifi(TS.Body), color = Palette.inkDim, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.leafDeep, contentColor = Color.White),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 30.dp, vertical = 14.dp),
            modifier = Modifier.testTag("retryButton"),
        ) {
            Icon(Icons.Filled.Refresh, null)
            Spacer(Modifier.size(8.dp))
            Text(s[S.retry], style = fifi(TS.Headline, W.Bold))
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Palette.leafDeep)
        Text(s[S.loading], style = fifi(TS.Body), color = Palette.inkDim)
    }
}

/** "● Ingredients" section heading, exposed to TalkBack as a heading. */
@Composable
fun SectionTitle(text: String, dot: Color, modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.size(14.dp).background(dot, CircleShape))
        Text(text, style = fifi(TS.Title2, W.Bold), color = Palette.ink)
    }
}
