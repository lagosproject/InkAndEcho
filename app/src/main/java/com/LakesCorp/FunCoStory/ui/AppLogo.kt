package com.LakesCorp.FunCoStory.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun AppLogo(
    modifier: Modifier = Modifier
) {
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val scale = size.width / 384f
        
        // 1. Distant past
        drawCircle(
            color = outlineVariant,
            radius = 32f * scale,
            center = Offset(84f * scale, 192f * scale)
        )
        // 2. Recent past
        drawCircle(
            color = outline,
            radius = 32f * scale,
            center = Offset(172f * scale, 192f * scale)
        )
        // 3. The echo (present)
        drawCircle(
            color = primary,
            radius = 32f * scale,
            center = Offset(260f * scale, 192f * scale)
        )
        // 4. Cursor active
        val cursorLeft = 316f * scale
        val cursorTop = 152f * scale
        val cursorWidth = 16f * scale
        val cursorHeight = 80f * scale
        val cursorRx = 8f * scale
        
        drawRoundRect(
            color = secondary,
            topLeft = Offset(cursorLeft, cursorTop),
            size = Size(cursorWidth, cursorHeight),
            cornerRadius = CornerRadius(cursorRx, cursorRx)
        )
    }
}

@Composable
fun AppLogoIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(percent = 22),
                clip = true
            )
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AppLogo(modifier = Modifier.fillMaxSize())
    }
}
