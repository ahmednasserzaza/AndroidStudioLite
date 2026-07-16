package com.worldcup.androidstudiolite.feature.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun <E> CollectEffects(effects: Flow<E>, handler: suspend (E) -> Unit) {
    LaunchedEffect(effects) {
        effects.collect { handler(it) }
    }
}
