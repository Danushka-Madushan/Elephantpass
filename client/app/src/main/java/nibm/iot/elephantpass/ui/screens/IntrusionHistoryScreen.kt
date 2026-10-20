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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nibm.iot.elephantpass.data.models.AlertItem
import nibm.iot.elephantpass.ui.components.IntrusionLogRow
import nibm.iot.elephantpass.ui.theme.OnSurfaceDark
import nibm.iot.elephantpass.ui.theme.OnSurfaceVariantDark
import nibm.iot.elephantpass.ui.theme.OutlineVariant
import nibm.iot.elephantpass.ui.theme.SurfaceContainerLow
import nibm.iot.elephantpass.ui.theme.WildlifeNavyPrimary

@Composable
fun IntrusionHistoryScreen(
    filteredAlerts: List<AlertItem>,
    totalCount: Int,
    searchQuery: String,
    selectedFilter: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onAlertClick: (AlertItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        "ALL" to "All Events",
        "CRITICAL" to "Critical",
        "NIGHT" to "Night",
        "TODAY" to "Recent"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Filter by sector, date, or severity...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = OnSurfaceVariantDark,
                        fontSize = 13.sp
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = OnSurfaceVariantDark
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = OnSurfaceVariantDark
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = SurfaceContainerLow,
                focusedBorderColor = WildlifeNavyPrimary,
                unfocusedBorderColor = OutlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { (key, label) ->
                val isSelected = (selectedFilter == key)
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelect(key) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WildlifeNavyPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceContainerLow,
                        labelColor = OnSurfaceVariantDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = OutlineVariant,
                        selectedBorderColor = WildlifeNavyPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Count Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isNotBlank() || selectedFilter != "ALL") {
                    "Showing ${filteredAlerts.size} of $totalCount telemetry records"
                } else {
                    "Showing all $totalCount intrusion logs"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariantDark,
                    fontSize = 12.sp
                )
            )
        }

        // Incident Logs List
        if (filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Empty",
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching intrusion logs found" else "No intrusion logs recorded",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "Try a different search term or filter chip." else "All corridor sectors are currently safe.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = OnSurfaceVariantDark
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredAlerts, key = { it.key }) { alert ->
                    IntrusionLogRow(
                        alert = alert,
                        onItemClick = onAlertClick
                    )
                }
            }
        }
    }
}
