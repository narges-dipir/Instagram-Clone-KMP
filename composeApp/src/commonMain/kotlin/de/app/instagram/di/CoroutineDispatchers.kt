package de.app.instagram.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface CoroutineDispatchers {
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
}

object DefaultCoroutineDispatchers : CoroutineDispatchers {
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.Default
}

fun createDefaultAppScope(
    dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
): CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)
