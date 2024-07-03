package com.example.teamproject_cv2.mainScreen

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DiaryEntry(
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val selectedEmojiIndex: Int = 0,
    val selectedDate: LocalDate = LocalDate.now(),
    val emotion: String = "",
    val emotionScore: Double = 0.0,
    val isPlaceholder: Boolean = false,
    val onClick: (() -> Unit)? = null
)

data class DiaryEntryFirestore(
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val selectedEmojiIndex: Int = 0,
    val selectedDate: String = "",  // LocalDate 대신 String
    val emotion: String = "",
    val emotionScore: Double = 0.0
)

suspend fun getDiaryEntries(firestore: FirebaseFirestore): List<DiaryEntry> {
    val today = LocalDate.now()
    val tenDaysAgo = today.minusDays(9)
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val displayFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

    val existingEntries = try {
        val snapshot = firestore.collection("diaries")
            .whereGreaterThanOrEqualTo("timestamp", tenDaysAgo.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        snapshot.documents.mapNotNull { it.toObject(DiaryEntryFirestore::class.java)?.toDiaryEntry() }
    } catch (e: Exception) {
        emptyList()
    }

    val allEntries = mutableListOf<DiaryEntry>()
    var currentDate = today

    while (currentDate.isAfter(tenDaysAgo) || currentDate.isEqual(tenDaysAgo)) {
        val formattedDate = currentDate.format(dateFormatter)
        val displayDate = displayFormatter.format(currentDate)
        val existingEntry = existingEntries.find { it.selectedDate == currentDate }

        if (existingEntry != null) {
            allEntries.add(existingEntry)
        } else {
            allEntries.add(DiaryEntry(
                text = "$displayDate 일기를 작성해주세요!",
                selectedDate = currentDate,
                isPlaceholder = true,
                onClick = { /* 클릭 시 처리할 작업 설정 */ }
            ))
        }

        currentDate = currentDate.minusDays(1)
    }

    return allEntries
}

private fun DiaryEntryFirestore.toDiaryEntry(): DiaryEntry {
    return DiaryEntry(
        text = this.text,
        imageUrl = this.imageUrl,
        timestamp = this.timestamp,
        selectedEmojiIndex = this.selectedEmojiIndex,
        selectedDate = LocalDate.parse(this.selectedDate),  // String을 LocalDate로 변환
        emotion = this.emotion,
        emotionScore = this.emotionScore
    )
}