package com.example.avenor

import com.example.avenor.data.AvenorClass
import com.example.avenor.data.Flashcard
import com.example.avenor.data.Priority
import com.example.avenor.domain.Leitner
import com.example.avenor.domain.QuickAddParser
import com.example.avenor.domain.Streaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Run with ./gradlew test. Same cases are checked for the iOS and web versions of the engine. */
class EngineTest {
    private val friday = LocalDate.of(2026, 10, 23)
    private val bio = AvenorClass(id = "bio", name = "Bio")

    @Test fun quickAddParsesEverything() {
        val r = QuickAddParser.parse("Bio essay fri 45m !", listOf(bio), friday)
        assertEquals("Bio essay", r.title)
        assertEquals(friday, r.dueDate)
        assertEquals(45, r.minutes)
        assertEquals(Priority.HIGH, r.priority)
        assertEquals("bio", r.classId)
    }

    @Test fun satPrepIsNotSaturday() {
        val r = QuickAddParser.parse("SAT prep sat 2h", emptyList(), friday)
        assertEquals("SAT prep", r.title)
        assertEquals(friday.plusDays(1), r.dueDate)
        assertEquals(120, r.minutes)
    }

    @Test fun dueTomorrowAndChapterNumbersStayInTitle() {
        val r = QuickAddParser.parse("Read ch 5 due tomorrow", emptyList(), friday)
        assertEquals("Read ch 5", r.title)
        assertEquals(friday.plusDays(1), r.dueDate)
        assertNull(r.minutes)
    }

    @Test fun kindStreakForgivesOneMissedDay() {
        val days = setOf(friday, friday.minusDays(2), friday.minusDays(3))
        assertEquals(3, Streaks.current(days, friday))
        assertEquals(1, Streaks.current(setOf(friday, friday.minusDays(3)), friday))
        assertEquals(0, Streaks.current(emptySet(), friday))
    }

    @Test fun leitnerPromotesAndResets() {
        val card = Flashcard(front = "Q", back = "A", box = 2)
        assertEquals(3, Leitner.review(card, remembered = true, relearning = false).box)
        assertEquals(1, Leitner.review(card, remembered = false, relearning = false).box)
        assertEquals(1, Leitner.review(card, remembered = true, relearning = true).box)
    }
}
