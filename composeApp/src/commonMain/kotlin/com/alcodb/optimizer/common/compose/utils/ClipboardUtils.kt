package com.alcodb.optimizer.common.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

expect fun plainTextClipEntry(text: String): ClipEntry

/**
 * Returns a callback that copies [text] into the system clipboard.
 */
@Composable
fun rememberCopyTextToClipboard(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) {
        { text ->
            scope.launch { clipboard.setClipEntry(plainTextClipEntry(text)) }
        }
    }
}
