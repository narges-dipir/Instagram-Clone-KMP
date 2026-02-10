package de.app.instagram

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.app.instagram.di.appModules
import de.app.instagram.profile.presentation.ProfileScreen
import de.app.instagram.profile.presentation.ProfileViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject


@Composable
@Preview
fun App() {
    KoinApplication(application = { modules(appModules) }) {
        MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val viewModel: ProfileViewModel = koinInject()
                val uiState by viewModel.uiState.collectAsState()
                ProfileScreen(
                    uiState = uiState,
                    onRetryClick = viewModel::loadProfile,
                )
            }
        }
    }
}
