package fr.berliat.hskwidget.core

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AppServicesTest {

    private lateinit var services: TestAppServices

    private object TestPriority {
        val Low = AppServices.Priority(10u)
        val High = AppServices.Priority(1u)
    }

    private class TestAppServices : AppServices()

    @BeforeTest
    fun setup() {
        services = TestAppServices()
    }

    @Test
    fun testRegisterAndGet() = runTest {
        val expected = "Result"
        services.register("service1") { expected }
        
        assertFailsWith<IllegalArgumentException> {
            services.get<String>("service1")
        }

        services.init(AppServices.Priority.Lowest)
        
        services.status.test {
            var item = awaitItem()
            // Wait for Ready state where service1 is loaded
            while (item !is AppServices.Status.Ready || item.partially || !services.isReadyForTest("service1")) {
                item = awaitItem()
            }
            assertEquals(expected, services.get("service1"))
        }
    }

    @Test
    fun testRegisterNow() {
        val expected = 42
        services.registerNow("instant", AppServices.Priority.Standard) { expected }
        assertEquals(expected, services.get("instant"))
    }

    @Test
    fun testDuplicateRegistrationThrows() {
        services.register("dup") { "first" }
        val ex = assertFailsWith<Exception> {
            services.register("dup") { "second" }
        }
        assertTrue(ex.message!!.contains("Already registered"))
    }

    @Test
    fun testPriorityInitializationOrder() = runTest {
        val order = mutableListOf<String>()
        
        services.register("low", TestPriority.Low) { 
            order.add("low")
            "low"
        }
        services.register("high", TestPriority.High) { 
            order.add("high")
            "high"
        }

        services.init(AppServices.Priority.Lowest)

        services.status.test {
            awaitItem()
            // Wait until both services are initialized
            while (order.size < 2) {
                awaitItem()
            }
            
            assertEquals("high", order[0])
            assertEquals("low", order[1])
        }
    }

    @Test
    fun testStatusTransitions() = runTest {
        services.status.test {
            // Initial state is Ready because appScope is registered in AppServices.init
            val initialState = awaitItem()
            assertTrue(initialState is AppServices.Status.Ready, "Expected Ready due to appScope but was $initialState")
            assertEquals(AppServices.Priority.Highest, initialState.upToPrio)
            assertFalse(initialState.partially)
            
            // Registering a new service at Highest priority makes the level 0 not ready
            services.register("s1", AppServices.Priority.Highest) { "ok" }
            assertEquals(AppServices.Status.Initialized, awaitItem())
            
            services.init(AppServices.Priority.Lowest)
            
            // init() forces transition to Initialized, but it's already there, so no emission.
            // Then it will transition to Ready once s1 is finished.
            
            var item = awaitItem()
            while (item !is AppServices.Status.Ready || item.partially) {
                item = awaitItem()
            }
            assertTrue(item is AppServices.Status.Ready)
            assertEquals(AppServices.Priority.Highest, item.upToPrio)
            assertFalse(item.partially)
        }
    }

    @Test
    fun testInitializationFailure() = runTest {
        val error = RuntimeException("Boom")
        services.register("broken") { throw error }

        services.init(AppServices.Priority.Lowest)

        services.status.test {
            var item = awaitItem()
            while (item !is AppServices.Status.Failed) {
                item = awaitItem()
            }
            assertEquals(error.message, item.error.message)
        }
    }

    private fun AppServices.isReadyForTest(name: String): Boolean {
        return try {
            this.get<Any>(name)
            true
        } catch (_: Exception) {
            false
        }
    }
}
