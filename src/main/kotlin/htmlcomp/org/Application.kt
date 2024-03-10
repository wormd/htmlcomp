package htmlcomp.org

import htmlcomp.org.components.Location
import htmlcomp.org.components.setupRoutes
import htmlcomp.org.plugins.configureHTTP
import htmlcomp.org.plugins.configureSecurity
import io.ktor.client.*
import io.ktor.client.engine.cio.*
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
        micrometerMetrics(appMicrometerRegistry)
        setupRoutes(dependencies, locations)
    }
}

private fun Routing.micrometerMetrics(appMicrometerRegistry: PrometheusMeterRegistry) {
    get("/metrics-micrometer") {
        call.respond<String>(appMicrometerRegistry.scrape())
    }
}

class DependenciesFactory : IDependenciesFactory {
    override val httpClient = HttpClient(CIO)
}

interface IDependenciesFactory {
    val httpClient: HttpClient
}
