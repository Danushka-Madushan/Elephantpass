package nibm.iot.elephantpass

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nibm.iot.elephantpass.ui.screens.IntrusionHistoryScreen
import nibm.iot.elephantpass.ui.screens.LiveMapScreen
import nibm.iot.elephantpass.ui.screens.LiveWatchScreen
import nibm.iot.elephantpass.ui.screens.SafetyGuideScreen
import nibm.iot.elephantpass.ui.theme.AlertContainerRed
import nibm.iot.elephantpass.ui.theme.AlertRed
import nibm.iot.elephantpass.ui.theme.ElephantpassTheme
import nibm.iot.elephantpass.ui.theme.OnSurfaceVariantDark
import nibm.iot.elephantpass.ui.theme.OutlineVariant
import nibm.iot.elephantpass.ui.theme.PrimaryContainerSky
import nibm.iot.elephantpass.ui.theme.SafeContainerGreen
import nibm.iot.elephantpass.ui.theme.SafeGreen
import nibm.iot.elephantpass.ui.theme.SurfaceContainerLow
import nibm.iot.elephantpass.ui.theme.WildlifeNavyPrimary
import nibm.iot.elephantpass.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force high-contrast dark icons for status bar & navigation bar against light themes
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            ElephantpassTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavItem(
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val navItems = listOf(
        NavItem("Watch", Icons.Default.Notifications),
        NavItem("Map", Icons.Default.Place),
        NavItem("Logs", Icons.Default.List),
        NavItem("Safety", Icons.Default.Home)
    )

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, OutlineVariant)
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PrimaryContainerSky, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Logo",
                                    tint = WildlifeNavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "Elephant Warning",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WildlifeNavyPrimary,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Department of Wildlife",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = OnSurfaceVariantDark,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            // Call 1992 Quick Hotline Action
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = AlertContainerRed,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1992"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Hotline",
                                        tint = AlertRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "1992",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AlertRed,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            // Telemetry Status Badge
                            val isAlert = uiState.liveStatus.isAlert
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isAlert) AlertContainerRed else SafeContainerGreen
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(if (isAlert) AlertRed else SafeGreen, CircleShape)
                                    )
                                    Text(
                                        text = if (isAlert) "ALERT" else "SAFE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAlert) AlertRed else SafeGreen,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                color = SurfaceContainerLow,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, OutlineVariant)
            ) {
                NavigationBar(
                    containerColor = SurfaceContainerLow,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isSelected = (uiState.selectedNavTab == index)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectNavTab(index) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = WildlifeNavyPrimary,
                                selectedTextColor = WildlifeNavyPrimary,
                                indicatorColor = PrimaryContainerSky,
                                unselectedIconColor = OnSurfaceVariantDark,
                                unselectedTextColor = OnSurfaceVariantDark
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = uiState.selectedNavTab,
                label = "NavTransition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> LiveWatchScreen(
                        liveStatus = uiState.liveStatus,
                        alerts = uiState.alerts,
                        targetLat = uiState.mapTargetLat,
                        targetLng = uiState.mapTargetLng,
                        focusNonce = uiState.mapFocusNonce,
                        onViewOnMapClick = { lat, lng ->
                            viewModel.focusOnCoordinates(lat ?: 7.9582, lng ?: 80.7621, navigateToMapTab = true)
                        },
                        onRecenterClick = { viewModel.recenterCorridor() },
                        onAlertClick = { alert ->
                            viewModel.focusOnCoordinates(alert.lat ?: 7.9582, alert.lng ?: 80.7621, navigateToMapTab = true)
                        },
                        onSeeAllHistoryClick = { viewModel.selectNavTab(2) }
                    )

                    1 -> LiveMapScreen(
                        liveStatus = uiState.liveStatus,
                        alerts = uiState.alerts,
                        targetLat = uiState.mapTargetLat,
                        targetLng = uiState.mapTargetLng,
                        focusNonce = uiState.mapFocusNonce,
                        onRecenterClick = { viewModel.recenterCorridor() }
                    )

                    2 -> IntrusionHistoryScreen(
                        filteredAlerts = uiState.filteredAlerts,
                        totalCount = uiState.alerts.size,
                        searchQuery = uiState.searchQuery,
                        selectedFilter = uiState.selectedFilter,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onFilterSelect = { viewModel.updateFilter(it) },
                        onAlertClick = { alert ->
                            viewModel.focusOnCoordinates(alert.lat ?: 7.9582, alert.lng ?: 80.7621, navigateToMapTab = true)
                        }
                    )

                    3 -> SafetyGuideScreen()
                }
            }
        }
    }
}
