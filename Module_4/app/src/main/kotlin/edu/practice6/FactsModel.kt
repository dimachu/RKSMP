package edu.practice6

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FactsModel : ViewModel() {
    private val facts = listOf(
        "Осьминоги имеют три сердца.",
        "Киты — млекопитающие.",
        "Пингвины — птицы.",
        "Летучие мыши способны к активному полёту.",
        "У жирафа семь шейных позвонков.",
        "Пауки имеют восемь ног.",
        "У насекомых шесть ног.",
        "Акулы имеют хрящевой скелет.",
        "Дельфины дышат лёгкими.",
        "Утконосы откладывают яйца.",
        "Самцы морских коньков вынашивают потомство.",
        "Кошки — млекопитающие.",
        "Морские звёзды относятся к иглокожим.",
        "Крокодилы — пресмыкающиеся.",
        "У бабочек полный метаморфоз.",
    )

    var fact by mutableStateOf("")
        private set

    var loading by mutableStateOf(false)
        private set

    private val mutableRate = MutableStateFlow(90.5)
    val rate = mutableRate.asStateFlow()

    var previous by mutableStateOf<Double?>(null)
        private set

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(5000)
                update()
            }
        }
    }

    fun getRandomFact(): Flow<String> = flow {
        delay(Random.nextLong(1500, 3001))
        emit(facts.random())
    }

    fun load() {
        if (loading) return
        loading = true

        viewModelScope.launch {
            try {
                getRandomFact().collect { fact = it }
            } finally {
                loading = false
            }
        }
    }

    fun update() {
        previous = mutableRate.value
        mutableRate.value = Random.nextDouble(88.5, 92.5)
    }
}
