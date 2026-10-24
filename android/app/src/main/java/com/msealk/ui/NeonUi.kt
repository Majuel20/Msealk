package com.msealk.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msealk.R
import com.msealk.ui.theme.*

object NeonColor {
    val Dark = SealDark
    val Dark2 = SealSurface
    val Panel = Color(0xFF162638)
    val PanelSoft = Color(0xFF111D2C)
    val Line = Color(0xFF2A3D50)
    val Text = Color(0xFFF0F5FA)
    val Muted = Color(0xFFAFBED0)
    val Cyan = SealCyan
    val Blue = Color(0xFFA0BEFF)
    val Purple = SealPurple
    val Pink = SealPink
    val Green = Color(0xFF7EE2B8)
    val Orange = Color(0xFFF1C47B)
    val Red = Color(0xFFFF8892)
}

val AppSealBackground = Brush.verticalGradient(listOf(NeonColor.Dark, Color(0xFF0C1726)))

@Composable
fun NeonTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier,
        style = MaterialTheme.typography.titleLarge.copy(
            brush = Brush.horizontalGradient(listOf(Color(0xFFC4FFFA), NeonColor.Cyan, NeonColor.Blue)),
            shadow = Shadow(color = NeonColor.Cyan.copy(alpha = 0.8f), blurRadius = 16f),
            letterSpacing = 0.6.sp
        ),
        fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun LogoMark(modifier: Modifier = Modifier, compact: Boolean = false) {
    Image(painterResource(R.drawable.app_logo), contentDescription = null,
        contentScale = ContentScale.Fit, modifier = modifier.size(if (compact) 42.dp else 58.dp))
}

/** Fondo, recorte y borde comparten contorno; el padding queda dentro. */
@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.clip(shape)
        .background(Brush.linearGradient(listOf(NeonColor.Panel, NeonColor.PanelSoft)), shape)
        .border(1.dp, NeonColor.Line, shape).padding(20.dp), content = content)
}

/** Material dibuja el borde; un borde externo abarcaría también el espacio táctil. */
@Composable
fun NeonButton(text: String, modifier: Modifier = Modifier, color: Color = NeonColor.Cyan,
    enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = NeonColor.Dark,
            disabledContainerColor = NeonColor.Line, disabledContentColor = NeonColor.Muted)) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun NeonOutlinedButton(text: String, modifier: Modifier = Modifier, color: Color = NeonColor.Cyan,
    enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.38f) else NeonColor.Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color,
            containerColor = color.copy(alpha = 0.04f), disabledContentColor = NeonColor.Muted.copy(alpha = 0.5f))) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun NeonMetric(label: String, value: String, modifier: Modifier = Modifier, accent: Color = NeonColor.Cyan) {
    Column(modifier.semantics(mergeDescendants = true) {}
        .clip(RoundedCornerShape(16.dp)).background(NeonColor.Dark.copy(alpha = 0.45f))
        .padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, color = accent, style = MaterialTheme.typography.titleLarge)
        Text(label, color = NeonColor.Muted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun NeonChip(text: String, modifier: Modifier = Modifier, color: Color = NeonColor.Cyan) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f), contentColor = color) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun NeonFilter(text: String, selected: Boolean, onClick: () -> Unit, color: Color = NeonColor.Cyan) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) },
        modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(labelColor = NeonColor.Muted,
            selectedContainerColor = color.copy(alpha = 0.16f), selectedLabelColor = color),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected,
            borderColor = NeonColor.Line, selectedBorderColor = color.copy(alpha = 0.5f)))
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, color = NeonColor.Text,
        style = MaterialTheme.typography.titleMedium)
}
val ZoneColorKeys: List<String> = listOf("cyan", "purple", "green", "orange", "pink", "blue", "red")

fun zoneColorByKey(key: String): Color = when (key.lowercase()) {
    "cyan" -> NeonColor.Cyan
    "purple" -> NeonColor.Purple
    "green" -> NeonColor.Green
    "orange" -> NeonColor.Orange
    "pink" -> NeonColor.Pink
    "blue" -> NeonColor.Blue
    "red" -> NeonColor.Red
    else -> NeonColor.Cyan
}

fun zoneColorLabel(key: String): String = when (key.lowercase()) {
    "cyan" -> "Cian"
    "purple" -> "Morado"
    "green" -> "Verde"
    "orange" -> "Ámbar"
    "pink" -> "Rosa"
    "blue" -> "Azul"
    "red" -> "Rojo"
    else -> "Cian"
}

fun nextZoneColorKey(currentSize: Int): String = ZoneColorKeys[currentSize.coerceAtLeast(0) % ZoneColorKeys.size]

/** Two controls per row normally; full width when the user needs larger text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdaptiveRow(content: @Composable RowScope.() -> Unit) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    FlowRow(maxItemsInEachRow = if (largeText) 1 else 2,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}
