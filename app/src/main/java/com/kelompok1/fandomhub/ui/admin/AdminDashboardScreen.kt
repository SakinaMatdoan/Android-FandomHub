package com.kelompok1.fandomhub.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.ui.components.StandardHeader

@Composable
fun AdminDashboardScreen(
    repository: FandomRepository,
    onNavigateToArtistList: () -> Unit,
    onNavigateToFanList: () -> Unit,
    onNavigateToApproveArtists: () -> Unit,
    onNavigateToReports: () -> Unit,
    onLogout: () -> Unit
) {
    val artistCount by repository.getArtistCount().collectAsState(initial = 0)
    val fanCount by repository.getFanCount().collectAsState(initial = 0)

    val pendingArtists by repository.getPendingArtists().collectAsState(initial = emptyList())
    val pendingReports by repository.getPendingReports().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            StandardHeader(
                title = "Admin Dashboard",
                actions = {
                   TextButton(onClick = onLogout) {
                       Text("Logout", color = MaterialTheme.colorScheme.error)
                   }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Total Artists Card
                Box(modifier = Modifier.weight(1f)) {
                    DashboardCard(
                        title = "Total Artists",
                        count = artistCount,
                        onClick = onNavigateToArtistList,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
                // Total Fans Card
                Box(modifier = Modifier.weight(1f)) {
                    DashboardCard(
                        title = "Total Fans",
                        count = fanCount,
                        onClick = onNavigateToFanList,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
            
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Pending Approvals
            DashboardCard(
                title = "Pending Artist Approvals",
                count = pendingArtists.size,
                onClick = onNavigateToApproveArtists,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            // Reports
            DashboardCard(
                title = "Pending Reports",
                count = pendingReports.size,
                onClick = onNavigateToReports,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    count: Int,
    onClick: () -> Unit,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
