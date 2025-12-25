package com.kelompok1.fandomhub.ui.market

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.ui.components.StandardHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    onNavigateToCart: () -> Unit,
    onNavigateToOrderHistory: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            StandardHeader(
                title = "Marketplace Dashboard",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            MarketplaceMenuItem(
                icon = Icons.Default.ShoppingBag,
                title = "Shopping Cart",
                subtitle = "View items in your cart",
                onClick = onNavigateToCart
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            MarketplaceMenuItem(
                icon = Icons.Default.ListAlt,
                title = "My Orders",
                subtitle = "Track active and past orders",
                onClick = onNavigateToOrderHistory
            )
        }
    }
}

@Composable
fun MarketplaceMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
