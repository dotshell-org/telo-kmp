package com.pelotcl.app.generic.ui.components.search.bar.stops

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pelotcl.app.generic.utils.graphics.LineIconResolver
import com.pelotcl.app.platform.DrawableProvider
import com.pelotcl.app.platform.LocalPlatformContext
import com.pelotcl.app.platform.StringProvider

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
