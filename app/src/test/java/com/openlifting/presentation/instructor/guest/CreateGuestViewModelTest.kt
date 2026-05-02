package com.openlifting.presentation.instructor.guest

import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.UserEntity
import com.openlifting.domain.model.Sex
import com.openlifting.domain.repository.CoachRepository
import com.openlifting.domain.repository.GuestCreated
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGuestViewModelTest {

    private val userDao         = mockk<UserDao>()
    private val coachRepository = mockk<CoachRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { userDao.getLoggedInUser() } returns UserEntity(
            id = 99L, email = "coach@x.com", name = "Coach Coach", role = "INSTRUCTOR"
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun build() = CreateGuestViewModel(userDao, coachRepository)

    @Test
    fun `draft is invalid when fields are blank`() {
        val vm = build()
        assertFalse(vm.draft.value.isValid)
    }

    @Test
    fun `draft becomes valid with full data`() {
        val vm = build()
        vm.setFirstName("Juan")
        vm.setLastName("Perez")
        vm.setBodyweight("80")
        vm.setAge("28")
        assertTrue(vm.draft.value.isValid)
    }

    @Test
    fun `bodyweight outside 30-250 range is rejected`() {
        val vm = build()
        vm.setFirstName("Juan"); vm.setLastName("Perez"); vm.setAge("28")
        vm.setBodyweight("25")  ; assertFalse(vm.draft.value.isValid)
        vm.setBodyweight("260") ; assertFalse(vm.draft.value.isValid)
        vm.setBodyweight("70")  ; assertTrue (vm.draft.value.isValid)
    }

    @Test
    fun `createGuest calls repository and invokes callback with new profile id`() = runTest {
        coEvery {
            coachRepository.createGuest(
                instructorUserId = 99L,
                firstName        = "Juan",
                lastName         = "Perez",
                bodyweightKg     = 80f,
                ageYears         = 28,
                sex              = Sex.MALE
            )
        } returns GuestCreated(athleteUserId = 10L, athleteProfileId = 42L)

        val vm = build()
        vm.setFirstName("Juan"); vm.setLastName("Perez")
        vm.setBodyweight("80"); vm.setAge("28")
        vm.setSex(Sex.MALE)

        var receivedProfileId: Long? = null
        vm.createGuest { receivedProfileId = it }
        advanceUntilIdle()

        assertEquals(42L, receivedProfileId)
        assertFalse(vm.isSaving.value)
        coVerify(exactly = 1) {
            coachRepository.createGuest(99L, "Juan", "Perez", 80f, 28, Sex.MALE)
        }
    }

    @Test
    fun `createGuest is no-op when draft is invalid`() = runTest {
        val vm = build()
        // Don't fill the form

        var receivedProfileId: Long? = null
        vm.createGuest { receivedProfileId = it }
        advanceUntilIdle()

        assertNull(receivedProfileId)
        coVerify(exactly = 0) {
            coachRepository.createGuest(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `createGuest trims whitespace before saving`() = runTest {
        coEvery {
            coachRepository.createGuest(any(), "Juan", "Perez", any(), any(), any())
        } returns GuestCreated(athleteUserId = 10L, athleteProfileId = 42L)

        val vm = build()
        vm.setFirstName("  Juan  ")
        vm.setLastName(" Perez ")
        vm.setBodyweight("80"); vm.setAge("28")

        vm.createGuest { /* ignore */ }
        advanceUntilIdle()

        coVerify { coachRepository.createGuest(any(), "Juan", "Perez", any(), any(), any()) }
    }
}
