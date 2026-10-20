package nibm.iot.elephantpass.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nibm.iot.elephantpass.data.models.AlertItem
import nibm.iot.elephantpass.data.models.LiveStatus
import nibm.iot.elephantpass.ui.components.InteractiveMapView
import nibm.iot.elephantpass.ui.components.IntrusionLogRow
import nibm.iot.elephantpass.ui.components.LiveStatusBanner
import nibm.iot.elephantpass.ui.theme.OnSurfaceVariantDark
import nibm.iot.elephantpass.ui.theme.WildlifeNavyPrimary

@Composable
fun LiveWatchScreen(
    liveStatus: LiveStatus,
    alerts: List<AlertItem>,
    targetLat: Double,
    targetLng: Double,
    focusNonce: Int,
    onViewOnMapClick: (lat: Double?, lng: Double?) -> Unit,
    onRecenterClick: () -> Unit,
    onAlertClick: (AlertItem) -> Unit,
    onSeeAllHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recentAlerts = alerts.take(4)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Hero Status Banner
        item {
            LiveStatusBanner(
                liveStatus = liveStatus,
                onViewOnMapClick = onViewOnMapClick
            )
        }

        // 2. Native Corridor Sector Map
        item {
            InteractiveMapView(
                alerts = alerts,
                liveStatus = liveStatus,
                targetLat = targetLat,
                targetLng = targetLng,
                focusNonce = focusNonce,
                onRecenterClick = onRecenterClick,
                mapHeight = 260
            )
        }

        // 3. Recent Intrusion Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Alerts",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = WildlifeNavyPrimary,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(onClick = onSeeAllHistoryClick) {
                    Text(
                        text = "Logs (${alerts.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = WildlifeNavyPrimary,
                            fontSize = 12.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "See All",
                        tint = WildlifeNavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 4. Recent Alerts List Items
        if (recentAlerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Peaceful",
                            tint = OnSurfaceVariantDark,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No recent breaches recorded",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceVariantDark,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        } else {
            items(recentAlerts, key = { it.key }) { alert ->
                IntrusionLogRow(
                    alert = alert,
                    onItemClick = onAlertClick
                )
            }
        }
    }
}
