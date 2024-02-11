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

class MockedDependenciesFactory : IDependenciesFactory {
    override var httpClient = HttpClient(MockEngine {
        respond(content = "", status = HttpStatusCode.OK)
    })
}

fun replaceDepsWith(block: MockedDependenciesFactory.() -> Unit): IDependenciesFactory {
    val instance = MockedDependenciesFactory()
    instance.block()
    return instance
}

class ApplicationTest {
    @Test
    fun `returns upstream data`() = testApplication {
        val deps = replaceDepsWith {
            httpClient = HttpClient(MockEngine { respond(content = "<h1>boo</h1>") })
        }
        routes { setupRoutes(deps) }

        val response = client.get("/it/it/product/1234boo")

        assertEquals("<h1>boo</h1>", response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
    }
}

fun ApplicationTestBuilder.routes(routing: Routing.() -> Unit) {
    application {
        routing {
            routing()
        }
    }
}