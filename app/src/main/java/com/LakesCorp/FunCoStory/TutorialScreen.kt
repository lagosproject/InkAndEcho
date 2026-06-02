package com.LakesCorp.FunCoStory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.LakesCorp.FunCoStory.ui.AppLogoIcon
import com.LakesCorp.FunCoStory.ui.components.MechanicalButton
import com.LakesCorp.FunCoStory.ui.components.PaperContainer

@Composable
fun TutorialScreen(
    onStartSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        AppLogoIcon(
            modifier = Modifier
                .size(120.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.guide_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.guide_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(vertical = 4.dp, horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Step 1
        TutorialCard(
            stepNumber = "01",
            icon = Icons.Default.Edit,
            iconColor = MaterialTheme.colorScheme.secondary,
            title = stringResource(id = R.string.step1_title),
            description = stringResource(id = R.string.step1_desc)
        )

        // Dotted divider
        DottedConnectorLine()

        // Step 2
        TutorialCard(
            stepNumber = "02",
            icon = Icons.Default.Send,
            iconColor = MaterialTheme.colorScheme.primary,
            title = stringResource(id = R.string.step2_title),
            description = stringResource(id = R.string.step2_desc)
        )

        // Dotted divider
        DottedConnectorLine()

        // Step 3
        TutorialCard(
            stepNumber = "03",
            icon = Icons.Default.AutoStories,
            iconColor = MaterialTheme.colorScheme.secondary,
            title = stringResource(id = R.string.step3_title),
            description = stringResource(id = R.string.step3_desc)
        )

        Spacer(modifier = Modifier.height(12.dp))

        MechanicalButton(
            onClick = onStartSession,
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = stringResource(id = R.string.btn_start_session),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

// Single step card inside the TutorialScreen
@Composable
fun TutorialCard(
    stepNumber: String,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    PaperContainer(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$stepNumber.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(999.dp)
                        )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// Dotted connector line between tutorial cards
@Composable
fun DottedConnectorLine() {
    val outlineCol = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        drawLine(
            color = outlineCol.copy(alpha = 0.3f),
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
            strokeWidth = 2.dp.toPx()
        )
    }
}
