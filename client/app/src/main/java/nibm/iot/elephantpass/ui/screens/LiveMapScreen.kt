package nibm.iot.elephantpass.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import nibm.iot.elephantpass.data.models.LiveStatus
import nibm.iot.elephantpass.ui.components.InteractiveMapView
import nibm.iot.elephantpass.ui.theme.AlertRed
import nibm.iot.elephantpass.ui.theme.OnSurfaceVariantDark
import nibm.iot.elephantpass.ui.theme.OutlineVariant
import nibm.iot.elephantpass.ui.theme.SafeGreen
import nibm.iot.elephantpass.ui.theme.WildlifeNavyPrimary

@Composable
fun LiveMapScreen(
    liveStatus: LiveStatus,
    alerts: List<AlertItem>,
    targetLat: Double,
    targetLng: Double,
    focusNonce: Int,
    onRecenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Full Interactive Map Component
        InteractiveMapView(
            alerts = alerts,
            liveStatus = liveStatus,
            targetLat = targetLat,
            targetLng = targetLng,
            focusNonce = focusNonce,
            onRecenterClick = onRecenterClick,
            mapHeight = 420,
            modifier = Modifier.weight(1f)
        )

        // Live Corridor Summary Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, OutlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Corridor Telemetry Status",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariantDark,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = if (liveStatus.isAlert) "ALERT: ${liveStatus.activeZone}" else "PERIMETER SAFE — All Clear",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (liveStatus.isAlert) AlertRed else SafeGreen,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Lat: ${"%.4f".format(targetLat)} • Lng: ${"%.4f".format(targetLng)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = OnSurfaceVariantDark,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }

                Text(
                    text = liveStatus.lighting,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = WildlifeNavyPrimary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
