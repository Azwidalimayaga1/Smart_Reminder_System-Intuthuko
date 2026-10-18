package com.intuthuko.smartreminder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intuthuko.smartreminder.ui.theme.*

@Composable
fun CampusSyncLogo(size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.3).dp))
            .background(MidnightNavy),
        contentAlignment = Alignment.Center
    ) {
        Icon(androidx.compose.ui.res.painterResource(com.intuthuko.smartreminder.R.drawable.ic_brand), contentDescription = "CampusSync", tint = Color.Unspecified, modifier = Modifier.size((size * 0.9).dp))
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (bg, fg) = when (priority) {
        "High" -> HighPriorityBackground to HighPriorityCoral
        "Medium" -> WarningBackground to WarningAmber
        "Low" -> SuccessBackground to SuccessGreen
        else -> Gray100 to Gray600
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = priority,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "Pending" -> WarningBackground to WarningAmber
        "Submitted" -> LightBlue to CobaltBlue
        "Completed" -> SuccessBackground to SuccessGreen
        else -> Gray100 to Gray600
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MainText)
        action?.invoke()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(LightBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = CobaltBlue, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MainText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 13.sp, color = SecondaryText)
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, text: String, modifier: Modifier = Modifier, iconTint: Color = SecondaryText) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = SecondaryText)
    }
}

@Composable
fun AvatarInitials(name: String, size: Int = 40, backgroundColor: Color = CobaltBlue) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (size * 0.38).sp,
            fontWeight = FontWeight.Bold,
            color = if (backgroundColor == CobaltBlue) MaterialTheme.colorScheme.onPrimary else Color.White
        )
    }
}

@Composable
fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (selected) CobaltBlue else Color.Transparent)
    val textColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else SecondaryText)
    val borderColor = if (selected) CobaltBlue else BorderColor

    Surface(
        modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = ButtonDefaults.outlinedButtonBorder.copy(0.5.dp).takeIf { !selected },
        contentColor = borderColor
    ) {
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun SubjectInitial(subjectName: String, color: Color, size: Int = 36) {
    val initial = subjectName.firstOrNull()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.28).dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            fontSize = (size * 0.4).sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun RoleBadge(role: String) {
    val (bg, fg) = when (role) {
        "Student" -> LightBlue to CobaltBlue
        "Tutor" -> PurpleLight to PurpleAccent
        "Admin" -> WarningBackground to WarningAmber
        else -> Gray100 to Gray600
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = role,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

fun subjectColor(subjectName: String): Color = when (subjectName.lowercase()) {
    "mathematics" -> CobaltBlue
    "physical sciences" -> HighPriorityCoral
    "life sciences" -> SuccessGreen
    "english" -> PurpleAccent
    "accounting" -> Color(0xFFF2A93B)
    "business studies" -> Color(0xFF7B61E8)
    else -> Gray500
}
