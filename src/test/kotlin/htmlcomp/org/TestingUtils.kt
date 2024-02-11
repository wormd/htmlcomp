package htmlcomp.org

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*

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

fun ApplicationTestBuilder.routes(routing: Routing.() -> Unit) {
    application {
        routing {
            routing()
        }
    }
}