package com.kelompok1.fandomhub.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.ReportEntity
import com.kelompok1.fandomhub.utils.DateUtils
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Search

import androidx.compose.ui.Alignment
import com.kelompok1.fandomhub.ui.components.StandardHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportListScreen(
    repository: FandomRepository,
    onBack: () -> Unit,
    onNavigateToProduct: (Int) -> Unit = {},
    onNavigateToFandom: (Int) -> Unit = {},
    onNavigateToPost: (Int) -> Unit = {},
    onNavigateToChat: (Int, Int) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Resolved")
    
    val pendingReports = repository.getPendingReports().collectAsState(initial = emptyList())
    val resolvedReports = repository.getResolvedReports().collectAsState(initial = emptyList())
    
    val displayReports = if (selectedTab == 0) pendingReports.value else resolvedReports.value
    
    var selectedReport by remember { mutableStateOf<ReportEntity?>(null) }

    Scaffold(
        topBar = {
            StandardHeader(
                title = "Admin: Reports",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                "$title (${if (index == 0) pendingReports.value.size else resolvedReports.value.size})"
                            ) 
                        }
                    )
                }
            }
            
            // Report List
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                if (displayReports.isEmpty()) {
                    item { 
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (selectedTab == 0) "No pending reports" else "No resolved reports",
                                color = Color.Gray
                            )
                        }
                    }
                }
                items(displayReports) { report ->
                    ReportItem(
                        report = report, 
                        onClick = { selectedReport = report },
                        showStatus = selectedTab == 1 // Show status for resolved tab
                    )
                }
            }
        }

        if (selectedReport != null) {
            ReportDetailDialog(
                report = selectedReport!!,
                isResolved = selectedTab == 1,
                onDismiss = { selectedReport = null },
                onViewContent = {
                    val type = selectedReport!!.type
                    val refId = selectedReport!!.referenceId ?: 0
                    if (refId != 0) {
                        when (type) {
                            "PRODUCT" -> onNavigateToProduct(refId)
                            "FANDOM" -> onNavigateToFandom(refId)
                            "POST" -> onNavigateToPost(refId)
                            "USER" -> onNavigateToChat(selectedReport!!.reporterId, refId)
                        }
                    }
                },
                onReject = {
                    scope.launch {
                        repository.dismissReport(selectedReport!!.id, selectedReport!!.reporterId)
                        selectedReport = null
                    }
                },
                onWarn = {
                    scope.launch {
                        repository.warnUser(
                            selectedReport!!.id, 
                            selectedReport!!.reportedId ?: 0,
                            selectedReport!!.reporterId
                        )
                        selectedReport = null
                    }
                },
                onSuspend = { days ->
                    scope.launch {
                        repository.suspendUser(
                            selectedReport!!.id, 
                            selectedReport!!.reportedId ?: 0, 
                            days * 24 * 60 * 60 * 1000L,
                            selectedReport!!.reporterId
                        )
                        selectedReport = null
                    }
                },
                onDelete = {
                     scope.launch {
                        repository.deleteUser(
                            selectedReport!!.id, 
                            selectedReport!!.reportedId ?: 0,
                            selectedReport!!.reporterId
                        )
                        selectedReport = null
                    }
                }
            )
        }
    }
}

@Composable
fun ReportItem(report: ReportEntity, onClick: () -> Unit, showStatus: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Type: ${report.type}", fontWeight = FontWeight.Bold)
                if (showStatus) {
                    val statusColor = when (report.status) {
                        "RESOLVED" -> Color(0xFF4CAF50)
                        "DISMISSED" -> Color.Gray
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Text(
                        report.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text("Reason: ${report.reason}", style = MaterialTheme.typography.bodyMedium)
            if (showStatus && report.adminAction != null) {
                Text(
                    "Action: ${report.adminAction}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE91E63)
                )
            }
            Text("Reporter ID: ${report.reporterId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(DateUtils.getRelativeTime(report.timestamp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun ReportDetailDialog(
    report: ReportEntity,
    isResolved: Boolean = false,
    onDismiss: () -> Unit,
    onViewContent: () -> Unit,
    onReject: () -> Unit,
    onWarn: () -> Unit,
    onSuspend: (Int) -> Unit, // days
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Report Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Type: ${report.type}", fontWeight = FontWeight.Bold)
                Text("Reason: ${report.reason}")
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Description:", fontWeight = FontWeight.Bold)
                Text(report.description.ifEmpty { "-" })
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Evidence Snapshot:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        report.contentSnapshot ?: "No snapshot available", 
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Content Navigation Button
                if (report.type in listOf("PRODUCT", "FANDOM", "POST", "USER")) {
                    OutlinedButton(
                        onClick = onViewContent,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View Content Context")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text("Reported User ID: ${report.reportedId ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                Text("Reporter ID: ${report.reporterId}", style = MaterialTheme.typography.bodySmall)
                
                if (isResolved) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (report.status == "RESOLVED") Color(0xFFE8F5E9) else Color(0xFFFAFAFA)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Status: ${report.status}", fontWeight = FontWeight.Bold, color = if (report.status == "RESOLVED") Color(0xFF4CAF50) else Color.Gray)
                            if (report.adminAction != null) {
                                Text("Action Taken: ${report.adminAction}", color = Color(0xFFE91E63))
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Admin Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Warn Button
                    OutlinedButton(
                        onClick = onWarn,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF57C00))
                    ) {
                        Text("⚠️ Issue Warning")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onDelete, 
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ban User", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Suspend User:", style = MaterialTheme.typography.labelMedium)
                    Row {
                        Button(onClick = { onSuspend(3) }, modifier = Modifier.weight(1f)) { Text("3 Days") }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = { onSuspend(7) }, modifier = Modifier.weight(1f)) { Text("7 Days") }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = { onSuspend(30) }, modifier = Modifier.weight(1f)) { Text("30 Days") }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}
