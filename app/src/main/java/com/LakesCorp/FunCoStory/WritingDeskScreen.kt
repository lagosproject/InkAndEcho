package com.LakesCorp.FunCoStory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingDeskScreen(
    currentTurn: Int,
    totalTurns: Int,
    writerName: String,
    echoText: String,
    hintLength: Int,
    soundManager: MediaPlaybackManager,
    onSealScroll: (String) -> Unit
) {
    var threadTextValue by remember(currentTurn) { mutableStateOf(TextFieldValue("")) }
    var lastText by remember(currentTurn) { mutableStateOf("") }
    var lastCursorLine by remember(currentTurn) { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current

    val words = remember(threadTextValue.text) {
        threadTextValue.text.trim().split(whitespaceRegex).filter { it.isNotBlank() }
    }
    val minWords = (hintLength + 1) / 2
    val isReady = words.size >= minWords

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Turn indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(id = R.string.turn_indicator, currentTurn, totalTurns),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.05.sp
            )
        }

        // The Echo card (what the last player wrote)
        if (echoText.isNotBlank()) {
            PaperContainer {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.the_echo),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = echoText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 26.sp
                )
            }
        }

        // The input area
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${stringResource(id = R.string.your_thread)} ($writerName)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = threadTextValue,
                    onValueChange = { newValue ->
                        threadTextValue = newValue
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxSize(),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    onTextLayout = { textLayoutResult ->
                        val layoutText = textLayoutResult.layoutInput.text.text
                        val currentCursor = threadTextValue.selection.start
                        if (currentCursor <= layoutText.length) {
                            val currentLine = textLayoutResult.getLineForOffset(currentCursor)
                            if (layoutText.length > lastText.length) {
                                if (currentLine > lastCursorLine) {
                                    soundManager.playReturnSound()
                                } else {
                                    soundManager.playKeySound()
                                }
                            }
                            lastText = layoutText
                            lastCursorLine = currentLine
                        }
                    },
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (threadTextValue.text.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.thread_placeholder),
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // Word count & validation warning layout
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${words.size} ${if (words.size == 1) "word" else "words"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (words.size < minWords && threadTextValue.text.isNotBlank()) {
                    Text(
                        text = stringResource(id = R.string.min_words_warning, minWords),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Seal the scroll button
        MechanicalButton(
            onClick = {
                if (isReady) {
                    focusManager.clearFocus()
                    onSealScroll(threadTextValue.text)
                    threadTextValue = TextFieldValue("")
                }
            },
            enabled = isReady,
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = if (currentTurn >= totalTurns) {
                    stringResource(id = R.string.btn_seal_end)
                } else {
                    stringResource(id = R.string.btn_seal_scroll)
                },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
