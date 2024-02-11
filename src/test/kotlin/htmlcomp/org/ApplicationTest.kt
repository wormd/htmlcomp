package htmlcomp.org

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

private fun engineWithSimpleResponse() = MockEngine { respond(content = "<h1>boo</h1>") }

const val simpleResponse = "<h1>boo</h1>"

val productLocation = Location(
    path = "/{country}/{language}/product/{...}",
    upstream = "http://product.upstream",
)

val locations = listOf(productLocation)

class ApplicationTest {
    @Test
    fun `returns upstream data`() = testApplication {
        val deps = replaceDepsWith { httpClient = HttpClient(engineWithSimpleResponse()) }
        routes { setupRoutes(deps, locations) }

        val response = client.get("/it/it/product/1234boo")

        assertEquals(simpleResponse, response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `reverse proxies to correct url`() = testApplication {
        val engine = engineWithSimpleResponse()
        val deps = replaceDepsWith { httpClient = HttpClient(engine) }
        routes { setupRoutes(deps, locations) }

        val calledPath = "/it/it/product/1234boo?test=due"
        client.get(calledPath)

        val url = engine.requestHistory.first().url.toString()
        assertEquals(url, "${productLocation.upstream}${calledPath}")
//        val clientCalledPath = engine.requestHistory.first().url.encodedPathAndQuery
//        assertEquals(clientCalledPath, calledPath)
    }
}