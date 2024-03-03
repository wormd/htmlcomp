package htmlcomp.org

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

private fun engineWithSimpleResponse(content: String = "<h1>boo</h1>") = MockEngine { respond(content = content) }

private fun engineResponds(vararg responses: Pair<String, String>): HttpClientEngine {
    return MockEngine { req ->
        responses.forEach {
            if (it.first == req.url.encodedPath) return@MockEngine respond(it.second)
            if (it.first == "${req.url.protocol.name}://${req.url.host}${req.url.encodedPath}")
                return@MockEngine respond(it.second)
        }
        val matchers = responses.map { it.first }
        System.err.println("WARN: Not matching any of the setup mocks. Request: `${req.url.encodedPath}` with $matchers")
        respond("not matching", HttpStatusCode.NotImplemented)
    }
}

const val simpleResponse = "<h1>boo</h1>"

val testLocation = Location(
    path = "/{country}/{language}/product/{...}",
    upstream = "http://product.upstream/app1",
)

val testLocations = listOf(testLocation)

class ApplicationTest {
    @Test
    fun `returns upstream data`() = testApplication {
        val deps = replaceDepsWith { httpClient = HttpClient(engineWithSimpleResponse()) }
        routes { setupRoutes(deps, testLocations) }

        val response = client.get("/it/it/product/1234boo")

        assertEquals(simpleResponse, response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `reverse proxies to correct url`() = testApplication {
        val engine = engineWithSimpleResponse()
        val testDeps = replaceDepsWith { httpClient = HttpClient(engine) }
        routes { setupRoutes(testDeps, testLocations) }

        val calledPath = "/it/it/product/1234boo?test=due"
        client.get(calledPath)

        val url = engine.requestHistory.first().url.toString()
        assertEquals(url, "${testLocation.upstream}${calledPath}")
    }

    @Test
    fun `resolves include tags`() = testApplication {

        val engine = engineResponds(
            "/app1/it/it/product/anything" to """<!-- include url="https://validUpstream.com/some/url" -->ciao""",
            "https://validUpstream.com/some/url" to """<h2>I'm resolved</h2>"""
        )
        val deps = replaceDepsWith { httpClient = HttpClient(engine) }
        routes { setupRoutes(deps, testLocations) }

        val response = client.get("/it/it/product/anything")

        val expected = """<h2>I'm resolved</h2>ciao"""
        assertEquals(expected, response.bodyAsText())
    }
}