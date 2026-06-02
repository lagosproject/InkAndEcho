package com.LakesCorp.FunCoStory

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.LakesCorp.FunCoStory.ui.AppLogoIcon

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
