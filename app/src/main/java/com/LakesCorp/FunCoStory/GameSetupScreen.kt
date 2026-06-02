package com.LakesCorp.FunCoStory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.LakesCorp.FunCoStory.ui.components.MechanicalButton
import com.LakesCorp.FunCoStory.ui.components.PaperContainer
import com.LakesCorp.FunCoStory.ui.components.StoryStartersDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
    scribblersCount: Int,
    onScribblersChanged: (Int) -> Unit,
    roundsCount: Int,
    onRoundsChanged: (Int) -> Unit,
    hintLength: Int,
    onHintLengthChanged: (Int) -> Unit,
    storyPrompt: String,
    onStoryPromptChanged: (String) -> Unit,
    writerNames: List<String>,
    onWriterNamesChanged: (List<String>) -> Unit,
    soundManager: MediaPlaybackManager,
    onStartGame: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showPromptsDialog by remember { mutableStateOf(false) }
    var showNamesList by remember { mutableStateOf(false) }

    var promptValue by remember { mutableStateOf(TextFieldValue(storyPrompt)) }
    var lastText by remember { mutableStateOf(storyPrompt) }
    var lastCursorLine by remember { mutableStateOf(0) }

    LaunchedEffect(storyPrompt) {
        if (storyPrompt != promptValue.text) {
            promptValue = TextFieldValue(
                text = storyPrompt,
                selection = TextRange(storyPrompt.length)
            )
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        // Header
        Text(
            text = stringResource(id = R.string.setup_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.setup_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: Scribblers Count & Writer Names
        PaperContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.num_scribblers),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (scribblersCount > 2) onScribblersChanged(scribblersCount - 1) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = scribblersCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { if (scribblersCount < 10) onScribblersChanged(scribblersCount + 1) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNamesList = !showNamesList },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.writer_names_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.writer_names_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (showNamesList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (showNamesList) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    writerNames.forEachIndexed { index, name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(24.dp)
                            )
                            TextField(
                                value = name,
                                onValueChange = { newName ->
                                    val updated = writerNames.toMutableList()
                                    updated[index] = newName
                                    onWriterNamesChanged(updated)
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(id = R.string.writer_name_placeholder, index + 1),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                },
                                colors = TextFieldDefaults.textFieldColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                             )
                        }
                    }
                }
            }
        }

        // Card 1.5: Number of Rounds
        PaperContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.num_rounds),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = stringResource(id = R.string.num_rounds_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (roundsCount > 1) onRoundsChanged(roundsCount - 1) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = roundsCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { if (roundsCount < 5) onRoundsChanged(roundsCount + 1) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Card 2: Hint Length
        PaperContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.hint_length),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = stringResource(id = R.string.hint_length_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    1 to stringResource(id = R.string.hint_strict),
                    3 to stringResource(id = R.string.hint_standard),
                    5 to stringResource(id = R.string.hint_loose)
                )
                options.forEach { (length, label) ->
                    val isChecked = hintLength == length
                    val bg = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    val textCol = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(bg, RoundedCornerShape(4.dp))
                            .border(
                                width = 1.dp,
                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onHintLengthChanged(length) }
                    ) {
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = textCol,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Card 3: Story Prompt
        PaperContainer {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.story_prompt),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = { showPromptsDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.btn_suggest_prompt),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = promptValue,
                    onValueChange = { newValue ->
                        promptValue = newValue
                        onStoryPromptChanged(newValue.text)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxSize(),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    onTextLayout = { textLayoutResult ->
                        val layoutText = textLayoutResult.layoutInput.text.text
                        val currentCursor = promptValue.selection.start
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
                            if (promptValue.text.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.prompt_placeholder),
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Typewriter guide marks in corners
                val outlineColor = MaterialTheme.colorScheme.outline
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sizePx = 6.dp.toPx()
                    val stroke = 2.dp.toPx()
                    val col = outlineColor
                    
                    // Bottom-left corner
                    drawLine(col, Offset(0f, size.height - stroke/2), Offset(sizePx, size.height - stroke/2), stroke)
                    drawLine(col, Offset(stroke/2, size.height), Offset(stroke/2, size.height - sizePx), stroke)
                    
                    // Bottom-right corner
                    drawLine(col, Offset(size.width, size.height - stroke/2), Offset(size.width - sizePx, size.height - stroke/2), stroke)
                    drawLine(col, Offset(size.width - stroke/2, size.height), Offset(size.width - stroke/2, size.height - sizePx), stroke)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Start button
        MechanicalButton(
            onClick = {
                focusManager.clearFocus()
                onStartGame()
            },
            backgroundColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shadowColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.btn_start_writing),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 0.05.sp
            )
        }
    }

    if (showPromptsDialog) {
        StoryStartersDialog(
            onPromptSelected = onStoryPromptChanged,
            onDismiss = { showPromptsDialog = false }
        )
    }
}
