package edu.nobel.data

import edu.nobel.domain.NobelPrize
import kotlinx.serialization.json.Json

fun seed(): List<NobelPrize> {
    val resource = checkNotNull(object {}.javaClass.getResourceAsStream("/prizes.json"))
    return resource.bufferedReader().use { Json.decodeFromString(it.readText()) }
}
