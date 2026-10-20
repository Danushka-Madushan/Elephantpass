package nibm.iot.elephantpass.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StrictWhiteColorScheme = lightColorScheme(
    primary = WildlifeNavyPrimary,
    onPrimary = OnWildlifeNavyPrimary,
    primaryContainer = PrimaryContainerSky,
    onPrimaryContainer = OnPrimaryContainerSky,
    secondary = OnSurfaceVariantDark,
    onSecondary = Color.White,
    secondaryContainer = SurfaceContainerLow,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = SafeGreen,
    onTertiary = Color.White,
    tertiaryContainer = SafeContainerGreen,
    onTertiaryContainer = OnSafeContainerGreen,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertContainerRed,
    onErrorContainer = OnAlertContainerRed,
    background = BackgroundWhite,
    onBackground = OnSurfaceDark,
    surface = SurfaceWhite,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerLow,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineColor,
    outlineVariant = OutlineVariant
)

@Composable
fun ElephantpassTheme(
    content: @Composable () -> Unit
) {
    // For villagers outdoor usability, enforce the high-contrast strict white theme
    MaterialTheme(
        colorScheme = StrictWhiteColorScheme,
        typography = Typography,
        content = content
    )
}
