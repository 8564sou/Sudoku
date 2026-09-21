package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber

@Composable
fun NumberPadView(
    canUndo: Boolean,
    canRedo: Boolean,
    isNotesMode: Boolean,
    hintsUsed: Int,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit,
    onNumberClick: (Int) -> Unit,
    getRemainingCount: (Int) -> Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Action Bar: Undo, Redo, Erase, Notes, Hint
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                icon = Icons.Default.Undo,
                label = "Undo",
                enabled = canUndo,
                onClick = onUndo,
                testTag = "undo_button"
            )

            ActionButton(
                icon = Icons.Default.Redo,
                label = "Redo",
                enabled = canRedo,
                onClick = onRedo,
                testTag = "redo_button"
            )

            ActionButton(
                icon = Icons.Default.Backspace,
                label = "Erase",
                enabled = true,
                onClick = onErase,
                testTag = "erase_button"
            )

            ActionButton(
                icon = Icons.Default.Edit,
                label = "Notes",
                enabled = true,
                isActive = isNotesMode,
                activeColor = AccentAmber,
                badgeText = if (isNotesMode) "ON" else null,
                onClick = onToggleNotes,
                testTag = "notes_toggle_button"
            )

            ActionButton(
                icon = Icons.Default.AutoAwesome,
                label = "Hint",
                enabled = true,
                activeColor = AccentAmber,
                badgeText = if (hintsUsed > 0) "$hintsUsed" else null,
                onClick = onHint,
                testTag = "hint_button"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 1-9 Number Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (num in 1..9) {
                val remaining = getRemainingCount(num)
                val isCompleted = remaining <= 0

                NumberButton(
                    number = num,
                    remaining = remaining,
                    isCompleted = isCompleted,
                    onClick = { onNumberClick(num) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    isActive: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    badgeText: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        BadgedBox(
            badge = {
                if (badgeText != null) {
                    Badge(
                        containerColor = activeColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(text = badgeText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> activeColor.copy(alpha = 0.2f)
                            enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        isActive -> activeColor
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                isActive -> activeColor
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun NumberButton(
    number: Int,
    remaining: Int,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = !isCompleted,
        shape = RoundedCornerShape(12.dp),
        color = if (isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        tonalElevation = if (isCompleted) 0.dp else 2.dp,
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(56.dp)
            .testTag("number_button_$number")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = if (isCompleted) "✓" else "$remaining",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
            )
        }
    }
}
