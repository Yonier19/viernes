package com.example.memory

data class LearningSuggestion(
    val phrase: String,
    val action: String, // e.g. "OPEN_APP"
    val target: String, // e.g. "Spotify"
    val confidence: Float, // e.g. 0.85f..1.0f
    val occurrences: Int
)

class LearningEngine(
    private val memoryDao: MemoryDao,
    private val commandHistoryDao: CommandHistoryDao,
    private val minOccurrencesThreshold: Int = 3
) {

    /**
     * Analyzes recent local command history to detect repetitive associations.
     * Only patterns with >= minOccurrencesThreshold and high consistency are proposed.
     */
    suspend fun detectPatterns(): List<LearningSuggestion> {
        val recentLogs = commandHistoryDao.getRecentHistorySync(100)
        if (recentLogs.size < minOccurrencesThreshold) {
            return emptyList()
        }

        // Group by phrase and target where intent was successful
        // Filter out commands that are already saved as active COMMAND_ALIAS memories
        val existingAliases = memoryDao.getMemoriesByTypeSync(MemoryType.COMMAND_ALIAS)
            .map { it.key.trim().lowercase() }
            .toSet()

        val occurrencesMap = mutableMapOf<Pair<String, String>, MutableList<CommandHistoryEntity>>()

        for (log in recentLogs) {
            val phrase = log.rawText.trim().lowercase()
            val target = log.target?.trim()
            if (phrase.isNotBlank() && !target.isNullOrBlank() && log.isSuccess) {
                // If already learned, skip
                if (existingAliases.contains(phrase)) continue

                val key = Pair(phrase, target)
                occurrencesMap.getOrPut(key) { mutableListOf() }.add(log)
            }
        }

        val suggestions = mutableListOf<LearningSuggestion>()

        for ((pair, historyList) in occurrencesMap) {
            val (phrase, target) = pair
            val occurrences = historyList.size

            if (occurrences >= minOccurrencesThreshold) {
                // Calculate confidence based on frequency and ratio against total phrase uses
                val totalPhraseUses = recentLogs.count { it.rawText.trim().equals(phrase, ignoreCase = true) }
                val consistencyRatio = if (totalPhraseUses > 0) occurrences.toFloat() / totalPhraseUses else 1.0f

                // Confidence formula: base on consistency with cap
                val confidence = (0.70f + (occurrences * 0.05f).coerceAtMost(0.25f)) * consistencyRatio
                val clampedConfidence = confidence.coerceIn(0.5f, 0.98f)

                suggestions.add(
                    LearningSuggestion(
                        phrase = phrase,
                        action = "OPEN_APP",
                        target = target,
                        confidence = ((clampedConfidence * 100).toInt()) / 100f,
                        occurrences = occurrences
                    )
                )
            }
        }

        return suggestions.sortedByDescending { it.confidence }
    }
}
