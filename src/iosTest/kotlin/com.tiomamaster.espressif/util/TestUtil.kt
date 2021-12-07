package com.tiomamaster.espressif.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

actual fun runBlockingTest(block: suspend CoroutineScope.() -> Unit) =
    runBlocking(testCoroutineContext) { this.block() }

actual val testCoroutineContext: CoroutineContext =
    newSingleThreadContext("testRunner")
