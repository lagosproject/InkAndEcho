package com.LakesCorp.FunCoStory.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "ink_and_echo_prefs")

class StoryRepository(private val context: Context) {
    private val storiesKey = stringPreferencesKey("completed_stories")

    val completedStoriesFlow: Flow<List<CompletedStory>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val storiesStr = preferences[storiesKey] ?: return@map emptyList()
            parseStoriesJson(storiesStr)
        }

    suspend fun saveCompletedStories(stories: List<CompletedStory>) {
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
        context.dataStore.edit { preferences ->
            preferences[storiesKey] = jsonArray.toString()
        }
    }

    private fun parseStoriesJson(storiesStr: String): List<CompletedStory> {
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
            android.util.Log.e("StoryRepository", "Error parsing stories JSON", e)
        }
        return stories
    }
}
