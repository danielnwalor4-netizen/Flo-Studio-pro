package com.example.flo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flo.audio.ArrangementEngine
import com.example.flo.model.Complexity
import com.example.flo.model.InstrumentType
import com.example.flo.model.KeyRoot
import com.example.flo.model.Mood
import com.example.flo.model.MusicalLayer
import com.example.flo.model.ScaleMode
import com.example.flo.model.TimeSignature
import com.example.flo.ui.theme.FloBorderSubtle
import com.example.flo.ui.theme.FloCyanAccent
import com.example.flo.ui.theme.FloDarkCard
import com.example.flo.ui.theme.FloDarkSurface
import com.example.flo.ui.theme.FloDarkSurfaceVariant
import com.example.flo.ui.theme.FloPurpleLight
import com.example.flo.ui.theme.FloPurplePrimary
import com.example.flo.ui.theme.FloTextPrimary
import com.example.flo.ui.theme.FloTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratePanel(
    selectedMood: Mood,
    onMoodSelected: (Mood) -> Unit,
    bpm: Int,
    onBpmChanged: (Int) -> Unit,
    keyRoot: KeyRoot,
    onKeyRootSelected: (KeyRoot) -> Unit,
    scaleMode: ScaleMode,
    onScaleModeSelected: (ScaleMode) -> Unit,
    timeSignature: TimeSignature,
    onTimeSignatureSelected: (TimeSignature) -> Unit,
    bars: Int,
    onBarsSelected: (Int) -> Unit,
    complexity: Complexity,
    onComplexitySelected: (Complexity) -> Unit,
    seed: Long,
    onRandomizeSeed: () -> Unit,
    onBrowsePresetsClick: () -> Unit,
    onRegenerateLayer: (MusicalLayer) -> Unit,
    onCreateVariation: () -> Unit,
    onGenerateClick: () -> Unit,
    onStructureSelected: (ArrangementEngine.ArrangementStructure) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(FloDarkSurface)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with Creator Presets Quick Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMPOSITION ENGINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = FloTextPrimary
            )
            // Presets Browser Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(FloDarkCard)
                    .border(width = 1.dp, color = FloPurpleLight, shape = RoundedCornerShape(6.dp))
                    .clickable { onBrowsePresetsClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("browse_presets_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = FloPurpleLight,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloPurpleLight
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // PRIMARY ACTION: GENERATE COMPOSITION
        Button(
            onClick = onGenerateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = FloPurplePrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("generate_composition_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "GENERATE COMPOSITION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // LAYER REGENERATION & VARIATION STRIP (M2)
        SectionLabel("LAYER REGENERATION (M2)")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LayerChip(label = "Melody", onClick = { onRegenerateLayer(MusicalLayer.MELODY) }, modifier = Modifier.weight(1f))
            LayerChip(label = "Chords", onClick = { onRegenerateLayer(MusicalLayer.CHORDS) }, modifier = Modifier.weight(1f))
            LayerChip(label = "Bass", onClick = { onRegenerateLayer(MusicalLayer.BASS) }, modifier = Modifier.weight(1f))
            LayerChip(label = "Drums", onClick = { onRegenerateLayer(MusicalLayer.RHYTHM) }, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Create Variation Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(FloDarkCard)
                .border(width = 1.dp, color = FloCyanAccent.copy(alpha = 0.6f), shape = RoundedCornerShape(6.dp))
                .clickable { onCreateVariation() }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = FloCyanAccent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CREATE MUSICAL VARIATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FloCyanAccent)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MOOD SECTION (M2: 9 Moods)
        SectionLabel("MOOD")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Mood.values().forEach { mood ->
                PillTag(
                    text = mood.displayName,
                    isSelected = mood == selectedMood,
                    onClick = { onMoodSelected(mood) },
                    testTag = "mood_pill_${mood.name.lowercase()}"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // KEY & SCALE SECTION (M2: 12 Chromatic Keys & 6 Scales)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("KEY & SCALE")
            Text(
                text = "${keyRoot.displayName} ${scaleMode.displayName.uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FloPurpleLight
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KeyRoot.values().forEach { root ->
                KeyPill(
                    text = root.displayName,
                    isSelected = root == keyRoot,
                    onClick = { onKeyRootSelected(root) }
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ScaleMode.values().forEach { scale ->
                PillTag(
                    text = scale.displayName,
                    isSelected = scale == scaleMode,
                    onClick = { onScaleModeSelected(scale) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TEMPO, TIME SIGNATURE & LENGTH (M2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("TEMPO & METER")
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onBpmChanged(bpm - 1) }, modifier = Modifier.size(22.dp)) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, tint = FloTextSecondary, modifier = Modifier.size(12.dp))
                }
                Text(
                    text = "$bpm BPM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloCyanAccent,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                IconButton(onClick = { onBpmChanged(bpm + 1) }, modifier = Modifier.size(22.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = FloTextSecondary, modifier = Modifier.size(12.dp))
                }
            }
        }
        Slider(
            value = bpm.toFloat(),
            onValueChange = { onBpmChanged(it.toInt()) },
            valueRange = 40f..200f,
            colors = SliderDefaults.colors(
                thumbColor = FloPurplePrimary,
                activeTrackColor = FloPurplePrimary,
                inactiveTrackColor = FloDarkCard
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimeSignature.values().forEach { ts ->
                PillTag(
                    text = ts.displayName,
                    isSelected = ts == timeSignature,
                    onClick = { onTimeSignatureSelected(ts) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // COMPOSITION LENGTH (BARS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("LENGTH (BARS)")
            Text(text = "$bars Bars", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FloTextSecondary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(4, 8, 16, 32).forEach { b ->
                PillTag(
                    text = "$b BARS",
                    isSelected = b == bars,
                    onClick = { onBarsSelected(b) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // COMPLEXITY & SEED (M2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("COMPLEXITY")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Seed: $seed", fontSize = 10.sp, color = FloTextSecondary)
                IconButton(onClick = onRandomizeSeed, modifier = Modifier.size(22.dp)) {
                    Icon(imageVector = Icons.Default.Casino, contentDescription = "Randomize Seed", tint = FloPurpleLight, modifier = Modifier.size(14.dp))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Complexity.values().forEach { comp ->
                PillTag(
                    text = comp.displayName,
                    isSelected = comp == complexity,
                    onClick = { onComplexitySelected(comp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // M5: AUTOMATIC SONG ARRANGEMENT STRUCTURES
        SectionLabel("ARRANGEMENT STRUCTURE (M5)")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ArrangementEngine.ArrangementStructure.values().forEach { struct ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(FloDarkCard)
                        .border(width = 0.5.dp, color = FloBorderSubtle, shape = RoundedCornerShape(4.dp))
                        .clickable { onStructureSelected(struct) }
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Text(text = struct.displayName, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = FloPurpleLight)
                }
            }
        }
    }
}

@Composable
private fun LayerChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(FloDarkCard)
            .border(width = 0.5.dp, color = FloBorderSubtle, shape = RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = FloPurpleLight, modifier = Modifier.size(10.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FloTextPrimary)
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = FloTextSecondary,
        letterSpacing = 1.0.sp,
        modifier = Modifier.padding(bottom = 5.dp)
    )
}

@Composable
fun PillTag(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val mod = modifier
        .clip(RoundedCornerShape(5.dp))
        .background(if (isSelected) FloPurplePrimary else FloDarkCard)
        .border(
            width = 1.dp,
            color = if (isSelected) FloPurpleLight else FloBorderSubtle,
            shape = RoundedCornerShape(5.dp)
        )
        .clickable { onClick() }
        .padding(horizontal = 8.dp, vertical = 5.dp)

    Box(
        modifier = if (testTag != null) mod.testTag(testTag) else mod,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else FloTextPrimary
        )
    }
}

@Composable
private fun KeyPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) FloPurplePrimary else FloDarkCard)
            .border(
                width = 1.dp,
                color = if (isSelected) FloPurpleLight else FloBorderSubtle,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else FloTextPrimary
        )
    }
}
