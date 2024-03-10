package htmlcomp.org.components

import htmlcomp.org.IDependenciesFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


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