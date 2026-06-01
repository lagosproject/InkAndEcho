package com.LakesCorp.FunCoStory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.LakesCorp.FunCoStory.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.LakesCorp.FunCoStory.R
import com.LakesCorp.FunCoStory.ui.AppLogoIcon
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.SolidColor

enum class Screen {
    SETUP, GUIDE, WRITE, ARCHIVE
}

class TypewriterSoundManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private var keySoundId: Int = 0
    private var returnSoundId: Int = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        keySoundId = soundPool?.load(context, R.raw.typewriter_key, 1) ?: 0
        returnSoundId = soundPool?.load(context, R.raw.typewriter_return, 1) ?: 0
    }

    fun playKeySound() {
        if (keySoundId != 0) {
            soundPool?.play(keySoundId, 0.8f, 0.8f, 1, 0, 1.0f)
        }
    }

    fun playReturnSound() {
        if (returnSoundId != 0) {
            soundPool?.play(returnSoundId, 0.8f, 0.8f, 1, 0, 1.0f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}

fun playSoundForTextChange(oldText: String, newText: String, soundManager: TypewriterSoundManager) {
    if (newText.length > oldText.length) {
        val addedText = getAddedText(oldText, newText)
        if (addedText.contains("\n")) {
            soundManager.playReturnSound()
        } else if (addedText.isNotEmpty()) {
            soundManager.playKeySound()
        }
    }
}

fun getAddedText(oldText: String, newText: String): String {
    var start = 0
    while (start < oldText.length && start < newText.length && oldText[start] == newText[start]) {
        start++
    }
    var oldEnd = oldText.length - 1
    var newEnd = newText.length - 1
    while (oldEnd >= start && newEnd >= start && oldText[oldEnd] == newText[newEnd]) {
        oldEnd--
        newEnd--
    }
    return if (start <= newEnd) newText.substring(start, newEnd + 1) else ""
}

data class CompletedStory(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val fullText: String,
    val authorsCount: Int,
    val genre: String,
    val authorList: List<String>
)

fun saveCompletedStories(context: Context, stories: List<CompletedStory>) {
    val prefs = context.getSharedPreferences("ink_and_echo_prefs", Context.MODE_PRIVATE)
    val jsonArray = JSONArray()
    for (story in stories) {
        val json = JSONObject().apply {
            put("id", story.id)
            put("title", story.title)
            put("date", story.date)
            put("fullText", story.fullText)
            put("authorsCount", story.authorsCount)
            put("genre", story.genre)
            val authors = JSONArray()
            story.authorList.forEach { authors.put(it) }
            put("authorList", authors)
        }
        jsonArray.put(json)
    }
    prefs.edit().putString("completed_stories", jsonArray.toString()).apply()
}

fun loadCompletedStories(context: Context): List<CompletedStory> {
    val prefs = context.getSharedPreferences("ink_and_echo_prefs", Context.MODE_PRIVATE)
    val storiesStr = prefs.getString("completed_stories", null) ?: return emptyList()
    val stories = mutableListOf<CompletedStory>()
    try {
        val jsonArray = JSONArray(storiesStr)
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val authors = mutableListOf<String>()
            val authorsArray = json.getJSONArray("authorList")
            for (j in 0 until authorsArray.length()) {
                authors.add(authorsArray.getString(j))
            }
            stories.add(
                CompletedStory(
                    id = json.getString("id"),
                    title = json.getString("title"),
                    date = json.getString("date"),
                    fullText = json.getString("fullText"),
                    authorsCount = json.getInt("authorsCount"),
                    genre = json.getString("genre"),
                    authorList = authors
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return stories
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InkAndEchoTheme {
                MainAppContainer()
            }
        }
    }
}

// Draw the tactile dotted paper grid
fun Modifier.paperGrid(gridColor: Color): Modifier = this.drawBehind {
    val dotRadius = 0.5.dp.toPx()
    val step = 8.dp.toPx()
    val width = size.width
    val height = size.height

    var x = 0f
    while (x < width) {
        var y = 0f
        while (y < height) {
            drawCircle(
                color = gridColor,
                radius = dotRadius,
                center = Offset(x, y)
            )
            y += step
        }
        x += step
    }
}

@Composable
fun MainAppContainer() {
    val defaultPrompt = stringResource(id = R.string.prompt_placeholder)
    val appTitle = stringResource(id = R.string.app_name)

    val context = LocalContext.current
    val soundManager = remember { TypewriterSoundManager(context) }
    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    // Navigation & Screen State
    var currentScreen by remember { mutableStateOf(Screen.GUIDE) }
    
    // Game Config State
    var scribblersCount by remember { mutableStateOf(4) }
    var hintLength by remember { mutableStateOf(3) }
    var storyPrompt by remember { mutableStateOf("") }
    var writerNames by remember { mutableStateOf(List(4) { "" }) }
    
    // Ongoing Game State
    var gameInProgress by remember { mutableStateOf(false) }
    var currentTurn by remember { mutableStateOf(1) }
    var storySegments by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastWordsEcho by remember { mutableStateOf("") }
    
    // Archive State
    var completedStories by remember {
        mutableStateOf(loadCompletedStories(context))
    }

    LaunchedEffect(completedStories) {
        saveCompletedStories(context, completedStories)
    }

    // Modal / Reader State
    var selectedReaderStory by remember { mutableStateOf<CompletedStory?>(null) }
    var showPassPhoneDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            InkBottomNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    // Prevent leaving a live game unless desired
                    if (screen == Screen.WRITE && !gameInProgress) {
                        // Not writing anything yet, go to setup first
                        currentScreen = Screen.SETUP
                    } else {
                        currentScreen = screen
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.paperGrid(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.GUIDE -> {
                    TutorialScreen(
                        onStartSession = {
                            currentScreen = Screen.SETUP
                        }
                    )
                }
                Screen.SETUP -> {
                    GameSetupScreen(
                        scribblersCount = scribblersCount,
                        onScribblersChanged = { count ->
                            scribblersCount = count
                            writerNames = if (writerNames.size < count) {
                                writerNames + List(count - writerNames.size) { "" }
                            } else {
                                writerNames.take(count)
                            }
                        },
                        hintLength = hintLength,
                        onHintLengthChanged = { length -> hintLength = length },
                        storyPrompt = storyPrompt,
                        onStoryPromptChanged = { prompt ->
                            storyPrompt = prompt
                        },
                        writerNames = writerNames,
                        onWriterNamesChanged = { writerNames = it },
                        soundManager = soundManager,
                        onStartGame = {
                            // Initialize new game state
                            currentTurn = 1
                            gameInProgress = true
                            if (storyPrompt.isBlank()) {
                                storySegments = emptyList()
                                lastWordsEcho = ""
                            } else {
                                storySegments = listOf(storyPrompt)
                                val words = storyPrompt.trim().split("\\s+".toRegex())
                                lastWordsEcho = "..." + words.takeLast(hintLength).joinToString(" ")
                            }
                            
                            currentScreen = Screen.WRITE
                        }
                    )
                }
                Screen.WRITE -> {
                    val currentWriterName = writerNames.getOrNull(currentTurn - 1)?.ifBlank { null }
                        ?: context.getString(R.string.default_writer_name, currentTurn)
                    WritingDeskScreen(
                        currentTurn = currentTurn,
                        totalTurns = scribblersCount,
                        writerName = currentWriterName,
                        echoText = lastWordsEcho,
                        soundManager = soundManager,
                        onSealScroll = { text ->
                            if (text.isNotBlank()) {
                                val updatedSegments = storySegments + text
                                storySegments = updatedSegments
                                
                                if (currentTurn >= scribblersCount) {
                                    // Compile finished story
                                    val fullStoryText = updatedSegments.joinToString("\n\n")
                                    val authorList = writerNames.mapIndexed { index, name ->
                                        name.ifBlank { context.getString(R.string.default_writer_name, index + 1) }
                                    }
                                    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
                                    
                                    val newStory = CompletedStory(
                                        title = "$appTitle #${completedStories.size + 1}",
                                        date = dateStr,
                                        fullText = fullStoryText,
                                        authorsCount = scribblersCount,
                                        genre = listOf("Mystery", "Fantasy", "Sci-Fi", "Drama").random(),
                                        authorList = authorList
                                    )
                                    completedStories = listOf(newStory) + completedStories
                                    
                                    // Reset game states
                                    storySegments = emptyList()
                                    storyPrompt = ""
                                    gameInProgress = false
                                    currentScreen = Screen.ARCHIVE
                                } else {
                                    // Prepare next turn
                                    val words = text.trim().split("\\s+".toRegex())
                                    lastWordsEcho = "..." + words.takeLast(hintLength).joinToString(" ")
                                    currentTurn += 1
                                    
                                    // Show "Pass the Phone" alert before next scribe types
                                    showPassPhoneDialog = true
                                }
                            }
                        }
                    )
                }
                Screen.ARCHIVE -> {
                    StoryArchiveScreen(
                        stories = completedStories,
                        onStoryClick = { story ->
                            selectedReaderStory = story
                        }
                    )
                }
            }

            // Overlay dialog for passing the phone
            if (showPassPhoneDialog) {
                val nextWriterName = writerNames.getOrNull(currentTurn - 1)?.ifBlank { null }
                    ?: context.getString(R.string.default_writer_name, currentTurn)
                PassPhoneDialog(
                    nextWriterName = nextWriterName,
                    onDismiss = { showPassPhoneDialog = false }
                )
            }

            // Fullscreen story reader overlay
            AnimatedVisibility(
                visible = selectedReaderStory != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedReaderStory?.let { story ->
                    StoryReaderOverlay(
                        story = story,
                        onClose = { selectedReaderStory = null }
                    )
                }
            }
        }
    }
}

// Skeuomorphic mechanical action key
@Composable
fun MechanicalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shadowColor: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowHeight = 3.dp
    
    val offsetVal by animateDpAsState(
        targetValue = if (isPressed) shadowHeight else 0.dp,
        label = "pressOffset"
    )

    Box(
        modifier = modifier
            .clickable(
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
                .background(shadowColor, shape = RoundedCornerShape(4.dp))
        )
        // Foreground clickable button layer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = offsetVal)
                .background(backgroundColor, shape = RoundedCornerShape(4.dp))
                .border(1.dp, shadowColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
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

// Bottom Navigation mimicking the Dymo Label/Notebook tabs
@Composable
fun InkBottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val items = listOf(
        Triple(Screen.GUIDE, R.string.tab_guide, Icons.Default.MenuBook),
        Triple(Screen.WRITE, R.string.tab_play, Icons.Default.PlayArrow),
        Triple(Screen.ARCHIVE, R.string.tab_archive, Icons.Default.Book)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            items.forEach { (screen, labelRes, icon) ->
                val label = stringResource(id = labelRes)
                val isActive = when (screen) {
                    Screen.WRITE -> currentScreen == Screen.WRITE || currentScreen == Screen.SETUP
                    else -> currentScreen == screen
                }
                val tabBg = if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Color.Transparent
                val iconColor = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(tabBg)
                        .clickable { onScreenSelected(screen) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconColor,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// SCREEN 1: Game Setup Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
    scribblersCount: Int,
    onScribblersChanged: (Int) -> Unit,
    hintLength: Int,
    onHintLengthChanged: (Int) -> Unit,
    storyPrompt: String,
    onStoryPromptChanged: (String) -> Unit,
    writerNames: List<String>,
    onWriterNamesChanged: (List<String>) -> Unit,
    soundManager: TypewriterSoundManager,
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

// SCREEN 2: Tutorial / How to Play
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

// SCREEN 3: Writing Desk Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingDeskScreen(
    currentTurn: Int,
    totalTurns: Int,
    writerName: String,
    echoText: String,
    soundManager: TypewriterSoundManager,
    onSealScroll: (String) -> Unit
) {
    var threadTextValue by remember(currentTurn) { mutableStateOf(TextFieldValue("")) }
    var lastText by remember(currentTurn) { mutableStateOf("") }
    var lastCursorLine by remember(currentTurn) { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current

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
        }

        Spacer(modifier = Modifier.weight(1f))

        // Seal the scroll button
        MechanicalButton(
            onClick = {
                if (threadTextValue.text.isNotBlank()) {
                    focusManager.clearFocus()
                    onSealScroll(threadTextValue.text)
                    threadTextValue = TextFieldValue("")
                }
            },
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

// SCREEN 4: Story Archive Screen
@Composable
fun StoryArchiveScreen(
    stories: List<CompletedStory>,
    onStoryClick: (CompletedStory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.archive_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.archive_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (stories.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = stringResource(id = R.string.no_stories),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stories) { story ->
                    StoryGridItem(story = story, onClick = { onStoryClick(story) })
                }
            }
        }
    }
}

// Single story item card in archive list
@Composable
fun StoryGridItem(
    story: CompletedStory,
    onClick: () -> Unit
) {
    PaperContainer(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = story.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = story.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = story.fullText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Dotted divider
        val dottedColor = MaterialTheme.colorScheme.outline
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            drawLine(
                color = dottedColor.copy(alpha = 0.3f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(id = R.string.authors_count, story.authorsCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Fullscreen story reader overlay dialog
@Composable
fun StoryReaderOverlay(
    story: CompletedStory,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsInitialized by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                ttsInstance?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        handler.post { isSpeaking = true }
                    }
                    override fun onDone(utteranceId: String?) {
                        handler.post { isSpeaking = false }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        handler.post { isSpeaking = false }
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        handler.post { isSpeaking = false }
                    }
                })
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .paperGrid(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.library_back),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.library_back),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isTtsInitialized) {
                        IconButton(onClick = {
                            tts?.let { ttsInstance ->
                                if (isSpeaking) {
                                    ttsInstance.stop()
                                    isSpeaking = false
                                } else {
                                    ttsInstance.language = Locale.getDefault()
                                    val params = Bundle().apply {
                                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "story_tts")
                                    }
                                    ttsInstance.speak(story.fullText, TextToSpeech.QUEUE_FLUSH, params, "story_tts")
                                    isSpeaking = true
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop reading" else "Read story",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "${story.title}\n\n${story.fullText}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Reader Canvas (Scrollable story text)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Story Title & Date
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completed ${story.date}".uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.05.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dotted divider
                    val readerDottedColor = MaterialTheme.colorScheme.outline
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            color = readerDottedColor.copy(alpha = 0.3f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Full story content in Courier Prime
                    Text(
                        text = story.fullText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 28.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Dotted divider
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            color = readerDottedColor.copy(alpha = 0.3f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Authors list
                    Text(
                        text = stringResource(id = R.string.authors_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        story.authorList.forEachIndexed { idx, author ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Pass Phone dialog overlay
@Composable
fun PassPhoneDialog(
    nextWriterName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.dialog_pass_title),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.dialog_pass_text, nextWriterName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_pass_btn),
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

// Card wrapper mimicking paper card layering
@Composable
fun PaperContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(20.dp)
    ) {
        content()
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

