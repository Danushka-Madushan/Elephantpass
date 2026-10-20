package nibm.iot.elephantpass.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nibm.iot.elephantpass.data.models.AlertItem
import nibm.iot.elephantpass.ui.theme.AlertBorderRed
import nibm.iot.elephantpass.ui.theme.AlertContainerRed
import nibm.iot.elephantpass.ui.theme.AlertRed
import nibm.iot.elephantpass.ui.theme.OnAlertContainerRed
import nibm.iot.elephantpass.ui.theme.OnSurfaceDark
import nibm.iot.elephantpass.ui.theme.OnSurfaceVariantDark
import nibm.iot.elephantpass.ui.theme.OnWarningContainerAmber
import nibm.iot.elephantpass.ui.theme.OutlineVariant
import nibm.iot.elephantpass.ui.theme.PrimaryContainerSky
import nibm.iot.elephantpass.ui.theme.WarningContainerAmber
import nibm.iot.elephantpass.ui.theme.WildlifeNavyPrimary

@Composable
fun IntrusionLogRow(
    alert: AlertItem,
    onItemClick: (AlertItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCritical = alert.isCritical
    val isNight = alert.isNight

    Card(
        onClick = { onItemClick(alert) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = alert.timestamp.ifBlank { "Just now" },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = WildlifeNavyPrimary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "• Telemetry",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = OnSurfaceVariantDark,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }

                Text(
                    text = alert.zone.ifBlank { "Unknown Sector" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceDark,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Severity Chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCritical) AlertContainerRed else WarningContainerAmber,
                        border = BorderStroke(1.dp, if (isCritical) AlertBorderRed else Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Severity",
                                tint = if (isCritical) AlertRed else OnWarningContainerAmber,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = alert.severity,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isCritical) OnAlertContainerRed else OnWarningContainerAmber
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Lighting Chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isNight) Color(0xFFEDE9FE) else PrimaryContainerSky,
                        border = BorderStroke(1.dp, if (isNight) Color(0xFFDDD6FE) else Color(0xFFBAE6FD))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isNight) Icons.Default.Info else Icons.Default.Check,
                                contentDescription = "Lighting",
                                tint = if (isNight) Color(0xFF5B21B6) else Color(0xFF0369A1),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isNight) "Night" else "Day",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    color = if (isNight) Color(0xFF5B21B6) else Color(0xFF0369A1)
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "View on map",
                tint = OnSurfaceVariantDark,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
