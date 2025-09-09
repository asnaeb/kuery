package io.github.asnaeb.kuery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val queryManagerCL = staticCompositionLocalOf<QueryManager> { error("Query not provided") }

@Composable
fun QueryManagerProvider(
    queryManager: QueryManager = remember { QueryManager() },
    content: @Composable () -> Unit
) = CompositionLocalProvider(queryManagerCL provides queryManager, content)

@Composable
fun ProvidedQueryManager(): QueryManager = queryManagerCL.current