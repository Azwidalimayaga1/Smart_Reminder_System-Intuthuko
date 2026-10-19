package com.intuthuko.smartreminder.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intuthuko.smartreminder.data.model.*
import com.intuthuko.smartreminder.data.repository.AppRepository
import com.intuthuko.smartreminder.ui.components.*
import com.intuthuko.smartreminder.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(user: User) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { AppHeader("Calendar", "Make room for what matters.", Icons.Default.CalendarMonth) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(WarmBackground).padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = SurfaceWhite, contentColor = CobaltBlue, indicator = { positions -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(positions[selectedTab]), height = 3.dp, color = CobaltBlue) }, divider = { Divider(color = BorderColor, thickness = 1.dp) }) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Month", modifier = Modifier.padding(14.dp), fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal, color = if (selectedTab == 0) CobaltBlue else SecondaryText)
                }
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Timetable", modifier = Modifier.padding(14.dp), fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal, color = if (selectedTab == 1) CobaltBlue else SecondaryText)
                }
                Tab(selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("Google", modifier = Modifier.padding(14.dp), fontSize = 13.sp, fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Normal, color = if (selectedTab == 2) CobaltBlue else SecondaryText)
                }
            }

            when (selectedTab) {
                0 -> MonthView(user)
                1 -> TimetableView()
                2 -> GoogleCalendarTab(user)
            }
        }
    }
}

@Composable
fun MonthView(user: User) {
    val repository = remember { AppRepository() }
    val events = remember(user, com.intuthuko.smartreminder.data.LocalStore.revision) { repository.getEventsForUser(user.userId) }
    var currentMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var currentYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }

    val monthName = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val daysInMonth = remember(currentMonth, currentYear) { YearMonth.of(currentYear, currentMonth).lengthOfMonth() }
    val firstDayOfWeek = remember(currentMonth, currentYear) { LocalDate.of(currentYear, currentMonth, 1).dayOfWeek.value % 7 }
    val today = LocalDate.now()

    val monthEvents = remember(events, currentMonth, currentYear) {
        events.filter { try { LocalDate.parse(it.eventDate).monthValue == currentMonth && LocalDate.parse(it.eventDate).year == currentYear } catch (e: Exception) { false } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(WarmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (currentMonth == 1) { currentMonth = 12; currentYear-- } else currentMonth-- }) { Icon(Icons.Default.ChevronLeft, "Previous", tint = MainText) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${monthName[currentMonth]} $currentYear", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                        }
                        IconButton(onClick = { if (currentMonth == 12) { currentMonth = 1; currentYear++ } else currentMonth++ }) { Icon(Icons.Default.ChevronRight, "Next", tint = MainText) }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                            Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Gray400)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val totalCells = firstDayOfWeek + daysInMonth
                    val rows = (totalCells + 6) / 7
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..6) {
                                val dayIndex = row * 7 + col
                                val day = dayIndex - firstDayOfWeek + 1
                                if (day in 1..daysInMonth) {
                                    val dateStr = "$currentYear-${String.format("%02d", currentMonth)}-${String.format("%02d", day)}"
                                    val dayEvents = monthEvents.filter { it.eventDate == dateStr }
                                    val isToday = today.year == currentYear && today.monthValue == currentMonth && today.dayOfMonth == day
                                    val isSelected = dateStr == selectedDate
                                    Box(
                                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(1.dp).clip(RoundedCornerShape(4.dp)).then(if (isToday) Modifier.background(CobaltBlue.copy(alpha = 0.1f)) else if (isSelected) Modifier.background(CobaltBlue.copy(alpha = 0.06f)) else Modifier).clickable { selectedDate = dateStr },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            if (isToday) {
                                                Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(CobaltBlue), contentAlignment = Alignment.Center) {
                                                    Text("$day", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Text("$day", fontSize = 11.sp, color = if (isSelected) CobaltBlue else MainText, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            }
                                            if (dayEvents.isNotEmpty()) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    dayEvents.take(3).forEach { evt ->
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(when (evt.eventType) { EventType.Meeting -> MeetingBlue; EventType.Exam -> ExamCoral; EventType.Workshop -> WorkshopGreen; EventType.Deadline -> DeadlineAmber; EventType.TaskDeadline -> AssignmentPurple }))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendItem("Meeting", MeetingBlue, Modifier.weight(1f))
                LegendItem("Exam", ExamCoral, Modifier.weight(1f))
                LegendItem("Workshop", WorkshopGreen, Modifier.weight(1f))
                LegendItem("Deadline", DeadlineAmber, Modifier.weight(1f))
            }
        }

        val selectedDayEvents = monthEvents.filter { it.eventDate == selectedDate }
        if (selectedDayEvents.isNotEmpty()) {
            item { Text("Events on ${selectedDate}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MainText, modifier = Modifier.padding(top = 4.dp)) }
            items(selectedDayEvents) { event ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(when (event.eventType) { EventType.Meeting -> MeetingBlue; EventType.Exam -> ExamCoral; EventType.Workshop -> WorkshopGreen; EventType.Deadline -> DeadlineAmber; EventType.TaskDeadline -> AssignmentPurple }))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MainText)
                            Text("${event.eventType.displayName}${if (event.eventTime != null) " at ${event.eventTime}" else ""}", fontSize = 13.sp, color = SecondaryText)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun TimetableView() {
    val repository = remember { AppRepository() }
    val timetableEntries = remember(com.intuthuko.smartreminder.data.LocalStore.revision) { repository.getTimetableEntries() }
    var selectedDay by remember { mutableIntStateOf(LocalDate.now().dayOfWeek.value.coerceAtMost(5)) }
    val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri")

    val dayEntries = timetableEntries.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startTime }

    Column(modifier = Modifier.fillMaxSize().background(WarmBackground)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dayNames.forEach { (dayNum, label) ->
                val isSelected = selectedDay == dayNum
                Surface(
                    modifier = Modifier.weight(1f).clickable { selectedDay = dayNum },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CobaltBlue else SurfaceWhite,
                    border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null
                ) {
                    Text(label, modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else SecondaryText)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WifiOff, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Offline available", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dayEntries) { entry ->
                val entryColor = try { Color(android.graphics.Color.parseColor(entry.color)) } catch (e: Exception) { CobaltBlue }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(entry.startTime, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                            Text("to", fontSize = 11.sp, color = Gray400)
                            Text(entry.endTime, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                        }
                        Box(modifier = Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(entryColor))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.subjectName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MainText)
                            Text("${entry.lecturer} • ${entry.room}", fontSize = 13.sp, color = SecondaryText)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = SecondaryText)
    }
}

