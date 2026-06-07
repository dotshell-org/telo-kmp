package com.pelotcl.app.generic.ui.components.search.bar.lines

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pelotcl.app.generic.data.models.search.LineSearchResult
import com.pelotcl.app.generic.ui.theme.PrimaryColor
import com.pelotcl.app.generic.ui.theme.SecondaryColor
import com.pelotcl.app.generic.utils.graphics.LineIconResolver
import com.pelotcl.app.platform.DrawableProvider
import com.pelotcl.app.platform.LocalPlatformContext
import com.pelotcl.app.platform.StringProvider

@Composable
fun LineSearchResultItem(
    lineResult: LineSearchResult,
    onClick: () -> Unit
) {
    val drawableName = LineIconResolver.getDrawableNameForLineName(lineResult.lineName)
    val drawableProvider = DrawableProvider(LocalPlatformContext.current)
    val stringProvider = StringProvider(LocalPlatformContext.current)
    val hasDrawable = drawableProvider.hasDrawable(drawableName)
    val hasModeBus = drawableProvider.hasDrawable("mode_bus")

    ListItem(
        headlineContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasDrawable) {
                    Image(
                        painter = drawableProvider.getPainter(drawableName),
                        contentDescription = stringProvider["line_icon"].replace("%s", lineResult.lineName),
                        modifier = Modifier.size(40.dp)
                    )
                } else if (hasModeBus) {
                    Image(
                        painter = drawableProvider.getPainter("mode_bus"),
                        contentDescription = stringProvider["bus_mode_icon"],
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        "${lineResult.category} ${lineResult.lineName}",
                        color = SecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = PrimaryColor),
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
    )
}
