package de.app.instagram.create.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CreateScreen(
    modifier: Modifier = Modifier,
) {
    PlatformCreateScreen(modifier = modifier)
}

@Composable
expect fun PlatformCreateScreen(
    modifier: Modifier = Modifier,
)
