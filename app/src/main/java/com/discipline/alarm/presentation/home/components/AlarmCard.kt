package com.discipline.alarm.presentation.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.presentation.theme.DarkBorder
import com.discipline.alarm.presentation.theme.DarkSurface
import com.discipline.alarm.presentation.theme.DarkSurfaceVariant
import com.discipline.alarm.presentation.theme.DisciplineAccent
import com.discipline.alarm.presentation.theme.DisciplineError
import com.discipline.alarm.presentation.theme.DisciplinePrimary
import com.discipline.alarm.presentation.theme.TextMuted
import com.discipline.alarm.presentation.theme.TextPrimary
import com.discipline.alarm.presentation.theme.TextSecondary

@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = alarm.formattedTime,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 36.sp,
                        color = if (alarm.enabled) TextPrimary else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = alarm.label.ifBlank { "Morning Discipline" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (alarm.enabled) DisciplinePrimary else TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = DisciplinePrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val tuneLabel = androidx.compose.runtime.remember(alarm.soundUri) {
                        com.discipline.alarm.util.FileStorageManager.getSoundDisplayName(context, alarm.soundUri)
                    }

                    Text(
                        text = "${alarm.repeatSummary} • 🎵 $tuneLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (alarm.mission != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = DisciplineAccent,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = alarm.mission.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DisciplineAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Alarm",
                        tint = TextMuted
                    )
                }
            }
        }
    }
}
