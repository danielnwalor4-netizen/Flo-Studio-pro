package com.example.flo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flo.model.InstrumentType
import com.example.flo.model.SoundParameters
import com.example.flo.model.Track
import com.example.flo.ui.theme.FloBorder
import com.example.flo.ui.theme.FloBorderSubtle
import com.example.flo.ui.theme.FloCyanAccent
import com.example.flo.ui.theme.FloDarkCard
import com.example.flo.ui.theme.FloDarkSurface
import com.example.flo.ui.theme.FloDarkSurfaceVariant
import com.example.flo.ui.theme.FloPurpleLight
import com.example.flo.ui.theme.FloPurplePrimary
import com.example.flo.ui.theme.FloTextPrimary
import com.example.flo.ui.theme.FloTextSecondary

@Composable
fun InspectorPanel(
    tracks: List<Track>,
    selectedTrackId: String,
    onTrackSelected: (String) -> Unit,
    onTrackInstrumentChanged: (String, InstrumentType) -> Unit,
    soundParameters: SoundParameters,
    onSoundParametersChanged: (SoundParameters) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTrack = tracks.firstOrNull { it.id == selectedTrackId } ?: tracks.firstOrNull() ?: Track("track_melody", "Melody", InstrumentType.PIANO_ACOUSTIC, 0xFF9D65FF)
    var expandedDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(FloDarkSurface)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOUND DESIGN & FX",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = FloTextPrimary
            )
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = FloPurplePrimary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Target Track Selector
        Text(
            text = "TARGET TRACK",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = FloTextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tracks.forEach { track ->
                val isSelected = track.id == activeTrack.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) FloDarkCard else FloDarkSurfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(track.colorHex) else FloBorderSubtle,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onTrackSelected(track.id) }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = track.name.split(" ").first(),
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(track.colorHex) else FloTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Track Instrument Selector
        Text(
            text = "INSTRUMENT SOUND",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = FloTextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(FloDarkCard)
                .border(width = 1.dp, color = FloBorderSubtle, shape = RoundedCornerShape(6.dp))
                .clickable { expandedDropdown = true }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("instrument_preset_selector")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Piano,
                        contentDescription = null,
                        tint = FloPurpleLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = activeTrack.instrumentType.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FloTextPrimary
                        )
                        Text(
                            text = activeTrack.instrumentType.category,
                            fontSize = 9.sp,
                            color = FloCyanAccent
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    tint = FloTextSecondary
                )
            }

            DropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false },
                modifier = Modifier.background(FloDarkSurfaceVariant)
            ) {
                val grouped = InstrumentType.values().groupBy { it.category }
                grouped.forEach { (category, instList) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "— $category —".uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = FloCyanAccent
                            )
                        },
                        onClick = { },
                        enabled = false
                    )
                    instList.forEach { inst ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = inst.displayName,
                                    fontSize = 11.sp,
                                    color = if (inst == activeTrack.instrumentType) FloPurpleLight else FloTextPrimary
                                )
                            },
                            onClick = {
                                onTrackInstrumentChanged(activeTrack.id, inst)
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FX PARAMETERS: Volume, Reverb, Attack, Release
        Text(
            text = "AUDIO EFFECTS & ENVELOPE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = FloTextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Volume Slider (0 - 100)
        ParamSlider(
            label = "Master Output",
            value = soundParameters.volume * 100f,
            valueRange = 0f..100f,
            unit = "%",
            onValueChange = {
                onSoundParametersChanged(soundParameters.copy(volume = it / 100f))
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Reverb Slider (0 - 100)
        ParamSlider(
            label = "Reverb Wet",
            value = soundParameters.reverb,
            valueRange = 0f..100f,
            unit = "%",
            onValueChange = {
                onSoundParametersChanged(soundParameters.copy(reverb = it))
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Attack Slider (0.5 - 10.0 ms)
        ParamSlider(
            label = "Envelope Attack",
            value = soundParameters.attack,
            valueRange = 0.5f..10f,
            unit = "ms",
            onValueChange = {
                onSoundParametersChanged(soundParameters.copy(attack = it))
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Release Slider (0.5 - 5.0 s)
        ParamSlider(
            label = "Envelope Release",
            value = soundParameters.release,
            valueRange = 0.5f..5f,
            unit = "s",
            onValueChange = {
                onSoundParametersChanged(soundParameters.copy(release = it))
            }
        )
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = FloTextSecondary
            )
            Text(
                text = String.format("%.1f %s", value, unit),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = FloCyanAccent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = FloPurplePrimary,
                activeTrackColor = FloPurplePrimary,
                inactiveTrackColor = FloDarkCard
            ),
            modifier = Modifier.height(26.dp)
        )
    }
}
