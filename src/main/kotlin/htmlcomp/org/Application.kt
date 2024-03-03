package htmlcomp.org

import htmlcomp.org.components.Fetched
import htmlcomp.org.components.fetchReplacements
import htmlcomp.org.plugins.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    val dependencies = DependenciesFactory()
    val productLocation = Location(
        path = "/{country}/{language}/product/{...}",
        upstream = "http://product.upstream",
    )
    val locations = listOf(productLocation)
    routing {
        get("/metrics-micrometer") {
            call.respond<String>(appMicrometerRegistry.scrape())
        }
        setupRoutes(dependencies, locations)
    }
}

class DependenciesFactory : IDependenciesFactory {
    override val httpClient = HttpClient(CIO)
}

interface IDependenciesFactory {
    val httpClient: HttpClient
}

data class Location(val path: String, val upstream: String)

fun Routing.setupRoutes(deps: IDependenciesFactory, locations: List<Location>) {
    for (location in locations) {
        get(location.path) {
            val url = "${location.upstream}${call.request.path()}?${call.request.queryString()}"
            val response = deps.httpClient.get(url)
            val content = response.bodyAsText()

            val fetchedContent = fetchReplacements(content, deps.httpClient)
            val replaced = doReplace(content, fetchedContent)

            call.respond(HttpStatusCode.OK, replaced)
        }
    }
}

private fun doReplace(content: String, replaces: List<Fetched>): String {
    var toReplace = content.substring(0)
    replaces.forEach {
        toReplace = toReplace.replace(it.raw, it.content)
    }
    return toReplace
}