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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.LakesCorp.FunCoStory.ui.GameViewModel
import com.LakesCorp.FunCoStory.ui.GameViewModelFactory
import com.LakesCorp.FunCoStory.data.CompletedStory
import com.LakesCorp.FunCoStory.data.StoryRepository

enum class Screen {
    SETUP, GUIDE, WRITE, ARCHIVE
}

val whitespaceRegex = "\\s+".toRegex()

class MediaPlaybackManager(private val context: Context) : DefaultLifecycleObserver {
    private var soundPool: SoundPool? = null
    private var keySoundId: Int = 0
    private var returnSoundId: Int = 0

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var onSpeechStateChanged: ((Boolean) -> Unit)? = null

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

    fun initTts(onSpeechStateChanged: (Boolean) -> Unit, onInitCompleted: () -> Unit) {
        this.onSpeechStateChanged = onSpeechStateChanged
        if (tts == null) {
            val handler = Handler(Looper.getMainLooper())
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            handler.post { onSpeechStateChanged(true) }
                        }
                        override fun onDone(utteranceId: String?) {
                            handler.post { onSpeechStateChanged(false) }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            handler.post { onSpeechStateChanged(false) }
                        }
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            handler.post { onSpeechStateChanged(false) }
                        }
                    })
                    handler.post { onInitCompleted() }
                }
            }
        } else if (isTtsInitialized) {
            onInitCompleted()
        }
    }

    fun speak(text: String) {
        if (isTtsInitialized) {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "story_tts")
            }
            tts?.language = Locale.getDefault()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "story_tts")
            onSpeechStateChanged?.invoke(true)
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        onSpeechStateChanged?.invoke(false)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        soundPool?.release()
        soundPool = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
        onSpeechStateChanged = null
    }
}

fun playSoundForTextChange(oldText: String, newText: String, soundManager: MediaPlaybackManager) {
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



@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var mediaPlaybackManager: MediaPlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPlaybackManager = MediaPlaybackManager(this)
        lifecycle.addObserver(mediaPlaybackManager)
        setContent {
            InkAndEchoTheme {
                MainAppContainer(mediaPlaybackManager)
            }
        }
    }
}

// Draw the tactile dotted paper grid
fun Modifier.paperGrid(gridColor: Color): Modifier = this.drawWithCache {
    val dotRadius = 0.5.dp.toPx()
    val stepPx = 8.dp.toPx()
    val stepInt = stepPx.toInt().coerceAtLeast(1)

    val tileBitmap = ImageBitmap(stepInt, stepInt)
    val canvas = Canvas(tileBitmap)
    val paint = Paint().apply {
        color = gridColor
        isAntiAlias = true
    }
    canvas.drawCircle(Offset(0f, 0f), dotRadius, paint)

    val shader = ImageShader(tileBitmap, TileMode.Repeated, TileMode.Repeated)
    val brush = ShaderBrush(shader)

    onDrawBehind {
        drawRect(brush)
    }
}

@Composable
fun MainAppContainer(soundManager: MediaPlaybackManager) {
    val defaultPrompt = stringResource(id = R.string.prompt_placeholder)
    val appTitle = stringResource(id = R.string.app_name)

    val context = LocalContext.current
    val storyRepository = remember { StoryRepository(context.applicationContext) }
    val viewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(storyRepository)
    )

    val completedStories by viewModel.completedStories.collectAsState()

    Scaffold(
        bottomBar = {
            InkBottomNavigationBar(
                currentScreen = viewModel.currentScreen,
                onScreenSelected = { screen ->
                    if (screen == Screen.WRITE && !viewModel.gameInProgress) {
                        viewModel.navigateTo(Screen.SETUP)
                    } else {
                        viewModel.navigateTo(screen)
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
            when (viewModel.currentScreen) {
                Screen.GUIDE -> {
                    TutorialScreen(
                        onStartSession = {
                            viewModel.navigateTo(Screen.SETUP)
                        }
                    )
                }
                Screen.SETUP -> {
                    GameSetupScreen(
                        scribblersCount = viewModel.scribblersCount,
                        onScribblersChanged = { viewModel.updateScribblersCount(it) },
                        roundsCount = viewModel.roundsCount,
                        onRoundsChanged = { viewModel.updateRoundsCount(it) },
                        hintLength = viewModel.hintLength,
                        onHintLengthChanged = { viewModel.updateHintLength(it) },
                        storyPrompt = viewModel.storyPrompt,
                        onStoryPromptChanged = { viewModel.updateStoryPrompt(it) },
                        writerNames = viewModel.writerNames,
                        onWriterNamesChanged = { viewModel.updateWriterNames(it) },
                        soundManager = soundManager,
                        onStartGame = { viewModel.startGame() }
                    )
                }
                Screen.WRITE -> {
                    val totalTurns = viewModel.scribblersCount * viewModel.roundsCount
                    val currentWriterIdx = (viewModel.currentTurn - 1) % viewModel.scribblersCount
                    val currentWriterName = viewModel.writerNames.getOrNull(currentWriterIdx)?.ifBlank { null }
                        ?: context.getString(R.string.default_writer_name, currentWriterIdx + 1)
                    WritingDeskScreen(
                        currentTurn = viewModel.currentTurn,
                        totalTurns = totalTurns,
                        writerName = currentWriterName,
                        echoText = viewModel.lastWordsEcho,
                        hintLength = viewModel.hintLength,
                        soundManager = soundManager,
                        onSealScroll = { text ->
                            viewModel.sealScroll(
                                text = text,
                                defaultWriterName = { index -> context.getString(R.string.default_writer_name, index + 1) },
                                appTitle = appTitle
                            )
                        }
                    )
                }
                Screen.ARCHIVE -> {
                    StoryArchiveScreen(
                        stories = completedStories,
                        onStoryClick = { story ->
                            viewModel.selectedReaderStory = story
                        },
                        onDeleteStory = { story ->
                            viewModel.deleteStory(story)
                        }
                    )
                }
            }

            // Overlay dialog for passing the phone
            if (viewModel.showPassPhoneDialog) {
                val nextWriterIdx = (viewModel.currentTurn - 1) % viewModel.scribblersCount
                val nextWriterName = viewModel.writerNames.getOrNull(nextWriterIdx)?.ifBlank { null }
                    ?: context.getString(R.string.default_writer_name, nextWriterIdx + 1)
                PassPhoneDialog(
                    nextWriterName = nextWriterName,
                    onDismiss = { viewModel.showPassPhoneDialog = false }
                )
            }

            // Fullscreen story reader overlay
            AnimatedVisibility(
                visible = viewModel.selectedReaderStory != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                viewModel.selectedReaderStory?.let { story ->
                    StoryReaderOverlay(
                        story = story,
                        soundManager = soundManager,
                        onClose = { viewModel.selectedReaderStory = null },
                        onDelete = {
                            viewModel.deleteStory(story)
                            viewModel.selectedReaderStory = null
                        }
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









// SCREEN 4: Story Archive Screen
@Composable
fun StoryArchiveScreen(
    stories: List<CompletedStory>,
    onStoryClick: (CompletedStory) -> Unit,
    onDeleteStory: (CompletedStory) -> Unit
) {
    var storyToDelete by remember { mutableStateOf<CompletedStory?>(null) }

    if (storyToDelete != null) {
        AlertDialog(
            onDismissRequest = { storyToDelete = null },
            title = {
                Text(
                    text = stringResource(id = R.string.dialog_delete_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.dialog_delete_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storyToDelete?.let { onDeleteStory(it) }
                        storyToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { storyToDelete = null }
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_delete_cancel),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        )
    }

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
                    StoryGridItem(
                        story = story,
                        onClick = { onStoryClick(story) },
                        onDelete = { storyToDelete = story }
                    )
                }
            }
        }
    }
}

// Single story item card in archive list
@Composable
fun StoryGridItem(
    story: CompletedStory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    PaperContainer(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = story.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete story",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
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
    soundManager: MediaPlaybackManager,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isTtsInitialized by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var showDeleteConfirmInReader by remember { mutableStateOf(false) }

    if (showDeleteConfirmInReader) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmInReader = false },
            title = {
                Text(
                    text = stringResource(id = R.string.dialog_delete_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.dialog_delete_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmInReader = false
                        onDelete()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmInReader = false }
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_delete_cancel),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        )
    }

    LaunchedEffect(Unit) {
        soundManager.initTts(
            onSpeechStateChanged = { speaking ->
                isSpeaking = speaking
            },
            onInitCompleted = {
                isTtsInitialized = true
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.stopSpeaking()
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
                            if (isSpeaking) {
                                soundManager.stopSpeaking()
                            } else {
                                soundManager.speak(story.fullText)
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
                        val sanitizedTitle = story.title.take(100).replace(Regex("[^\\w\\s\\-#]"), "")
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "$sanitizedTitle\n\n${story.fullText}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        try {
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            android.util.Log.e("StoryReaderOverlay", "Failed to start share activity", e)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(onClick = { showDeleteConfirmInReader = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete story",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
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

