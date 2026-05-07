package com.openlifting.presentation.athlete.session

import app.cash.turbine.test
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.RecommendationEntity
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.local.entity.TrainingSetEntity
import com.openlifting.domain.datasource.EmgDataSource
import com.openlifting.domain.model.EmgEvent
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.repository.SessionRepository
import com.openlifting.domain.usecase.metrics.ComputeSetMetrics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val emgDataSource     = mockk<EmgDataSource>()
    private val computeMetrics    = mockk<ComputeSetMetrics>()
    private val userDao           = mockk<UserDao>()
    private val setDao            = mockk<SetDao>()
    private val sessionDao        = mockk<SessionDao>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun build(
        savedStateHandle: androidx.lifecycle.SavedStateHandle = androidx.lifecycle.SavedStateHandle()
    ) = SessionViewModel(
        savedStateHandle, sessionRepository, emgDataSource, computeMetrics, userDao, setDao, sessionDao
    )

    private fun set(localId: Long, setNumber: Int, loadKg: Float, targetReps: Int = 5) =
        TrainingSetEntity(
            localId = localId, sessionLocalId = 1L, setNumber = setNumber,
            loadKg = loadKg, targetReps = targetReps,
            variant = "LOW_BAR", depth = "PARALLEL", rpe = 7f
        )

    private fun metrics(setId: Long, bsaVl: Float = 8f, bsaVm: Float = 8f, bsaGmax: Float = 8f, bsaEs: Float = 8f) =
        SetMetricsEntity(
            setLocalId = setId,
            bsaVlPct = bsaVl, bsaVmPct = bsaVm, bsaGmaxPct = bsaGmax, bsaEsPct = bsaEs,
            esGmaxRatio = 1.3f, hqRatio = 0.65f, intraSetFatigueRatio = 1.05f
        )

    /**
     * Returns a stream that emits a single SetComplete event with the given activations,
     * skipping the live phase events. Sufficient for tests that exercise the post-stream
     * persistence pipeline.
     */
    private fun stubSetCompleteFlow(
        setId: String,
        targetReps: Int,
        activationsByRep: List<List<MuscleActivation>> = emptyList()
    ) = flowOf<EmgEvent>(
        EmgEvent.SetComplete(
            setId             = setId,
            totalReps         = targetReps,
            activationsByRep  = activationsByRep
        )
    )

    @Test
    fun `finalizeSession with no started session invokes onSkipped without writing to repo`() = runTest {
        val vm = build()
        var skipped = false
        vm.finalizeSession { skipped = true }
        advanceUntilIdle()
        assertTrue(skipped)
        coVerify(exactly = 0) { sessionRepository.endSession(any()) }
    }

    @Test
    fun `exitSummary resets state and invokes callback`() = runTest {
        val vm = build()
        var done = false
        vm.exitSummary { done = true }
        advanceUntilIdle()
        assertTrue(done)
        // After reset, currentSetNumber is 1 again
        assertEquals(1, vm.currentSetNumber())
        assertEquals(SessionUiState.MetadataEntry, vm.uiState.value)
    }

    @Test
    fun `finalizeSession aggregates totals and overall risk across sets`() = runTest {
        // Mocks for measureSet
        val user = com.openlifting.data.local.entity.UserEntity(
            id = 1L, email = "x@x.com", name = "Emilio", role = "ATHLETE"
        )
        coEvery { userDao.getLoggedInUser() } returns user
        coEvery { sessionRepository.createSession(any(), any()) } returns 1L
        // Stub the EMG stream to emit a single SetComplete carrying empty activations.
        // (SetComplete is the only event the persistence pipeline cares about; live events
        // only drive UI updates that are not exercised here.)
        every { emgDataSource.streamSet(any()) } answers {
            val req = arg<com.openlifting.domain.datasource.StartSetRequest>(0)
            stubSetCompleteFlow(req.setRequestId, req.targetReps)
        }
        coEvery { computeMetrics(any(), any()) } returns
            ComputeSetMetrics.Result(
                metrics = com.openlifting.domain.model.SetMetrics(
                    setLocalId = 0L,
                    bsaVlPct = 8f, bsaVmPct = 8f, bsaGmaxPct = 8f, bsaEsPct = 8f,
                    hqRatio = 0.65f, esGmaxRatio = 1.3f, intraSetFatigueRatio = 1.05f
                ),
                recommendations = emptyList()
            )
        coEvery { sessionRepository.saveSetWithDetails(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns 100L

        // Mocks for finalizeSession
        val sets = listOf(
            set(localId = 100L, setNumber = 1, loadKg = 100f, targetReps = 5),
            set(localId = 101L, setNumber = 2, loadKg = 110f, targetReps = 5),
            set(localId = 102L, setNumber = 3, loadKg = 120f, targetReps = 3)
        )
        coEvery { setDao.getSetsForSession(1L) } returns sets
        coEvery { setDao.getMetricsForSet(100L) } returns metrics(100L, bsaVl = 5f)            // NORMAL
        coEvery { setDao.getMetricsForSet(101L) } returns metrics(101L, bsaVl = 12f)           // MONITOR
        coEvery { setDao.getMetricsForSet(102L) } returns metrics(102L, bsaVl = 18f)           // RISK
        coEvery { setDao.getRecommendationsForSet(any()) } returns emptyList()
        coEvery { sessionDao.getById(1L) } returns TrainingSessionEntity(
            localId = 1L, athleteUserId = 1L, startedAt = 0L, endedAt = 18 * 60_000L
        )

        val vm = build()

        // Trigger one set so sessionLocalId becomes 1L
        vm.measureSet(
            loadKg = 100f, targetReps = 5,
            variant = com.openlifting.domain.model.SquatVariant.LOW_BAR,
            depth   = com.openlifting.domain.model.SquatDepth.PARALLEL,
            rpe     = 7f
        )
        advanceUntilIdle()

        vm.uiState.test {
            // Skip whatever state we are in first
            skipItems(0)

            vm.finalizeSession { /* should NOT be invoked */ }
            advanceUntilIdle()

            // Walk states until we reach SessionSummary
            var summary: SessionUiState.SessionSummary? = null
            while (summary == null) {
                val s = expectMostRecentItem()
                if (s is SessionUiState.SessionSummary) summary = s else cancelAndIgnoreRemainingEvents().also { return@test }
            }

            assertEquals(3, summary.totalSets)
            // Volume = 100*5 + 110*5 + 120*3 = 500 + 550 + 360 = 1410
            assertEquals(1410f, summary.totalVolumeKg, 0.01f)
            assertEquals(120f,  summary.maxLoadKg, 0.01f)
            assertEquals(RiskLevel.RISK, summary.overallRisk)  // bsaVl=18 in set 3

            val recap = summary.sets.sortedBy { it.setNumber }
            assertEquals(RiskLevel.NORMAL,  recap[0].overallRisk)
            assertEquals(RiskLevel.MONITOR, recap[1].overallRisk)
            assertEquals(RiskLevel.RISK,    recap[2].overallRisk)
            assertEquals(18f, recap[2].bsaWorstPct, 0.01f)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify { sessionRepository.endSession(1L) }
    }

    @Test
    fun `top recommendations are ordered by severity and deduplicated by text`() = runTest {
        // Same setup as above, but with recommendations attached
        val user = com.openlifting.data.local.entity.UserEntity(
            id = 1L, email = "x@x.com", name = "Emilio", role = "ATHLETE"
        )
        coEvery { userDao.getLoggedInUser() } returns user
        coEvery { sessionRepository.createSession(any(), any()) } returns 1L
        every { emgDataSource.streamSet(any()) } answers {
            val req = arg<com.openlifting.domain.datasource.StartSetRequest>(0)
            stubSetCompleteFlow(req.setRequestId, req.targetReps)
        }
        coEvery { computeMetrics(any(), any()) } returns
            ComputeSetMetrics.Result(
                metrics = com.openlifting.domain.model.SetMetrics(
                    setLocalId = 0L,
                    bsaVlPct = 8f, bsaVmPct = 8f, bsaGmaxPct = 8f, bsaEsPct = 8f,
                    hqRatio = 0.65f, esGmaxRatio = 1.3f, intraSetFatigueRatio = 1.05f
                ),
                recommendations = emptyList()
            )
        coEvery { sessionRepository.saveSetWithDetails(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns 100L

        val sets = listOf(set(localId = 100L, setNumber = 1, loadKg = 100f))
        coEvery { setDao.getSetsForSession(1L) } returns sets
        coEvery { setDao.getMetricsForSet(100L) } returns metrics(100L)
        coEvery { setDao.getRecommendationsForSet(100L) } returns listOf(
            RecommendationEntity(id = 1L, setLocalId = 100L, text = "duplicate text", severity = "RISK"),
            RecommendationEntity(id = 2L, setLocalId = 100L, text = "duplicate text", severity = "RISK"),
            RecommendationEntity(id = 3L, setLocalId = 100L, text = "monitor advice",  severity = "MONITOR"),
            RecommendationEntity(id = 4L, setLocalId = 100L, text = "info advice",     severity = "NORMAL"),
            RecommendationEntity(id = 5L, setLocalId = 100L, text = "another risk",    severity = "RISK")
        )
        coEvery { sessionDao.getById(1L) } returns TrainingSessionEntity(
            localId = 1L, athleteUserId = 1L, startedAt = 0L, endedAt = 60_000L
        )

        val vm = build()
        vm.measureSet(
            loadKg = 100f, targetReps = 5,
            variant = com.openlifting.domain.model.SquatVariant.LOW_BAR,
            depth   = com.openlifting.domain.model.SquatDepth.PARALLEL,
            rpe     = 7f
        )
        advanceUntilIdle()
        vm.finalizeSession { /* not invoked */ }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("expected SessionSummary, got $state", state is SessionUiState.SessionSummary)
        val recs = (state as SessionUiState.SessionSummary).topRecommendations

        assertEquals(3, recs.size)                          // capped at 3
        assertEquals(RiskLevel.RISK, recs[0].severity)      // RISK first
        assertEquals(RiskLevel.RISK, recs[1].severity)
        assertEquals(RiskLevel.MONITOR, recs[2].severity)   // MONITOR third
        assertEquals(setOf("duplicate text", "another risk", "monitor advice"), recs.map { it.text }.toSet())
    }
}
