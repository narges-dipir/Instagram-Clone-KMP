package de.app.instagram.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            ProfileUiState.Loading -> {
                CircularProgressIndicator()
                Text(
                    text = "Loading profile...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            is ProfileUiState.Success -> {
                Text(text = uiState.profile.username, style = MaterialTheme.typography.headlineSmall)
                Text(text = uiState.profile.fullName, style = MaterialTheme.typography.titleMedium)
                Text(text = uiState.profile.bio, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Posts ${uiState.profile.stats.posts} | Followers ${uiState.profile.stats.followers} | Following ${uiState.profile.stats.following}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            is ProfileUiState.Error -> {
                Text(text = uiState.message, style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
