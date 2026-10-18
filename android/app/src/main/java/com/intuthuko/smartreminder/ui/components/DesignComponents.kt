package com.intuthuko.smartreminder.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intuthuko.smartreminder.ui.theme.*

@Composable
fun AppHeader(title: String, subtitle: String, icon: ImageVector, onBack: (() -> Unit)? = null, action: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.fillMaxWidth().background(WarmBackground).padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBack != null) FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(48.dp), shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SurfaceWhite, contentColor = MainText)) { Icon(Icons.Default.ArrowBack, "Back") }
            else Surface(color = LightBlue, shape = RoundedCornerShape(16.dp)) { Icon(icon, null, tint = CobaltBlue, modifier = Modifier.padding(13.dp).size(22.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MainText, modifier = Modifier.semantics { heading() })
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
            }
            action()
        }
    }
}
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.65f))) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}
@Composable
fun MenuRow(title: String, subtitle: String = "", icon: ImageVector, tint: Color = CobaltBlue, onClick: () -> Unit) {
    Surface(onClick = onClick, color = SurfaceWhite, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = tint.copy(alpha = 0.11f), shape = RoundedCornerShape(14.dp)) { Icon(icon, null, tint = tint, modifier = Modifier.padding(11.dp).size(22.dp)) }
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall, color = MainText); if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText) }
            Icon(Icons.Default.ChevronRight, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
        }
    }
}
