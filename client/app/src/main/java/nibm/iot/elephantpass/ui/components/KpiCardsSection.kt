package nibm.iot.elephantpass.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nibm.iot.elephantpass.data.models.LiveStatus
import nibm.iot.elephantpass.ui.theme.AlertRed
import nibm.iot.elephantpass.ui.theme.OnSurfaceDark
import nibm.iot.elephantpass.ui.theme.OnSurfaceVariantDark
import nibm.iot.elephantpass.ui.theme.OutlineVariant
import nibm.iot.elephantpass.ui.theme.SafeGreen
import nibm.iot.elephantpass.ui.theme.SurfaceContainerLow
import nibm.iot.elephantpass.ui.theme.WildlifeNavyPrimary

@Composable
fun KpiCardsSection(
    liveStatus: LiveStatus,
    alertCount: Int,
    lastAlertTime: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiSingleCard(
                icon = Icons.Default.Shield,
                label = "Perimeter State",
                value = if (liveStatus.isAlert) "ALERT" else "SAFE",
                valueColor = if (liveStatus.isAlert) AlertRed else SafeGreen,
                subtitle = "Continuous Monitoring",
                modifier = Modifier.weight(1f)
            )

            KpiSingleCard(
                icon = Icons.Default.PinDrop,
                label = "Active Sector",
                value = liveStatus.activeZone.ifBlank { "All Clear" },
                valueColor = OnSurfaceDark,
                subtitle = "Zone 1 (D26) & Zone 2 (D13)",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiSingleCard(
                icon = Icons.Default.Lightbulb,
                label = "Lighting & Relays",
                value = liveStatus.lighting.ifBlank { "ONLINE" },
                valueColor = OnSurfaceDark,
                subtitle = "Floodlight & Strobe Armed",
                modifier = Modifier.weight(1f)
            )

            KpiSingleCard(
                icon = Icons.Default.History,
                label = "Intrusion Logs",
                value = "$alertCount Incident${if (alertCount == 1) "" else "s"}",
                valueColor = OnSurfaceDark,
                subtitle = if (lastAlertTime.isNotBlank()) "Last: $lastAlertTime" else "Historical Records",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiSingleCard(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SurfaceContainerLow, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = WildlifeNavyPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariantDark
                    )
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = valueColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = OnSurfaceVariantDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
