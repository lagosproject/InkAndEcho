package com.LakesCorp.FunCoStory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.LakesCorp.FunCoStory.R

data class StoryStarter(
    val text: String,
    val genre: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryStartersDialog(
    onPromptSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mysteryStarters = remember { context.resources.getStringArray(R.array.story_starters_mystery) }
    val fantasyStarters = remember { context.resources.getStringArray(R.array.story_starters_fantasy) }
    val scifiStarters = remember { context.resources.getStringArray(R.array.story_starters_scifi) }
    val dramaStarters = remember { context.resources.getStringArray(R.array.story_starters_drama) }

    val storyStartersList = remember(mysteryStarters, fantasyStarters, scifiStarters, dramaStarters) {
        mysteryStarters.map { StoryStarter(it, "Mystery") } +
        fantasyStarters.map { StoryStarter(it, "Fantasy") } +
        scifiStarters.map { StoryStarter(it, "Sci-Fi") } +
        dramaStarters.map { StoryStarter(it, "Drama") }
    }

    var selectedGenreFilter by remember { mutableStateOf("All") }
    val genres = listOf("All", "Mystery", "Fantasy", "Sci-Fi", "Drama")

    val filteredStarters = remember(selectedGenreFilter, storyStartersList) {
        if (selectedGenreFilter == "All") {
            storyStartersList
        } else {
            storyStartersList.filter { it.genre.equals(selectedGenreFilter, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_prompts_title),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Genre Filters (Scrollable Row of Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    genres.forEach { genre ->
                        val isSelected = selectedGenreFilter == genre
                        val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        val chipText = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        val chipBorder = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                .clickable { selectedGenreFilter = genre }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = genre.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = chipText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Dotted line
                val dottedColor = MaterialTheme.colorScheme.outline
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                ) {
                    drawLine(
                        color = dottedColor.copy(alpha = 0.2f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }

                // Scrollable prompts list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStarters) { starter ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    onPromptSelected(starter.text)
                                    onDismiss()
                                }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (starter.genre.uppercase()) {
                                                    "MYSTERY" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    "FANTASY" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                                    "SCI-FI" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = starter.genre.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (starter.genre.uppercase()) {
                                                "MYSTERY" -> MaterialTheme.colorScheme.secondary
                                                "FANTASY" -> MaterialTheme.colorScheme.tertiary
                                                "SCI-FI" -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                Text(
                                    text = starter.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val randomStarter = storyStartersList.randomOrNull()
                    if (randomStarter != null) {
                        onPromptSelected(randomStarter.text)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.btn_surprise_me),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    )
}
