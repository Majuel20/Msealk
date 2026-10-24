package com.msealk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.msealk.data.*
import com.msealk.util.*
import kotlinx.coroutines.launch

@Composable
internal fun ZonesScreen(zones: List<UserZone>, onZonesChanged: (List<UserZone>) -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var keywordsText by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(MatchMode.ANY) }
    var colorKey by rememberSaveable { mutableStateOf(nextZoneColorKey(zones.size)) }
    var showForm by rememberSaveable { mutableStateOf(zones.isEmpty()) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }

    fun resetForm(collapse: Boolean = false) {
        editingId = null
        name = ""
        keywordsText = ""
        mode = MatchMode.ANY
        colorKey = nextZoneColorKey(zones.size)
        formError = null
        if (collapse) showForm = false
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.testTag("ZonesScreen").fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader("Mis zonas", "Los lugares que importan, en un solo lugar.") }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (editingId == null) "Zona" else "Editando zona", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (showForm) "Nombre, color y coincidencias." else "Organiza tus avisos por casa, trabajo o distrito.", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    NeonOutlinedButton(if (showForm) "Cerrar" else "Nueva zona", color = NeonColor.Cyan) {
                        if (showForm) resetForm(collapse = true) else showForm = true
                    }
                }

                if (showForm) {
                    Spacer(Modifier.height(10.dp))
                    NeonTextField(value = name, onValueChange = { name = it; formError = null }, label = "Nombre: Casa, Trabajo...", singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    ZoneColorPicker(selected = colorKey, onSelected = { colorKey = it })
                    Spacer(Modifier.height(8.dp))
                    NeonTextField(
                        value = keywordsText,
                        onValueChange = { keywordsText = it; formError = null },
                        label = "Palabras clave",
                        minLines = 3,
                        supporting = "Una por línea o con coma: distrito, urbanización, SED, avenida."
                    )
                    formError?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = NeonColor.Red, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    CompactSwitchRow(
                        title = "Coincidir con todas",
                        subtitle = if (mode == MatchMode.ALL) "Debe contener todas las palabras" else "Basta con que contenga una palabra",
                        checked = mode == MatchMode.ALL,
                        onCheckedChange = { mode = if (it) MatchMode.ALL else MatchMode.ANY }
                    )
                    Spacer(Modifier.height(10.dp))
                    AdaptiveRow {
                        NeonButton(if (editingId == null) "Guardar" else "Actualizar", modifier = Modifier.weight(1f)) {
                            val words = AppSealPrefs.splitKeywords(keywordsText)
                            if (words.isEmpty()) {
                                formError = "Agrega al menos una palabra clave."
                                return@NeonButton
                            }
                            val zone = UserZone(
                                id = editingId ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim().ifBlank { "Mi zona" },
                                enabled = zones.firstOrNull { it.id == editingId }?.enabled ?: true,
                                matchMode = mode,
                                keywords = words,
                                colorKey = colorKey
                            )
                            val newZones = if (editingId == null) zones + zone else zones.map { if (it.id == editingId) zone else it }
                            onZonesChanged(newZones)
                            resetForm(collapse = true)
                        }
                        NeonOutlinedButton("Limpiar", modifier = Modifier.weight(1f), color = NeonColor.Purple) { resetForm(false) }
                    }
                }
            }
        }

        if (zones.isEmpty()) item { EmptyCard("Sin zonas", "Agrega casa, trabajo o zona exacta para recibir alertas útiles.") }
        else items(zones) { zone ->
            ZoneMiniCard(
                zone = zone,
                onToggle = { enabled -> onZonesChanged(zones.map { if (it.id == zone.id) it.copy(enabled = enabled) else it }) },
                onEdit = {
                    editingId = zone.id
                    name = zone.name
                    keywordsText = zone.cleanKeywords().joinToString("\n")
                    mode = zone.matchMode
                    colorKey = zone.colorKey.ifBlank { "cyan" }
                    showForm = true
                    scope.launch { listState.animateScrollToItem(0) }
                },
                onDelete = { onZonesChanged(zones.filterNot { it.id == zone.id }) }
            )
        }
    }
}

@Composable
private fun ZoneColorPicker(selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Color: ${zoneColorLabel(selected)}", color = Color.White, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ZoneColorKeys) { key ->
                val color = zoneColorByKey(key)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = zoneColorLabel(key) }
                        .selectable(selected = key == selected, role = Role.RadioButton) { onSelected(key) }
                        .padding(7.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = if (key == selected) 0.95f else 0.42f))
                        .border(2.dp, if (key == selected) Color.White else color.copy(alpha = 0.45f), CircleShape)

                )
            }
        }
    }
}

@Composable
private fun ZoneMiniCard(zone: UserZone, onToggle: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = zoneColorByKey(zone.colorKey)
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("¿Eliminar ${zone.name}?") },
        text = { Text("Dejarás de recibir coincidencias para esta zona.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Eliminar", color = NeonColor.Red) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } })
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(zone.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${zone.cleanKeywords().size} ${if (zone.cleanKeywords().size == 1) "palabra clave" else "palabras clave"} · ${zone.matchMode.label()}", color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
                Text(zone.cleanKeywords().joinToString(" · "), color = NeonColor.Muted, style = MaterialTheme.typography.bodySmall)
            }
            NeonSwitch(checked = zone.enabled, onCheckedChange = onToggle, label = "Activar zona ${zone.name}")
        }
        Spacer(Modifier.height(8.dp))
        AdaptiveRow {
            NeonOutlinedButton("Editar", color = color, modifier = Modifier.weight(1f), onClick = onEdit)
            NeonOutlinedButton("Eliminar", color = NeonColor.Red, modifier = Modifier.weight(1f), onClick = { confirmDelete = true })
        }
    }
}

