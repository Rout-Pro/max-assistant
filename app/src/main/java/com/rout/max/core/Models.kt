package com.rout.max.core

data class MAXAction(
    val type: String,
    val params: Map<String, Any?> = emptyMap()
)

data class BrainResponse(
    val speech: String,
    val emotion: String,
    val expectFollowUp: Boolean,
    val actions: List<MAXAction>
)

data class ChatTurn(val role: String, val content: String)

/** Live assistant states. The orb and status line must always reflect this real state. */
enum class AssistantState(val label: String) {
    IDLE("Idle"),
    LISTENING("Listening"),
    THINKING("Thinking"),
    SPEAKING("Speaking"),
    EXECUTING("Executing"),
    STOPPED("Stopped")
}

enum class PersonalityMode(val label: String, val desc: String) {
    BUTLER("JARVIS Butler", "Polite Sir-style butler, crisp and proactive"),
    FRIENDLY("Friendly", "Natural, relaxed, helpful"),
    SWEET("Sweet", "Warm, caring, encouraging"),
    MISCHIEF("Mischief", "Light teasing and humor"),
    ANIME("Anime-Inspired", "Anime-style expressions, original character"),
    PROFESSIONAL("Professional", "Efficient, factual, task-focused"),
    SERIOUS_SUPPORT("Serious Support", "Calm, empathetic, no teasing")
}

data class MemoryEntry(val id: Long, val text: String, val createdAt: Long)

enum class TaskStepStatus { RUNNING, DONE, FAILED, SKIPPED, WAITING_CONFIRM }

data class TaskLogEntry(
    val step: String,
    val status: TaskStepStatus,
    val detail: String = "",
    val at: Long = System.currentTimeMillis()
)
