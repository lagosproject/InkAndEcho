package com.LakesCorp.FunCoStory.data

import java.util.UUID

data class CompletedStory(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val fullText: String,
    val authorsCount: Int,
    val genre: String,
    val authorList: List<String>
)
