package com.openlifting.data.repository

import com.openlifting.data.local.OpenLiftingDatabase
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.remote.api.VortexSessionApi
import com.openlifting.data.remote.dto.CreateSessionRequest
import com.openlifting.data.remote.dto.EndSessionRequest
import com.openlifting.data.remote.dto.PaginatedSessions
import com.openlifting.data.remote.dto.TrainingSessionDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SessionRepositoryImplTest {

    private val db        = mockk<OpenLiftingDatabase>(relaxed = true)
    private val sessionDao = mockk<SessionDao>(relaxed = true)
    private val setDao    = mockk<SetDao>(relaxed = true)
    private val sessionApi = mockk<VortexSessionApi>()

    private fun build() = SessionRepositoryImpl(db, sessionDao, setDao, sessionApi)

    private fun remoteSession(
        id: Long = 12L,
        endedAt: String? = null
    ) = TrainingSessionDto(
        id = id,
        exercise = "back_squat",
        startedAt = "2026-05-10T15:30:00Z",
        endedAt = endedAt,
        deviceSource = "SIMULATED",
        createdAt = "2026-05-10T15:30:00Z"
    )

    // ── createSession ─────────────────────────────────────────────────────────

    @Test
    fun `createSession online persists entity with serverId and synced=true`() = runTest {
        coEvery { sessionApi.createSession(any()) } returns Response.success(remoteSession(id = 12L))
        val captured = slot<TrainingSessionEntity>()
        coEvery { sessionDao.insert(capture(captured)) } returns 1L

        val localId = build().createSession(athleteUserId = 7L)

        assertEquals(1L, localId)
        assertEquals(12L, captured.captured.serverId)
        assertEquals(7L, captured.captured.athleteUserId)
        assertTrue(captured.captured.synced)
        coVerify(exactly = 1) { sessionApi.createSession(any<CreateSessionRequest>()) }
    }

    @Test
    fun `createSession offline persists entity with null serverId and synced=false`() = runTest {
        coEvery { sessionApi.createSession(any()) } throws IOException("offline")
        val captured = slot<TrainingSessionEntity>()
        coEvery { sessionDao.insert(capture(captured)) } returns 2L

        val localId = build().createSession(athleteUserId = 7L)

        assertEquals(2L, localId)
        assertNull(captured.captured.serverId)
        assertFalse(captured.captured.synced)
    }

    @Test
    fun `createSession with backend 5xx falls back to local-only row`() = runTest {
        coEvery { sessionApi.createSession(any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaTypeOrNull()))
        val captured = slot<TrainingSessionEntity>()
        coEvery { sessionDao.insert(capture(captured)) } returns 3L

        build().createSession(athleteUserId = 7L)

        assertNull(captured.captured.serverId)
        assertFalse(captured.captured.synced)
    }

    @Test
    fun `createSession passes instructorUserId through to entity`() = runTest {
        coEvery { sessionApi.createSession(any()) } returns Response.success(remoteSession())
        val captured = slot<TrainingSessionEntity>()
        coEvery { sessionDao.insert(capture(captured)) } returns 1L

        build().createSession(athleteUserId = 7L, instructorUserId = 99L)

        assertEquals(99L, captured.captured.instructorUserId)
    }

    // ── endSession ────────────────────────────────────────────────────────────

    @Test
    fun `endSession with serverId calls PUT and stays synced when remote ok`() = runTest {
        coEvery { sessionDao.getById(1L) } returns TrainingSessionEntity(
            localId = 1L, serverId = 12L, athleteUserId = 7L, synced = true
        )
        coEvery { sessionApi.endSession(eq(12L), any()) } returns Response.success(remoteSession(endedAt = "2026-05-10T16:00:00Z"))
        val updated = slot<TrainingSessionEntity>()
        coEvery { sessionDao.update(capture(updated)) } returns Unit

        build().endSession(1L)

        assertTrue(updated.captured.endedAt!! > 0L)
        assertTrue(updated.captured.synced)
        coVerify(exactly = 1) { sessionApi.endSession(eq(12L), any<EndSessionRequest>()) }
    }

    @Test
    fun `endSession on local-only session updates Room without calling backend`() = runTest {
        coEvery { sessionDao.getById(2L) } returns TrainingSessionEntity(
            localId = 2L, serverId = null, athleteUserId = 7L, synced = false
        )
        val updated = slot<TrainingSessionEntity>()
        coEvery { sessionDao.update(capture(updated)) } returns Unit

        build().endSession(2L)

        assertTrue(updated.captured.endedAt!! > 0L)
        assertFalse(updated.captured.synced)
        coVerify(exactly = 0) { sessionApi.endSession(any(), any()) }
    }

    @Test
    fun `endSession with PUT failure marks row as unsynced`() = runTest {
        coEvery { sessionDao.getById(1L) } returns TrainingSessionEntity(
            localId = 1L, serverId = 12L, athleteUserId = 7L, synced = true
        )
        coEvery { sessionApi.endSession(eq(12L), any()) } throws IOException("offline")
        val updated = slot<TrainingSessionEntity>()
        coEvery { sessionDao.update(capture(updated)) } returns Unit

        build().endSession(1L)

        assertTrue(updated.captured.endedAt!! > 0L)
        assertFalse(updated.captured.synced)
    }

    @Test
    fun `endSession on missing local row is a no-op`() = runTest {
        coEvery { sessionDao.getById(999L) } returns null

        build().endSession(999L)

        coVerify(exactly = 0) { sessionApi.endSession(any(), any()) }
        coVerify(exactly = 0) { sessionDao.update(any()) }
    }

    // ── syncSessionsFromBackend ───────────────────────────────────────────────

    @Test
    fun `syncSessionsFromBackend inserts new remote sessions`() = runTest {
        coEvery { sessionApi.listSessions(any()) } returns Response.success(
            PaginatedSessions(data = listOf(remoteSession(id = 12L), remoteSession(id = 13L)), meta = null)
        )
        coEvery { sessionDao.getByServerId(12L) } returns null
        coEvery { sessionDao.getByServerId(13L) } returns null
        coEvery { sessionDao.insert(any()) } returns 0L

        val count = build().syncSessionsFromBackend(athleteUserId = 7L)

        assertEquals(2, count)
        coVerify(exactly = 2) { sessionDao.insert(any()) }
        coVerify(exactly = 0) { sessionDao.update(any()) }
    }

    @Test
    fun `syncSessionsFromBackend updates already-mirrored sessions in place`() = runTest {
        coEvery { sessionApi.listSessions(any()) } returns Response.success(
            PaginatedSessions(data = listOf(remoteSession(id = 12L, endedAt = "2026-05-10T16:00:00Z")), meta = null)
        )
        coEvery { sessionDao.getByServerId(12L) } returns TrainingSessionEntity(
            localId = 5L, serverId = 12L, athleteUserId = 7L, synced = true
        )
        val updated = slot<TrainingSessionEntity>()
        coEvery { sessionDao.update(capture(updated)) } returns Unit

        build().syncSessionsFromBackend(athleteUserId = 7L)

        assertEquals(5L, updated.captured.localId)
        assertEquals(12L, updated.captured.serverId)
        assertTrue(updated.captured.synced)
        coVerify(exactly = 0) { sessionDao.insert(any()) }
    }

    @Test
    fun `syncSessionsFromBackend returns null when network fails`() = runTest {
        coEvery { sessionApi.listSessions(any()) } throws IOException("offline")

        val count = build().syncSessionsFromBackend(athleteUserId = 7L)

        assertNull(count)
        coVerify(exactly = 0) { sessionDao.insert(any()) }
    }

    @Test
    fun `syncSessionsFromBackend returns null on 5xx and writes nothing`() = runTest {
        coEvery { sessionApi.listSessions(any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaTypeOrNull()))

        val count = build().syncSessionsFromBackend(athleteUserId = 7L)

        assertNull(count)
        coVerify(exactly = 0) { sessionDao.insert(any()) }
    }

    @Test
    fun `syncSessionsFromBackend with empty page returns 0`() = runTest {
        coEvery { sessionApi.listSessions(any()) } returns Response.success(
            PaginatedSessions(data = emptyList(), meta = null)
        )

        val count = build().syncSessionsFromBackend(athleteUserId = 7L)

        assertEquals(0, count)
    }
}
