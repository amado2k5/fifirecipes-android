package cooking.fifi.android.ui.kids

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cooking.fifi.android.ui.theme.KidsPalette
import cooking.fifi.android.ui.theme.LocalKidsFont
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

/** Kids screens: polka-dot canvas + Baloo faces for the whole subtree. */
@Composable
fun KidsScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKidsFont provides true) {
        Box(Modifier.fillMaxSize()) {
            KidsCanvas()
            content()
        }
    }
}

@Composable
fun KidsCanvas() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(KidsPalette.canvas)
        val step = 46.dp.toPx()
        val r = 2.6.dp.toPx()
        var y = 0f
        while (y < size.height + step) {
            var x = 0f
            while (x < size.width + step) {
                drawCircle(KidsPalette.dot, r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

@Composable
fun RainbowBar(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(KidsPalette.rainbow))
}

/** Chunky rounded kids button (Let's cook / Next / Finish). */
@Composable
fun KidsButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = KidsPalette.go,
    border: Color = KidsPalette.checkBorder,
    foreground: Color = Color.White,
    tag: String? = null,
) {
    Box(
        modifier
            .shadow(6.dp, CircleShape, ambientColor = KidsPalette.ink, spotColor = KidsPalette.ink)
            .clip(CircleShape)
            .background(brush)
            .border(3.dp, border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 30.dp, vertical = 14.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = fifi(TS.Title3, W.Heavy), color = foreground)
    }
}
