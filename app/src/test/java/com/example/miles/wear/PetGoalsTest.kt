package com.example.miles.wear

import com.example.miles.wear.data.model.PetType
import com.example.miles.wear.data.model.WeeklyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fitness pet + weekly-goals model behavior. */
class PetGoalsTest {

    @Test
    fun `pet type from name resolves valid pets`() {
        assertEquals(PetType.DOG, PetType.fromName("DOG"))
        assertEquals(PetType.CAT, PetType.fromName("CAT"))
        assertEquals(PetType.PARROT, PetType.fromName("PARROT"))
        assertEquals(PetType.BUNNY, PetType.fromName("BUNNY"))
    }

    @Test
    fun `unknown pet type falls back to dog`() {
        assertEquals(PetType.DOG, PetType.fromName("MONKEY"))
        assertEquals(PetType.DOG, PetType.fromName(null))
        assertEquals(PetType.DOG, PetType.fromName(""))
    }

    @Test
    fun `all four adoptable pets have emoji and treat names`() {
        assertEquals(4, PetType.entries.size)
        PetType.entries.forEach { pet ->
            assertTrue(pet.emoji.isNotEmpty())
            assertTrue(pet.label.isNotEmpty())
            assertTrue(pet.treatName.isNotEmpty())
        }
    }

    @Test
    fun `weekly progress defaults to zero goals`() {
        val w = WeeklyProgress()
        assertEquals(0.0, w.distanceKm, 0.0)
        assertEquals(0, w.activeMinutes)
        assertEquals(0.0, w.distanceGoalKm, 0.0)
        assertEquals(0, w.minutesGoal)
    }
}