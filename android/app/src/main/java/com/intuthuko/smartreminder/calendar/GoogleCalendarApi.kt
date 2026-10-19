package com.intuthuko.smartreminder.calendar

import com.google.gson.JsonParser
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoogleCalendarEvent(
    val id: String,
    val title: String,
    val date: String,
    val dateTime: String?,
    val location: String
) {
    fun displayWhen(zone: ZoneId = ZoneId.systemDefault()): String = if (dateTime != null) {
        OffsetDateTime.parse(dateTime).atZoneSameInstant(zone)
            .format(DateTimeFormatter.ofPattern("EEE, d MMM • HH:mm"))
    } else LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE, d MMM")) + " • All day"
}

class CalendarApiException(val status: Int, val reason: String) : Exception("Calendar API request failed ($status)")

object GoogleCalendarApi {
    const val READ_SCOPE = "https://www.googleapis.com/auth/calendar.events.readonly"

    fun parseEvents(body: String): List<GoogleCalendarEvent> {
        val root = JsonParser.parseString(body).asJsonObject
        val items = root.getAsJsonArray("items") ?: return emptyList()
        return items.mapNotNull { element ->
            runCatching {
                val event = element.asJsonObject
                if (event.get("status")?.asString == "cancelled") return@runCatching null
                val start = event.getAsJsonObject("start") ?: return@runCatching null
                val dateTime = start.get("dateTime")?.takeUnless { it.isJsonNull }?.asString
                val date = if (dateTime != null) OffsetDateTime.parse(dateTime).toLocalDate().toString()
                    else LocalDate.parse(start.get("date")?.asString ?: return@runCatching null).toString()
                GoogleCalendarEvent(
                    event.get("id")?.asString?.takeIf { it.isNotBlank() } ?: return@runCatching null,
                    event.get("summary")?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() } ?: "Untitled event",
                    date, dateTime,
                    event.get("location")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                )
            }.getOrNull()
        }.distinctBy { it.id }
    }

    suspend fun upcoming(accessToken: String): List<GoogleCalendarEvent> = withContext(Dispatchers.IO) {
        val minimum = URLEncoder.encode(Instant.now().toString(), "UTF-8")
        val connection = URL("https://www.googleapis.com/calendar/v3/calendars/primary/events?timeMin=$minimum&maxResults=20&singleEvents=true&orderBy=startTime&fields=items(id,summary,status,start,location)")
            .openConnection() as HttpsURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Accept", "application/json")
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                // Keep server responses and tokens out of logs and user-facing errors.
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val reason = runCatching {
                    val node = JsonParser.parseString(error).asJsonObject.getAsJsonObject("error")
                    node.getAsJsonArray("errors")?.firstOrNull()?.asJsonObject?.get("reason")?.asString.orEmpty()
                }.getOrDefault("")
                throw CalendarApiException(status, reason)
            }
            parseEvents(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }
}
