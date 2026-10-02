package cooking.fifi.android

import cooking.fifi.android.data.RetryInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RetryInterceptorTest {
    private val server = MockWebServer()
    private val client = OkHttpClient.Builder().addInterceptor(RetryInterceptor(baseDelayMs = 1)).build()

    @Before fun start() = server.start()
    @After fun stop() = server.shutdown()

    private fun get() = client.newCall(Request.Builder().url(server.url("/img.jpg")).build()).execute()

    @Test fun retriesDroppedConnectionsAndServerErrors() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody("ok"))
        get().use { assertEquals("ok", it.body.string()) }
        assertEquals(3, server.requestCount)
    }

    @Test fun givesUpAfterTwoRetries() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }
        get().use { assertEquals(500, it.code) }
        assertEquals(3, server.requestCount)
    }

    @Test fun doesNotRetryClientErrors() {
        server.enqueue(MockResponse().setResponseCode(404))
        get().use { assertEquals(404, it.code) }
        assertEquals(1, server.requestCount)
    }
}
