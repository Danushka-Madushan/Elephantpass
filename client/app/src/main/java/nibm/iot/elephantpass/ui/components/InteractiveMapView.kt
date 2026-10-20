package nibm.iot.elephantpass.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import nibm.iot.elephantpass.data.models.AlertItem
import nibm.iot.elephantpass.data.models.LiveStatus
import nibm.iot.elephantpass.ui.theme.OnSurfaceDark
import nibm.iot.elephantpass.ui.theme.OnSurfaceVariantDark
import nibm.iot.elephantpass.ui.theme.OutlineVariant
import nibm.iot.elephantpass.ui.theme.SurfaceContainerLow
import nibm.iot.elephantpass.ui.theme.WildlifeNavyPrimary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun InteractiveMapView(
    alerts: List<AlertItem>,
    liveStatus: LiveStatus?,
    targetLat: Double,
    targetLng: Double,
    focusNonce: Int,
    onRecenterClick: () -> Unit,
    modifier: Modifier = Modifier,
    mapHeight: Int = 320
) {
    val context = LocalContext.current

    // Initialize osmdroid user agent configuration
    remember {
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    val mapViewRef = remember { arrayOf<MapView?>(null) }

    // Update center location when focusNonce / coordinates change
    LaunchedEffect(focusNonce, targetLat, targetLng) {
        val map = mapViewRef[0] ?: return@LaunchedEffect
        val point = GeoPoint(targetLat, targetLng)
        map.controller.animateTo(point, 15.5, 800L)
    }

    // Refresh markers on alerts or liveStatus changes
    LaunchedEffect(alerts, liveStatus) {
        val map = mapViewRef[0] ?: return@LaunchedEffect
        map.overlays.clear()

        // Active Breach Marker
        if (liveStatus != null && liveStatus.isAlert && liveStatus.lat != null && liveStatus.lng != null) {
            val activePoint = GeoPoint(liveStatus.lat, liveStatus.lng)
            val activeMarker = Marker(map).apply {
                position = activePoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "ACTIVE BREACH: ${liveStatus.activeZone}"
                snippet = "Relays: ${liveStatus.lighting} • ${liveStatus.updatedAt}"
            }
            map.overlays.add(activeMarker)
            activeMarker.showInfoWindow()
        }

        // History Alert Markers
        alerts.forEach { alert ->
            if (alert.lat != null && alert.lng != null) {
                val point = GeoPoint(alert.lat, alert.lng)
                val alertMarker = Marker(map).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = alert.zone
                    snippet = "${alert.severity} • ${alert.timestamp}"
                }
                map.overlays.add(alertMarker)
            }
        }

        map.invalidate()
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef[0]?.onDetach()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Map",
                        tint = WildlifeNavyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Corridor Sector Map",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = WildlifeNavyPrimary,
                            fontSize = 14.sp
                        )
                    )
                }

                Button(
                    onClick = onRecenterClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerLow,
                        contentColor = OnSurfaceVariantDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OutlineVariant),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Recenter",
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Center",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Habarana / Sigiriya Sanctuary Corridor",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = OnSurfaceVariantDark
                        )
                    )
                    Text(
                        text = "${alerts.size} pins",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
                            controller.setZoom(15.0)
                            controller.setCenter(GeoPoint(targetLat, targetLng))
                            mapViewRef[0] = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
