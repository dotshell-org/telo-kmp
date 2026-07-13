package eu.dotshell.telo.generic.ui.components.search.bar.stops

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.dotshell.telo.generic.utils.graphics.LineIconResolver
import eu.dotshell.telo.platform.DrawableProvider
import eu.dotshell.telo.platform.LocalPlatformContext
import eu.dotshell.telo.platform.StringProvider

@Composable
fun SearchConnectionBadge(lineName: String, sizeDp: Int = 30) {
    val drawableName = LineIconResolver.getDrawableNameForLineName(lineName)
    val drawableProvider = DrawableProvider(LocalPlatformContext.current)
    val stringProvider = StringProvider(LocalPlatformContext.current)

    if (drawableName.isNotBlank() && drawableProvider.hasDrawable(drawableName)) {
        Image(
            painter = drawableProvider.getPainter(drawableName),
            contentDescription = stringProvider["line_icon"].replace("%s", lineName),
            modifier = Modifier.size(sizeDp.dp)
        )
    }
}
