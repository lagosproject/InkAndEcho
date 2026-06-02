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
import com.LakesCorp.FunCoStory.ui.components.PassPhoneDialog
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













