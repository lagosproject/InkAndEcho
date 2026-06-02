package com.LakesCorp.FunCoStory.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.LakesCorp.FunCoStory.CompletedStory
import com.LakesCorp.FunCoStory.Screen
import com.LakesCorp.FunCoStory.StoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(private val repository: StoryRepository) : ViewModel() {

    // Navigation & Screen State
    var currentScreen by mutableStateOf(Screen.GUIDE)
        private set

    // Game Config State
    var scribblersCount by mutableStateOf(4)
        private set
    var roundsCount by mutableStateOf(1)
        private set
    var hintLength by mutableStateOf(3)
        private set
    var storyPrompt by mutableStateOf("")
        private set
    var writerNames by mutableStateOf(List(4) { "" })
        private set

    // Ongoing Game State
    var gameInProgress by mutableStateOf(false)
        private set
    var currentTurn by mutableStateOf(1)
        private set
    var storySegments by mutableStateOf<List<String>>(emptyList())
        private set
    var lastWordsEcho by mutableStateOf("")
        private set

    // Modal / Reader State
    var selectedReaderStory by mutableStateOf<CompletedStory?>(null)
    var showPassPhoneDialog by mutableStateOf(false)

    // flow representing archive stories
    val completedStories: StateFlow<List<CompletedStory>> = repository.completedStoriesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun updateScribblersCount(count: Int) {
        scribblersCount = count
        if (writerNames.size != count) {
            writerNames = List(count) { index ->
                writerNames.getOrNull(index) ?: ""
            }
        }
    }

    fun updateRoundsCount(count: Int) {
        roundsCount = count
    }

    fun updateHintLength(length: Int) {
        hintLength = length
    }

    fun updateStoryPrompt(prompt: String) {
        storyPrompt = prompt
    }

    fun updateWriterNames(names: List<String>) {
        writerNames = names
    }

    fun startGame() {
        currentTurn = 1
        gameInProgress = true
        if (storyPrompt.isBlank()) {
            storySegments = emptyList()
            lastWordsEcho = ""
        } else {
            storySegments = listOf(storyPrompt)
            val words = storyPrompt.trim().split(Regex("\\s+"))
            lastWordsEcho = "..." + words.takeLast(hintLength).joinToString(" ")
        }
        currentScreen = Screen.WRITE
    }

    fun sealScroll(text: String, defaultWriterName: (Int) -> String, appTitle: String) {
        if (text.isBlank()) return
        
        val updatedSegments = storySegments + text
        storySegments = updatedSegments
        val totalTurns = scribblersCount * roundsCount

        if (currentTurn >= totalTurns) {
            val fullStoryText = updatedSegments.joinToString("\n\n")
            val authorList = writerNames.mapIndexed { index, name ->
                val cleanName = name.replace(Regex("[\\r\\n\\t]"), "").trim().take(50)
                cleanName.ifBlank { defaultWriterName(index) }
            }
            val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            
            val newStory = CompletedStory(
                title = "$appTitle #${completedStories.value.size + 1}",
                date = dateStr,
                fullText = fullStoryText,
                authorsCount = scribblersCount,
                genre = listOf("Mystery", "Fantasy", "Sci-Fi", "Drama").random(),
                authorList = authorList
            )
            
            saveStory(listOf(newStory) + completedStories.value)
            
            storySegments = emptyList()
            storyPrompt = ""
            gameInProgress = false
            currentScreen = Screen.ARCHIVE
        } else {
            val words = text.trim().split(Regex("\\s+"))
            lastWordsEcho = "..." + words.takeLast(hintLength).joinToString(" ")
            currentTurn += 1
            showPassPhoneDialog = true
        }
    }

    fun deleteStory(story: CompletedStory) {
        saveStory(completedStories.value.filter { it.id != story.id })
    }

    private fun saveStory(stories: List<CompletedStory>) {
        viewModelScope.launch {
            repository.saveCompletedStories(stories)
        }
    }
}

class GameViewModelFactory(private val repository: StoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
