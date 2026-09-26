package com.discipline.alarm.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.presentation.theme.DarkSurfaceVariant
import com.discipline.alarm.presentation.theme.DisciplineAccent
import com.discipline.alarm.presentation.theme.DisciplinePrimary
import com.discipline.alarm.presentation.theme.TextMuted
import com.discipline.alarm.presentation.theme.TextPrimary
import com.discipline.alarm.presentation.theme.TextSecondary
import com.discipline.alarm.util.DateTimeUtils

@Composable
fun NightDashboardBanner(
    nextAlarm: Alarm?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DisciplinePrimary.copy(alpha = 0.15f),
                            DarkSurfaceVariant
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = DisciplineAccent,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Good Night",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (nextAlarm != null) {
                        val trigger = DateTimeUtils.calculateNextTriggerMillis(nextAlarm)
                        val countdown = DateTimeUtils.getCountdownString(trigger)
                        Text(
                            text = countdown,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DisciplineAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (nextAlarm != null) {
                    Text(
                        text = "Next Alarm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = nextAlarm.formattedTime,
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = nextAlarm.label.ifBlank { "Morning Discipline" },
                        style = MaterialTheme.typography.titleMedium,
                        color = DisciplinePrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mission badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = DisciplineAccent,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "Mission: ${nextAlarm.mission?.name ?: "None"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }

                        // Snooze status badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (nextAlarm.snoozeEnabled) DisciplineAccent else TextMuted,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "Snooze: ${if (nextAlarm.snoozeEnabled) "ON" else "OFF"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (nextAlarm.snoozeEnabled) TextPrimary else TextMuted
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No Alarms Scheduled",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Text(
                        text = "Tap + below to set your morning discipline alarm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
