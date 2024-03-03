package htmlcomp.org.components

import htmlcomp.org.utils.pMap
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

suspend fun fetchReplacements(
    content: String,
    client: HttpClient,
): List<Fetched> {
    val matches = Regex("<!--.*-->").findAll(content)

    val replaces =
        matches.mapNotNull {

            val raw =
                Regex("url=\"(?<extracted>.*)\"")
                    .find(it.value)?.groups
                    ?.get("extracted")?.value

                    ?: return@mapNotNull null

            // TODO validate
            ToFetch(raw = it.value, url = raw)

        }
            .pMap { Fetched(it.raw, client.get(it.url).bodyAsText()) }

    return replaces
}

data class ToFetch(val raw: String, val url: String)
data class Fetched(val raw: String, val content: String)