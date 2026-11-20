package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PhoneContact
import com.example.ui.theme.CallActionGreen
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.glassEdgeBrush

/**
 * A device contact. The green button calls the main number; tapping the card opens the
 * contact's numbers so the right one can be picked, instead of calling by accident.
 */
@Composable
fun ContactItem(
    contact: PhoneContact,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(contact.id) { mutableStateOf(false) }
    val initial = contact.name.trim().take(1).uppercase().ifBlank { "#" }

    // Consistent color derived from name
    val avatarColors = listOf(
        Color(0xFF6C4DFF),
        Color(0xFF00B894),
        Color(0xFF3D8BFF),
        Color(0xFFFFAA1F),
        Color(0xFFFF4D8D),
        Color(0xFF00BCD4)
    )
    val colorIndex = kotlin.math.abs(contact.name.hashCode()) % avatarColors.size
    val avatarColor = avatarColors[colorIndex]
    val numbers = contact.allNumbers.ifEmpty { listOf(contact.primaryNumber) }.distinct()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
            .animateContentSize()
            .testTag("contact_item_${contact.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = LocalGlassColors.current.fill
        ),
        border = BorderStroke(1.dp, glassEdgeBrush(LocalGlassColors.current))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = avatarColor.copy(alpha = 0.18f),
                border = BorderStroke(1.5.dp, avatarColor.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = avatarColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (numbers.size > 1) "${contact.primaryNumber} • ${numbers.size} numbers" else contact.primaryNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Hide numbers" else "Show numbers",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            CallCircleButton(
                contentDescription = "Call ${contact.name}",
                onClick = { onCallClick(contact.primaryNumber) },
                modifier = Modifier.testTag("call_contact_${contact.id}")
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 74.dp, end = 14.dp, bottom = 10.dp)) {
                numbers.forEachIndexed { index, number ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        CallCircleButton(
                            contentDescription = "Call $number",
                            onClick = { onCallClick(number) },
                            size = 40
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallCircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 46
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = CallActionGreen.copy(alpha = 0.18f),
        modifier = modifier.size(size.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = contentDescription,
                tint = CallActionGreen,
                modifier = Modifier.size((size * 0.46f).dp)
            )
        }
    }
}
