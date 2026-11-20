package edu.console

import java.io.File
import java.security.MessageDigest
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

suspend fun digest(file: File): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { stream ->
        val buffer = ByteArray(65_536)
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = stream.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

suspend fun duplicates(dir: File, seconds: Long): Map<String, List<File>>? =
    withTimeoutOrNull(seconds * 1_000) {
        val files = withContext(Dispatchers.IO) {
            val context = currentCoroutineContext()
            dir.walkTopDown().filter { file ->
                context.ensureActive()
                file.isFile && file.extension.equals("json", ignoreCase = true)
            }.toList()
        }
        val semaphore = Semaphore(4)
        files.map { file ->
            async { semaphore.withPermit { digest(file) to file } }
        }.awaitAll().groupBy({ it.first }, { it.second }).filterValues { it.size > 1 }
    }

fun read(name: String): JsonElement {
    val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/$name.json"))
    return stream.bufferedReader().use { Json.parseToJsonElement(it.readText()) }
}

fun main(args: Array<String>) = runBlocking {
    try {
        if (args.firstOrNull() == "duplicates") {
            printDuplicates(args)
        } else {
            printSources(args)
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        println("Ошибка: ${exception.message}")
    }
}

private suspend fun printDuplicates(args: Array<String>) {
    val directory = File(args.getOrElse(1) { "." })
    require(directory.isDirectory) { "Нет каталога: $directory" }
    val seconds = args.getOrNull(2)?.toLongOrNull() ?: 10
    require(seconds >= 0 && seconds < Long.MAX_VALUE / 1_000)

    val groups = duplicates(directory, seconds)
    when {
        groups == null -> println("Поиск прерван по таймауту")
        groups.isEmpty() -> println("Дубликатов нет")
        else -> groups.forEach { (hash, files) ->
            println("$hash: ${files.joinToString { it.path }}")
        }
    }
}

private suspend fun printSources(args: Array<String>) {
    val elapsed = measureTimeMillis {
        coroutineScope {
            val users = async {
                loadSource("Пользователи", 1_800, args, ::readUsers)
            }
            val sales = async {
                loadSource("Продажи", 1_200, args, ::readSales)
            }
            val weather = async {
                loadSource("Погода", 2_500, args, ::readWeather)
            }
            awaitAll(users, sales, weather).forEach(::println)
        }
    }
    println("Общее время: $elapsed мс")
}

private suspend fun loadSource(
    label: String,
    delayMillis: Long,
    args: Array<String>,
    readData: () -> Any,
): String {
    println("Старт: $label")
    return try {
        delay(delayMillis)
        val forcedFailure = "fail" in args && label == "Продажи"
        val randomFailure = !forcedFailure && "success" !in args && Random.nextInt(10) == 0
        if (forcedFailure || randomFailure) error("источник недоступен")

        val data = withContext(Dispatchers.IO) { readData() }
        "$label: $data"
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        "Ошибка $label: ${exception.message}"
    }
}

private fun readUsers(): List<String> = read("users").jsonArray.map { user ->
    user.jsonObject.getValue("name").jsonPrimitive.content
}

private fun readSales(): Map<String, Int> =
    read("sales").jsonObject.getValue("items").jsonArray.associate { entry ->
        val sale = entry.jsonObject
        val product = sale.getValue("product").jsonPrimitive.content
        val revenue = sale.getValue("revenue").jsonPrimitive.int
        product to revenue
    }

private fun readWeather(): List<String> = read("weather").jsonArray.map { entry ->
    val forecast = entry.jsonObject
    val city = forecast.getValue("city").jsonPrimitive.content
    "$city: ${forecast["temp"]}°C"
}
