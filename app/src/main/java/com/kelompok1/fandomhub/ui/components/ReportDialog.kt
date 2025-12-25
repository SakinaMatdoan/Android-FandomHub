package com.kelompok1.fandomhub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val reasons = listOf(
        "Spam",
        "Nudity or sexual activity",
        "Hate speech or symbols",
        "Violence or dangerous organizations",
        "Bullying or harassment",
        "Selling illegal or regulated goods",
        "Intellectual property violation",
        "False information",
        "Something else"
    )
    
    var selectedReason by remember { mutableStateOf("") }
    var additionalDetails by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Report",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(reasons) { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (reason == selectedReason),
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                if (selectedReason == "Something else") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = additionalDetails,
                        onValueChange = { additionalDetails = it },
                        label = { Text("Please specify") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                } else if (selectedReason.isNotEmpty()) {
                     Spacer(modifier = Modifier.height(8.dp))
                     OutlinedTextField(
                        value = additionalDetails,
                        onValueChange = { additionalDetails = it },
                        label = { Text("Additional Details (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalReason = selectedReason
                            val finalDesc = if (selectedReason == "Something else") {
                                additionalDetails
                            } else {
                                additionalDetails
                            }
                            onSubmit(finalReason, finalDesc)
                        },
                        enabled = selectedReason.isNotEmpty() && (selectedReason != "Something else" || additionalDetails.isNotBlank())
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}
