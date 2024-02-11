package htmlcomp.org

import htmlcomp.org.plugins.*
import io.ktor.client.*
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

fun app(client: HttpClient) {
    TODO("Not yet implemented")
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
    routing {
        get("/metrics-micrometer") {
            call.respond<String>(appMicrometerRegistry.scrape())
        }
        setupRoutes(dependencies)
    }
}

class DependenciesFactory: IDependenciesFactory {
    override val httpClient = HttpClient()
}

interface IDependenciesFactory {
    val httpClient: HttpClient
}

fun Routing.setupRoutes(deps: IDependenciesFactory) {
    get("/{country}/{language}/product/{...}") {
        val response = deps.httpClient.get("https://ktor.io/")
        call.respond(HttpStatusCode.OK, response.bodyAsText())
    }
}
