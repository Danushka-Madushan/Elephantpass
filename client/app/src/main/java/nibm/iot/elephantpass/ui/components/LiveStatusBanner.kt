package nibm.iot.elephantpass.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nibm.iot.elephantpass.data.models.LiveStatus
import nibm.iot.elephantpass.ui.theme.AlertBorderRed
import nibm.iot.elephantpass.ui.theme.AlertContainerRed
import nibm.iot.elephantpass.ui.theme.AlertRed
import nibm.iot.elephantpass.ui.theme.OnAlertContainerRed
import nibm.iot.elephantpass.ui.theme.OnSafeContainerGreen
import nibm.iot.elephantpass.ui.theme.SafeBorderGreen
import nibm.iot.elephantpass.ui.theme.SafeContainerGreen
import nibm.iot.elephantpass.ui.theme.SafeGreen

@Composable
fun LiveStatusBanner(
    liveStatus: LiveStatus,
    onViewOnMapClick: (lat: Double?, lng: Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAlert = liveStatus.isAlert

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val containerColor = if (isAlert) AlertContainerRed else SafeContainerGreen
    val borderColor = if (isAlert) AlertBorderRed else SafeBorderGreen
    val contentColor = if (isAlert) OnAlertContainerRed else OnSafeContainerGreen
    val iconColor = if (isAlert) AlertRed else SafeGreen

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isAlert) Modifier.scale(pulseScale) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isAlert) AlertRed.copy(alpha = 0.15f) else SafeGreen.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAlert) Icons.Default.Warning else Icons.Default.Notifications,
                            contentDescription = if (isAlert) "Emergency Alert" else "Safe",
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isAlert) "ALERT: ${liveStatus.activeZone}" else "PERIMETER SAFE — All Clear",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isAlert) "Active Warning • ${liveStatus.lighting}" else "Active Monitoring • ${liveStatus.lighting}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = contentColor.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    border = BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Time",
                            tint = contentColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = liveStatus.updatedAt.ifBlank { "Just now" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = contentColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            if (isAlert) {
                Button(
                    onClick = { onViewOnMapClick(liveStatus.lat, liveStatus.lng) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Map Location",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VIEW BREACH LOCATION ON MAP",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
