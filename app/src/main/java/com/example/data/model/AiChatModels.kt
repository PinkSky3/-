package com.example.data.model

import com.squareup.moshi.JsonClass

data class ModelHealth(
    val id: String,
    val displayName: String,
    val isOnline: Boolean
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    val choices: List<Choice>? = null
)

@JsonClass(generateAdapter = true)
data class Choice(
    val message: ChatMessage? = null
)

@JsonClass(generateAdapter = true)
data class ModelsListResponse(
    val `object`: String? = null,
    val data: List<ModelInfo>? = null
)

@JsonClass(generateAdapter = true)
data class ModelInfo(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val owned_by: String? = null,
    val model: String? = null,
    val model_type: String? = null,
    val channel_type: String? = null
)
