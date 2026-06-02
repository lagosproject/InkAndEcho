package com.LakesCorp.FunCoStory.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MechanicalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shadowColor: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowHeight = 3.dp
    
    val offsetVal by animateDpAsState(
        targetValue = if (isPressed && enabled) shadowHeight else 0.dp,
        label = "pressOffset"
    )

    val finalBgColor = if (enabled) backgroundColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val finalContentColor = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val finalShadowColor = if (enabled) shadowColor else Color.Transparent
    val finalBorderColor = if (enabled) shadowColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Shadow base layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(finalShadowColor, shape = RoundedCornerShape(4.dp))
        )
        // Foreground clickable button layer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = offsetVal)
                .background(finalBgColor, shape = RoundedCornerShape(4.dp))
                .border(1.dp, finalBorderColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides finalContentColor) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    content()
                }
            }
        }
    }
}
