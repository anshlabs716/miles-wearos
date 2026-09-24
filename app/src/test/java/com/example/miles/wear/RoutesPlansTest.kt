package com.example.miles.wear

import com.example.miles.wear.data.model.TrainingCatalog
import com.example.miles.wear.data.model.WorkoutMode
import com.example.miles.wear.data.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saved routes + progressive training plan behavior (all real plans). */
class RoutesPlansTest {

    @Test
    fun `catalog has the four core programs`() {
        assertEquals(4, TrainingCatalog.all.size)
        assertEquals(listOf("c25k", "5k_improver", "10k_builder", "hiit"), TrainingCatalog.all.map { it.key })
    }

    @Test
    fun `each plan has 3 workouts per week across its full duration`() {
        val expectedWeeks = mapOf(
            "c25k" to 8,
            "5k_improver" to 6,
            "10k_builder" to 8,
            "hiit" to 4
        )
        TrainingCatalog.all.forEach { plan ->
            val weeks = expectedWeeks[plan.key] ?: 0
            assertEquals(weeks, plan.weeks.size)
            assertEquals(weeks * 3, plan.totalWorkouts)
            assertEquals(7, plan.weeks[0].days.size) // Mon..Sun rows
        }
    }

    @Test
    fun `workout day ids are unique across each plan`() {
        TrainingCatalog.all.forEach { plan ->
            val ids = plan.allDays.map { it.id }
            assertEquals(ids.size, ids.toSet().size)
            ids.forEach { assertTrue(it.matches(Regex("w\\dd\\d"))) }
        }
    }

    @Test
    fun `workout days always carry a real interval or goal plan`() {
        TrainingCatalog.all.forEach { plan ->
            plan.workoutDays.forEach { day ->
                assertFalse(day.isRest)
                assertNotNull(TrainingCatalog.byKey(plan.key)?.dayById(day.id))
                val planDef = day.plan
                assertTrue(planDef.mode == WorkoutMode.INTERVAL || planDef.mode == WorkoutMode.GOAL)
                when (planDef.mode) {
                    WorkoutMode.INTERVAL -> {
                        assertTrue(planDef.workSeconds >= 30)
                        assertTrue(planDef.recoverySeconds >= 30)
                        assertTrue(planDef.repetitions >= 4)
                    }
                    WorkoutMode.GOAL -> assertTrue(planDef.goalValue > 0.0)
                    else -> {}
                }
            }
        }
    }

    @Test
    fun `every plan day survives the encode-decode round trip`() {
        TrainingCatalog.all.forEach { plan ->
            plan.workoutDays.forEach { day ->
                val decoded = WorkoutPlan.decode(day.plan.encode())
                assertEquals(day.plan.mode, decoded.mode)
                assertEquals(day.plan.workSeconds, decoded.workSeconds)
                assertEquals(day.plan.recoverySeconds, decoded.recoverySeconds)
                assertEquals(day.plan.repetitions, decoded.repetitions)
            }
        }
    }

    @Test
    fun `c25k starts easy and ends long`() {
        val c25k = TrainingCatalog.byKey("c25k")!!
        val first = c25k.weeks[0].days.first { !it.isRest }
        val last = c25k.weeks[7].days.first { !it.isRest }
        assertTrue(first.plan.workSeconds < last.plan.workSeconds)
    }

    @Test
    fun `rest days are marked and never have an active plan`() {
        TrainingCatalog.all.forEach { plan ->
            plan.allDays.filter { it.isRest }.forEach { rest ->
                assertTrue(rest.plan.mode == WorkoutMode.FREE)
                assertEquals(0, rest.plan.repetitions)
            }
        }
    }
}