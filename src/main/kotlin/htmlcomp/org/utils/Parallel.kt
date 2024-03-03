package htmlcomp.org.utils

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <A, B> Iterable<A>.pMap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

suspend fun <A, B> Sequence<A>.pMap(f: suspend (A) -> B): List<B> = coroutineScope {
    toList().map { async { f(it) } }.awaitAll()
}