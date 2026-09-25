package com.example.medmitra.util

import android.util.Log
import java.util.Locale

data class ParsedPrescriptionItem(
    val name: String,
    val dosage: String,
    val times: List<String>,
    val instructions: String
)

object OfflinePrescriptionParser {

    private const val TAG = "OfflinePrescriptionParser"

    // Common medical prefixes
    private val MEDICAL_PREFIX_REGEX = Regex(
        "^(?:Tab(?:let)?s?|Cap(?:sule)?s?|Syr(?:up)?s?|Inj(?:ection)?s?|Oint(?:ment)?s?|Drops?|T\\.|C\\.|S\\.)\\s*",
        RegexOption.IGNORE_CASE
    )

    // Common drug name suffixes
    private val DRUG_SUFFIX_REGEX = Regex(
        "\\b[A-Za-z]{3,}(?:in|ol|am|ide|cin|mab|ine|one|tan|vir|stat|pril|zole|lol|sartan|pine|statin|mycin|fen|nac|cillin|xone|lam|pam|tid|zol|cef|dox|cept|ib|tin|xine)\\b",
        RegexOption.IGNORE_CASE
    )

    // Dosages: e.g. 500mg, 500 mg, 10 ml, 250 mcg, 1 tablet, 2 capsules
    private val DOSAGE_REGEX = Regex(
        "\\b(\\d+(?:\\.\\d+)?)\\s*(mg/ml|mg|g|ml|mcg|tablets?|tabs?|pills?|capsules?|caps?|iu)\\b",
        RegexOption.IGNORE_CASE
    )

    // Numeric schedules: e.g. 1-0-1, 1-1-1, 0-1-0, 1-0-0, 0-0-1, 1 - 0 - 1
    private val NUMERIC_SCHEDULE_REGEX = Regex(
        "\\b([0-3])\\s*[-–—/]\\s*([0-3])\\s*[-–—/]\\s*([0-3])\\b"
    )

    // Explicit time regex: e.g. 08:00 AM, 20:30, 8:00pm, 10:00
    private val TIME_STRING_REGEX = Regex(
        "\\b(\\d{1,2}:\\d{2}\\s*(?:AM|PM|am|pm)?)\\b"
    )

    // Frequency keywords
    private val TWICE_DAILY_REGEX = Regex("\\b(?:twice\\s+daily|2\\s*times?\\s*(?:a\\s*day|daily)?|b\\.?i\\.?d\\.?|bid)\\b", RegexOption.IGNORE_CASE)
    private val THREE_TIMES_REGEX = Regex("\\b(?:three\\s+times?\\s*(?:a\\s*day|daily)?|thrice\\s+daily|3\\s*times?\\s*(?:a\\s*day|daily)?|t\\.?i\\.?d\\.?|tid)\\b", RegexOption.IGNORE_CASE)
    private val FOUR_TIMES_REGEX = Regex("\\b(?:four\\s+times?\\s*(?:a\\s*day|daily)?|4\\s*times?\\s*(?:a\\s*day|daily)?|q\\.?i\\.?d\\.?|qid)\\b", RegexOption.IGNORE_CASE)
    private val ONCE_DAILY_REGEX = Regex("\\b(?:once\\s+daily|1\\s*time\\s*(?:a\\s*day|daily)?|once\\s*a\\s*day|q\\.?d\\.?|qd)\\b", RegexOption.IGNORE_CASE)

    // Time of day keywords
    private val MORNING_REGEX = Regex("\\b(?:morning|breakfast|a\\.m\\.|am)\\b", RegexOption.IGNORE_CASE)
    private val AFTERNOON_REGEX = Regex("\\b(?:afternoon|lunch|noon)\\b", RegexOption.IGNORE_CASE)
    private val NIGHT_REGEX = Regex("\\b(?:night|evening|bedtime|p\\.m\\.|pm)\\b", RegexOption.IGNORE_CASE)

    // Instructions
    private val INSTRUCTIONS_REGEX = Regex(
        "\\b(?:before\\s+(?:food|meals)|after\\s+(?:food|meals)|with\\s+(?:food|water)|empty\\s+stomach|at\\s+bedtime|a\\.c\\.|p\\.c\\.|with\\s+meals?)\\b",
        RegexOption.IGNORE_CASE
    )

    fun parse(rawText: String): List<ParsedPrescriptionItem> {
        if (rawText.isBlank()) return emptyList()

        val items = mutableListOf<ParsedPrescriptionItem>()
        val lines = rawText.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var currentLineIndex = 0
        while (currentLineIndex < lines.size) {
            val line = lines[currentLineIndex]

            val isMeta = isMetadataLine(line)
            val hasDosageOrPattern = DOSAGE_REGEX.containsMatchIn(line) || NUMERIC_SCHEDULE_REGEX.containsMatchIn(line)

            if (isMeta && !hasDosageOrPattern) {
                currentLineIndex++
                continue
            }

            val candidateItem = parseLineOrBlock(line, lines.getOrNull(currentLineIndex + 1))
            if (candidateItem != null) {
                items.add(candidateItem.first)
                currentLineIndex += candidateItem.second
            } else {
                currentLineIndex++
            }
        }

        try {
            Log.d(TAG, "Parsed ${items.size} prescription items from text.")
        } catch (_: Throwable) {
            // Ignored in pure unit tests where Log is not mocked
        }
        return items
    }

    private fun isMetadataLine(line: String): Boolean {
        val lower = line.lowercase()
        val metadataKeywords = listOf(
            "dr.", "dr ", "doctor", "clinic", "hospital", "st.", "patient",
            "date:", "date ", "rx", "signature", "age:", "gender:",
            "address", "phone", "tel:", "email", "diagnosis", "prescription"
        )
        return metadataKeywords.any { lower.contains(it) }
    }

    private fun parseLineOrBlock(line: String, nextLine: String?): Pair<ParsedPrescriptionItem, Int>? {
        var textToAnalyze = line
        var linesConsumed = 1

        val hasDosageCurrent = DOSAGE_REGEX.containsMatchIn(line)
        val hasScheduleCurrent = NUMERIC_SCHEDULE_REGEX.containsMatchIn(line) ||
                TWICE_DAILY_REGEX.containsMatchIn(line) ||
                THREE_TIMES_REGEX.containsMatchIn(line) ||
                TIME_STRING_REGEX.containsMatchIn(line)
        val hasDrugSuffix = DRUG_SUFFIX_REGEX.containsMatchIn(line)
        val hasMedicalPrefix = MEDICAL_PREFIX_REGEX.containsMatchIn(line)

        if (nextLine != null && !isMetadataLine(nextLine)) {
            val hasScheduleNext = NUMERIC_SCHEDULE_REGEX.containsMatchIn(nextLine) ||
                    TWICE_DAILY_REGEX.containsMatchIn(nextLine) ||
                    THREE_TIMES_REGEX.containsMatchIn(nextLine) ||
                    TIME_STRING_REGEX.containsMatchIn(nextLine) ||
                    INSTRUCTIONS_REGEX.containsMatchIn(nextLine)
            val hasDosageNext = DOSAGE_REGEX.containsMatchIn(nextLine)

            if (!hasScheduleCurrent && hasScheduleNext && !MEDICAL_PREFIX_REGEX.containsMatchIn(nextLine)) {
                textToAnalyze = "$line $nextLine"
                linesConsumed = 2
            } else if (!hasDosageCurrent && !hasScheduleCurrent && (hasDrugSuffix || hasMedicalPrefix) && (hasDosageNext || hasScheduleNext)) {
                textToAnalyze = "$line $nextLine"
                linesConsumed = 2
            }
        }

        val isCandidate = DOSAGE_REGEX.containsMatchIn(textToAnalyze) ||
                NUMERIC_SCHEDULE_REGEX.containsMatchIn(textToAnalyze) ||
                (hasDrugSuffix && (hasScheduleCurrent || hasDosageCurrent)) ||
                hasMedicalPrefix

        if (!isCandidate) return null

        val dosageMatch = DOSAGE_REGEX.find(textToAnalyze)
        val dosage = dosageMatch?.value ?: "1 tablet"

        val times = extractTimes(textToAnalyze)

        val instructionsMatch = INSTRUCTIONS_REGEX.find(textToAnalyze)
        val instructions = when {
            instructionsMatch != null -> formatInstruction(instructionsMatch.value)
            textToAnalyze.contains("after", ignoreCase = true) -> "After food"
            textToAnalyze.contains("before", ignoreCase = true) -> "Before food"
            else -> "Take as directed"
        }

        val name = extractMedicineName(textToAnalyze, dosageMatch?.value, instructionsMatch?.value)
        if (name.length < 3 || isGenericWord(name)) return null

        val item = ParsedPrescriptionItem(
            name = name,
            dosage = dosage,
            times = times,
            instructions = instructions
        )

        return Pair(item, linesConsumed)
    }

    private fun extractTimes(text: String): List<String> {
        val timeMatches = TIME_STRING_REGEX.findAll(text).map { it.value.trim() }.toList()
        if (timeMatches.isNotEmpty()) {
            return timeMatches.map { formatTimeString(it) }
        }

        val numMatch = NUMERIC_SCHEDULE_REGEX.find(text)
        if (numMatch != null) {
            val (m, a, n) = numMatch.destructured
            val result = mutableListOf<String>()
            if ((m.toIntOrNull() ?: 0) > 0) result.add("08:00")
            if ((a.toIntOrNull() ?: 0) > 0) result.add("14:00")
            if ((n.toIntOrNull() ?: 0) > 0) result.add("20:00")
            if (result.isNotEmpty()) return result
        }

        if (THREE_TIMES_REGEX.containsMatchIn(text)) return listOf("08:00", "14:00", "20:00")
        if (FOUR_TIMES_REGEX.containsMatchIn(text)) return listOf("06:00", "12:00", "18:00", "22:00")
        if (TWICE_DAILY_REGEX.containsMatchIn(text)) return listOf("08:00", "20:00")
        if (ONCE_DAILY_REGEX.containsMatchIn(text)) return listOf("08:00")

        val timeOfDayList = mutableListOf<String>()
        if (MORNING_REGEX.containsMatchIn(text)) timeOfDayList.add("08:00")
        if (AFTERNOON_REGEX.containsMatchIn(text)) timeOfDayList.add("14:00")
        if (NIGHT_REGEX.containsMatchIn(text)) timeOfDayList.add("20:00")
        if (timeOfDayList.isNotEmpty()) return timeOfDayList

        return listOf("08:00", "20:00")
    }

    private fun extractMedicineName(
        text: String,
        matchedDosage: String?,
        matchedInstruction: String?
    ): String {
        var clean = text

        clean = clean.replace(Regex("^\\d+[.)-]\\s*"), "")
        clean = clean.replace(MEDICAL_PREFIX_REGEX, "")

        if (matchedDosage != null) {
            clean = clean.replace(matchedDosage, "", ignoreCase = true)
        }

        clean = clean.replace(NUMERIC_SCHEDULE_REGEX, "")
        clean = clean.replace(TIME_STRING_REGEX, "")

        if (matchedInstruction != null) {
            clean = clean.replace(matchedInstruction, "", ignoreCase = true)
        }

        clean = clean.replace(TWICE_DAILY_REGEX, "")
            .replace(THREE_TIMES_REGEX, "")
            .replace(FOUR_TIMES_REGEX, "")
            .replace(ONCE_DAILY_REGEX, "")
            .replace(MORNING_REGEX, "")
            .replace(AFTERNOON_REGEX, "")
            .replace(NIGHT_REGEX, "")

        clean = clean.replace(Regex("[^A-Za-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val words = clean.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return ""

        val candidateWords = words.takeWhile { word ->
            !word.equals("tablet", ignoreCase = true) &&
                    !word.equals("capsule", ignoreCase = true) &&
                    !word.equals("syrup", ignoreCase = true) &&
                    !word.equals("daily", ignoreCase = true) &&
                    !word.equals("days", ignoreCase = true) &&
                    !word.equals("take", ignoreCase = true) &&
                    !word.equals("after", ignoreCase = true) &&
                    !word.equals("before", ignoreCase = true)
        }

        val candidateName = candidateWords.joinToString(" ")
        val finalRaw = candidateName.ifBlank { words.firstOrNull() ?: "" }
        return finalRaw.split(" ").joinToString(" ") { it.capitalizeFirstLetter() }
    }

    private fun formatInstruction(instruction: String): String {
        val lower = instruction.lowercase()
        return when {
            lower.contains("before") -> "Before food"
            lower.contains("after") -> "After food"
            lower.contains("empty") -> "Empty stomach"
            lower.contains("bedtime") -> "At bedtime"
            lower.contains("water") -> "Take with water"
            else -> instruction.capitalizeFirstLetter()
        }
    }

    private fun formatTimeString(timeStr: String): String {
        val clean = timeStr.trim().uppercase()
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")
        val digits = clean.replace("AM", "").replace("PM", "").trim()
        val parts = digits.split(":")
        var hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0

        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    private fun isGenericWord(word: String): Boolean {
        val lower = word.lowercase()
        return lower in setOf(
            "tablet", "tablets", "capsule", "capsules", "syrup", "injection",
            "medicine", "medication", "daily", "twice", "thrice", "morning",
            "afternoon", "evening", "night", "doctor", "clinic", "hospital",
            "patient", "signature", "date", "take", "dose", "days"
        )
    }

    private fun String.capitalizeFirstLetter(): String {
        if (isEmpty()) return this
        return substring(0, 1).uppercase() + substring(1).lowercase()
    }
}
