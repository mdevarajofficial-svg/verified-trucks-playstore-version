package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VerifiedTrucksBoldColorScheme =
  lightColorScheme(
    primary = RoyalBluePrimary,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueContainer,
    onPrimaryContainer = OnRoyalBlueContainer,
    secondary = EmeraldGreenAccent,
    onSecondary = Color.White,
    secondaryContainer = EmeraldGreenContainer,
    onSecondaryContainer = OnEmeraldGreenContainer,
    tertiary = RoyalBlueDark,
    onTertiary = Color.White,
    tertiaryContainer = AmberWarningContainer,
    onTertiaryContainer = SlateInkPrimary,
    background = CleanWhiteBackground,
    onBackground = SlateInkPrimary,
    surface = CleanWhiteBackground,
    onSurface = SlateInkPrimary,
    surfaceVariant = LightSurfaceSlate,
    onSurfaceVariant = SlateInkSecondary,
    surfaceContainerLowest = CleanWhiteBackground,
    surfaceContainerLow = LightSurfaceSlate,
    surfaceContainer = SurfaceSlateVariant,
    outline = BorderSlate,
    outlineVariant = BorderSlate,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = CrimsonError,
  )

object VerifiedTrucksBrushes {
  val ExecutiveHeroGradient: Brush =
    Brush.linearGradient(
      colors = listOf(RoyalBlueDark, RoyalBluePrimary, RoyalBlueBright)
    )

  val EmeraldActionGradient: Brush =
    Brush.horizontalGradient(
      colors = listOf(EmeraldGreenDark, EmeraldGreenAccent, EmeraldGreenVibrant)
    )
}

val VerifiedTrucksShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
  )

@Composable
fun VerifiedTrucksTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = VerifiedTrucksBoldColorScheme,
    typography = Typography,
    shapes = VerifiedTrucksShapes,
    content = content,
  )
}
