package com.openlifting.data.repository

import androidx.room.withTransaction
import com.openlifting.data.local.OpenLiftingDatabase
import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.remote.api.VortexInstructorApi
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.local.entity.TrainingSetEntity
import com.openlifting.data.remote.api.VortexSessionApi
import com.openlifting.data.remote.dto.CreateSessionRequest
import com.openlifting.data.remote.dto.EndSessionRequest
import com.openlifting.data.remote.dto.PaginatedSessions
import com.openlifting.data.remote.dto.PatchSessionRequest
import com.openlifting.data.remote.dto.PostSetRequest
import com.openlifting.data.remote.dto.PostSetResponse
import com.openlifting.data.remote.dto.TrainingSessionDto
import com.openlifting.domain.model.DeviceSource
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
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
    private val athleteProfileDao = mockk<AthleteProfileDao>(relaxed = true)
    private val sessionApi = mockk<VortexSessionApi>()
    private val instructorApi = mockk<VortexInstructorApi>(relaxed = true)

    private fun build() = SessionRepositoryImpl(
        db, sessionDao, setDao, athleteProfileDao, sessionApi, instructorApi
    )

    /**
     * `db.withTransaction { ... }` is a suspend extension defined in `androidx.room`.
     * mockk(relaxed=true) returns a default value but never invokes the block, which
     * leaves the calling coroutine suspended (UncompletedCoroutinesError). We mock
     * the static so the block runs synchronously inside the test scheduler.
     */
    @Before
    fun setUpRoomTransactionStub() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            secondArg<suspend () -> Any?>().invoke()
        }
        // Default: athlete is not a guest. Tests that exercise the guest branch override this.
        coEvery { athleteProfileDao.getByUserId(any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

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

    // ── updateSessionDeviceSource ─────────────────────────────────────────────

    @Test
    fun `updateSessionDeviceSource patches Vortex and Room when source changes`() = runTest {
        coEvery { sessionDao.getById(1L) } returns TrainingSessionEntity(
            localId = 1L, serverId = 12L, athleteUserId = 7L,
            deviceSource = "SIMULATED", synced = true
        )
        coEvery { sessionApi.patchSession(eq(12L), any()) } returns
            Response.success(remoteSession())
        val updated = slot<TrainingSessionEntity>()
        coEvery { sessionDao.update(capture(updated)) } returns Unit

        build().updateSessionDeviceSource(1L, DeviceSource.REAL)

        assertEquals("REAL", updated.captured.deviceSource)
        assertTrue(updated.captured.synced)
        coVerify(exactly = 1) { sessionApi.patchSession(eq(12L), any<PatchSessionRequest>()) }
    }

    @Test
    fun `updateSessionDeviceSource is a no-op when source already matches`() = runTest {
        coEvery { sessionDao.getById(1L) } returns TrainingSessionEntity(
            localId = 1L, serverId = 12L, athleteUserId = 7L,
            deviceSource = "REAL", synced = true
        )

        build().updateSessionDeviceSource(1L, DeviceSource.REAL)

        coVerify(exactly = 0) { sessionApi.patchSession(any(), any()) }
        coVerify(exactly = 0) { sessionDao.update(any()) }
    }

    @Test
    fun `updateSessionDeviceSource on local-only session updates Room without backend`() = runTest {
        coEvery { sessionDao.getById(1L) } returns TrainingSessionEntity(
            localId = 1L, serverId = null, athleteUserId = 7L,
            deviceSource = "SIMULATED", synced = false
        )
        val updated = slot<TrainingSessionEntity>()
        coEvery { sessionDao.update(capture(updated)) } returns Unit

        build().updateSessionDeviceSource(1L, DeviceSource.REAL)

        assertEquals("REAL", updated.captured.deviceSource)
        assertFalse(updated.captured.synced)
        coVerify(exactly = 0) { sessionApi.patchSession(any(), any()) }
    }

    @Test
    fun `updateSessionDeviceSource marks unsynced when PATCH fails`() = runTest {
        coEvery { sessionDao.getById(1L) } returns TrainingSessionEntity(
            localId = 1L, serverId = 12L, athleteUserId = 7L,
            deviceSource = "SIMULATED", synced = true
        )
        coEvery { sessionApi.patchSession(eq(12L), any()) } throws IOException("offline")
        val updated = slot<TrainingSessionEntity>()
        coEvery { sessionDao.update(capture(updated)) } returns Unit

        build().updateSessionDeviceSource(1L, DeviceSource.REAL)

        assertEquals("REAL", updated.captured.deviceSource)
        assertFalse(updated.captured.synced)
    }

    @Test
    fun `updateSessionDeviceSource on missing session is a no-op`() = runTest {
        coEvery { sessionDao.getById(999L) } returns null

        build().updateSessionDeviceSource(999L, DeviceSource.REAL)

        coVerify(exactly = 0) { sessionApi.patchSession(any(), any()) }
        coVerify(exactly = 0) { sessionDao.update(any()) }
    }

    // ── saveSetWithDetails (POST /sets path) ──────────────────────────────────
    //
    // `db.withTransaction { ... }` is a suspend extension that mockk(relaxed=true)
    // turns into a no-op (returns 0 for Long). That means the localId of the
    // inserted set is observed as 0 inside the SUT — which is fine for asserting
    // network behavior; we just verify `markSynced(serverId = ...)` was called
    // with the response id, ignoring the localId.

    private fun stubSession(serverId: Long?) {
        coEvery { sessionDao.getById(1L) } returns TrainingSessionEntity(
            localId = 1L, serverId = serverId, athleteUserId = 7L
        )
    }

    private fun fakeMetrics(setLocalId: Long = 0L) = SetMetrics(
        setLocalId = setLocalId,
        bsaVlPct = 32.5f, bsaVmPct = 28.5f, bsaGmaxPct = 25.0f, bsaEsPct = 14.0f,
        hqRatio = 0.45f, esGmaxRatio = 0.62f, intraSetFatigueRatio = 0.18f,
        thresholdsVersion = 1
    )

    private fun fakeActivations(): List<List<MuscleActivation>> = listOf(
        listOf(
            MuscleActivation(repId = 0L, muscle = Muscle.VASTUS_LATERALIS, side = MuscleSide.LEFT,
                percentMvc = 87.2f, peakPercentMvc = 112.4f),
            MuscleActivation(repId = 0L, muscle = Muscle.VASTUS_LATERALIS, side = MuscleSide.RIGHT,
                percentMvc = 85.0f, peakPercentMvc = 108.0f)
        )
    )

    @Test
    fun `saveSetWithDetails skips POST when session has no serverId`() = runTest {
        stubSession(serverId = null)

        build().saveSetWithDetails(
            sessionLocalId   = 1L,
            setNumber        = 1,
            loadKg           = 140f,
            targetReps       = 5,
            variant          = SquatVariant.LOW_BAR,
            depth            = SquatDepth.PARALLEL,
            rpe              = 8.5f,
            activationsByRep = fakeActivations(),
            metrics          = fakeMetrics(),
            recommendations  = emptyList()
        )

        coVerify(exactly = 0) { sessionApi.postSet(any(), any()) }
        coVerify(exactly = 0) { setDao.markSynced(any(), any()) }
    }

    @Test
    fun `saveSetWithDetails posts to Vortex and marks synced when remote ok`() = runTest {
        stubSession(serverId = 12L)
        coEvery { sessionApi.postSet(eq(12L), any()) } returns
            Response.success(PostSetResponse(id = 42L, sessionId = 12L, setNumber = 1))

        build().saveSetWithDetails(
            sessionLocalId   = 1L,
            setNumber        = 1,
            loadKg           = 140f,
            targetReps       = 5,
            variant          = SquatVariant.LOW_BAR,
            depth            = SquatDepth.PARALLEL,
            rpe              = 8.5f,
            activationsByRep = fakeActivations(),
            metrics          = fakeMetrics(),
            recommendations  = listOf(
                Recommendation(setLocalId = 0L, text = "Test", severity = RiskLevel.MONITOR, evidence = "x")
            )
        )

        val req = slot<PostSetRequest>()
        coVerify(exactly = 1) { sessionApi.postSet(eq(12L), capture(req)) }
        assertEquals(1, req.captured.setNumber)
        assertEquals(140.0, req.captured.loadKg, 0.01)
        assertEquals("LOW_BAR", req.captured.variant)
        assertEquals(1, req.captured.reps.size)
        coVerify(exactly = 1) { setDao.markSynced(any(), serverId = 42L) }
    }

    @Test
    fun `saveSetWithDetails leaves set unsynced when POST fails`() = runTest {
        stubSession(serverId = 12L)
        coEvery { sessionApi.postSet(eq(12L), any()) } throws IOException("offline")

        build().saveSetWithDetails(
            sessionLocalId   = 1L,
            setNumber        = 1,
            loadKg           = 140f,
            targetReps       = 5,
            variant          = SquatVariant.LOW_BAR,
            depth            = SquatDepth.PARALLEL,
            rpe              = 8.5f,
            activationsByRep = fakeActivations(),
            metrics          = fakeMetrics(),
            recommendations  = emptyList()
        )

        coVerify(exactly = 0) { setDao.markSynced(any(), any()) }
    }

    @Test
    fun `saveSetWithDetails leaves set unsynced when POST returns 5xx`() = runTest {
        stubSession(serverId = 12L)
        coEvery { sessionApi.postSet(eq(12L), any()) } returns
            Response.error(500, "".toResponseBody("application/json".toMediaTypeOrNull()))

        build().saveSetWithDetails(
            sessionLocalId   = 1L,
            setNumber        = 1,
            loadKg           = 140f,
            targetReps       = 5,
            variant          = SquatVariant.LOW_BAR,
            depth            = SquatDepth.PARALLEL,
            rpe              = 8.5f,
            activationsByRep = fakeActivations(),
            metrics          = fakeMetrics(),
            recommendations  = emptyList()
        )

        coVerify(exactly = 0) { setDao.markSynced(any(), any()) }
    }
}
