package com.openlifting.presentation.athlete.home

import app.cash.turbine.test
import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.local.entity.TrainingSetEntity
import com.openlifting.data.local.entity.UserEntity
import com.openlifting.domain.model.RiskLevel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AthleteHomeViewModelTest {

    private val userDao            = mockk<UserDao>()
    private val athleteProfileDao  = mockk<AthleteProfileDao>()
    private val sessionDao         = mockk<SessionDao>()
    private val setDao             = mockk<SetDao>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun userEntity(id: Long = 1L, name: String = "Emilio Giordano") =
        UserEntity(id = id, email = "test@x.com", name = name, role = "ATHLETE")

    private fun profile(userId: Long, calibratedAt: Long? = null, firstName: String = "Emilio") =
        AthleteProfileEntity(
            id = 1L, userId = userId, firstName = firstName, lastName = "Giordano",
            bodyweightKg = 80f, ageYears = 30, sex = "MALE", calibratedAt = calibratedAt
        )

    private fun session(id: Long, startedAt: Long, athleteUserId: Long = 1L) =
        TrainingSessionEntity(
            localId = id, athleteUserId = athleteUserId, startedAt = startedAt,
            endedAt = startedAt + 18 * 60_000L
        )

    private fun set(localId: Long, sessionId: Long, setNumber: Int, loadKg: Float) =
        TrainingSetEntity(
            localId = localId, sessionLocalId = sessionId, setNumber = setNumber,
            loadKg = loadKg, targetReps = 5, variant = "LOW_BAR", depth = "PARALLEL", rpe = 7f
        )

    private fun metrics(
        setLocalId: Long, bsaVl: Float = 5f, bsaVm: Float = 5f, bsaGmax: Float = 5f, bsaEs: Float = 5f,
        esGmax: Float = 1.2f, hq: Float = 0.65f, fatigue: Float = 1.05f
    ) = SetMetricsEntity(
        setLocalId = setLocalId,
        bsaVlPct = bsaVl, bsaVmPct = bsaVm, bsaGmaxPct = bsaGmax, bsaEsPct = bsaEs,
        esGmaxRatio = esGmax, hqRatio = hq, intraSetFatigueRatio = fatigue
    )

    private fun build(): AthleteHomeViewModel =
        AthleteHomeViewModel(userDao, athleteProfileDao, sessionDao, setDao)

    // ── Tests ───────────────────────────────────────────────────────────────

    @Test
    fun `emits Empty state when athlete has no sessions`() = runTest {
        val u = userEntity()
        coEvery { userDao.getLoggedInUser() } returns u
        coEvery { athleteProfileDao.getByUserId(u.id) } returns profile(u.id)
        every { sessionDao.observeForAthlete(u.id) } returns MutableStateFlow(emptyList())

        build().uiState.test {
            // Skip Loading
            val first = awaitItem()
            val state = if (first is AthleteHomeUiState.Loading) awaitItem() else first
            assertTrue("expected Empty, got $state", state is AthleteHomeUiState.Empty)
            assertEquals("Emilio", (state as AthleteHomeUiState.Empty).athleteFirstName)
            assertEquals(false, state.mvcCalibrated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Empty state reflects MVC calibrated when calibratedAt is set`() = runTest {
        val u = userEntity()
        coEvery { userDao.getLoggedInUser() } returns u
        coEvery { athleteProfileDao.getByUserId(u.id) } returns profile(u.id, calibratedAt = 12345L)
        every { sessionDao.observeForAthlete(u.id) } returns MutableStateFlow(emptyList())

        build().uiState.test {
            val first = awaitItem()
            val state = if (first is AthleteHomeUiState.Loading) awaitItem() else first
            assertTrue(state is AthleteHomeUiState.Empty)
            assertEquals(true, (state as AthleteHomeUiState.Empty).mvcCalibrated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Loaded state with one session has null deltas`() = runTest {
        val u = userEntity()
        val s = session(id = 10L, startedAt = 1_000_000L)
        val ts = set(localId = 100L, sessionId = s.localId, setNumber = 1, loadKg = 100f)
        val tm = metrics(setLocalId = ts.localId, bsaVl = 12f, esGmax = 1.6f, hq = 0.55f)

        coEvery { userDao.getLoggedInUser() } returns u
        coEvery { athleteProfileDao.getByUserId(u.id) } returns profile(u.id)
        every  { sessionDao.observeForAthlete(u.id) } returns MutableStateFlow(listOf(s))
        coEvery { setDao.getSetsForSession(s.localId) } returns listOf(ts)
        coEvery { setDao.getMetricsForSet(ts.localId) } returns tm

        build().uiState.test {
            val first = awaitItem()
            val state = if (first is AthleteHomeUiState.Loading) awaitItem() else first
            assertTrue("expected Loaded, got $state", state is AthleteHomeUiState.Loaded)
            val loaded = state as AthleteHomeUiState.Loaded
            assertEquals(1, loaded.lastSession.setCount)
            assertEquals(100f, loaded.lastSession.maxLoadKg, 0.01f)
            assertEquals(12f, loaded.lastSession.bsaWorstPct, 0.01f)
            assertEquals(1.6f, loaded.esGmax.current, 0.001f)
            assertNull("first session should have no delta", loaded.esGmax.deltaVsPrevious)
            assertNull(loaded.hq.deltaVsPrevious)
            assertEquals(RiskLevel.MONITOR, loaded.esGmax.risk)  // 1.5 <= 1.6 < 2.0
            assertEquals(RiskLevel.MONITOR, loaded.hq.risk)      // 0.45 <= 0.55 < 0.60
            assertEquals(1, loaded.bsaTrend.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Loaded state with two sessions computes deltas vs previous`() = runTest {
        val u = userEntity()
        // Most-recent first in observeForAthlete result (typical Room ordering DESC by startedAt)
        val s1 = session(id = 11L, startedAt = 2_000_000L)   // newer
        val s0 = session(id = 10L, startedAt = 1_000_000L)   // older
        val t1 = set(localId = 110L, sessionId = s1.localId, setNumber = 1, loadKg = 110f)
        val t0 = set(localId = 100L, sessionId = s0.localId, setNumber = 1, loadKg = 100f)
        val m1 = metrics(setLocalId = t1.localId, bsaVl = 14f, esGmax = 1.8f, hq = 0.50f)
        val m0 = metrics(setLocalId = t0.localId, bsaVl = 10f, esGmax = 1.4f, hq = 0.65f)

        coEvery { userDao.getLoggedInUser() } returns u
        coEvery { athleteProfileDao.getByUserId(u.id) } returns profile(u.id, calibratedAt = 1L)
        every  { sessionDao.observeForAthlete(u.id) } returns MutableStateFlow(listOf(s1, s0))
        coEvery { setDao.getSetsForSession(s1.localId) } returns listOf(t1)
        coEvery { setDao.getSetsForSession(s0.localId) } returns listOf(t0)
        coEvery { setDao.getMetricsForSet(t1.localId) } returns m1
        coEvery { setDao.getMetricsForSet(t0.localId) } returns m0

        build().uiState.test {
            val first = awaitItem()
            val state = if (first is AthleteHomeUiState.Loading) awaitItem() else first
            assertTrue("expected Loaded, got $state", state is AthleteHomeUiState.Loaded)
            val loaded = state as AthleteHomeUiState.Loaded

            // ES:GMax went 1.4 -> 1.8 (worse, +0.4)
            assertNotNull(loaded.esGmax.deltaVsPrevious)
            assertEquals(0.4f, loaded.esGmax.deltaVsPrevious!!, 0.001f)
            assertEquals(RiskLevel.MONITOR, loaded.esGmax.risk)

            // H:Q went 0.65 -> 0.50 (worse, -0.15)
            assertEquals(-0.15f, loaded.hq.deltaVsPrevious!!, 0.001f)
            assertEquals(RiskLevel.MONITOR, loaded.hq.risk)

            // Trend has 2 points in chronological order (oldest -> newest)
            assertEquals(2, loaded.bsaTrend.size)
            assertEquals(10f, loaded.bsaTrend[0].value, 0.01f)
            assertEquals(14f, loaded.bsaTrend[1].value, 0.01f)

            // Calibration flag came through
            assertEquals(true, loaded.mvcCalibrated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `BSA worst across multiple sets uses max across muscles and sets`() = runTest {
        val u = userEntity()
        val s = session(id = 10L, startedAt = 1_000_000L)
        val a = set(localId = 100L, sessionId = s.localId, setNumber = 1, loadKg = 100f)
        val b = set(localId = 101L, sessionId = s.localId, setNumber = 2, loadKg = 100f)
        val ma = metrics(setLocalId = a.localId, bsaVl = 8f, bsaVm = 9f, bsaGmax = 11f, bsaEs = 7f)
        val mb = metrics(setLocalId = b.localId, bsaVl = 6f, bsaVm = 18f, bsaGmax = 5f, bsaEs = 4f) // worst overall

        coEvery { userDao.getLoggedInUser() } returns u
        coEvery { athleteProfileDao.getByUserId(u.id) } returns profile(u.id)
        every  { sessionDao.observeForAthlete(u.id) } returns MutableStateFlow(listOf(s))
        coEvery { setDao.getSetsForSession(s.localId) } returns listOf(a, b)
        coEvery { setDao.getMetricsForSet(a.localId) } returns ma
        coEvery { setDao.getMetricsForSet(b.localId) } returns mb

        build().uiState.test {
            val first = awaitItem()
            val state = if (first is AthleteHomeUiState.Loading) awaitItem() else first
            assertTrue(state is AthleteHomeUiState.Loaded)
            assertEquals(18f, (state as AthleteHomeUiState.Loaded).lastSession.bsaWorstPct, 0.01f)
            assertEquals(RiskLevel.RISK, state.lastSession.overallRisk)  // bsaVm=18 -> RISK (>=15)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
