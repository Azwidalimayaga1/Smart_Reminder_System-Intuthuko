package com.intuthuko.smartreminder

import com.intuthuko.smartreminder.calendar.GoogleCalendarApi
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class GoogleCalendarApiTest {
    @Test fun parsesTimedAndAllDayEventsWithoutShiftingAllDayDates() {
        val result = GoogleCalendarApi.parseEvents("""{"items":[
            {"id":"timed","summary":"Tutor session","start":{"dateTime":"2026-10-19T08:00:00Z"},"location":"Online"},
            {"id":"all-day","summary":"Exam day","start":{"date":"2026-10-20"}}
        ]}""")
        assertEquals(2, result.size)
        assertEquals("Online", result[0].location)
        assertTrue(result[0].displayWhen(ZoneId.of("Africa/Johannesburg")).endsWith("10:00"))
        assertEquals("2026-10-20", result[1].date)
        assertNull(result[1].dateTime)
        assertTrue(result[1].displayWhen(ZoneId.of("America/Los_Angeles")).endsWith("All day"))
    }

    @Test fun ignoresCancelledMalformedAndDuplicateEventsAndHandlesMissingTitles() {
        val result = GoogleCalendarApi.parseEvents("""{"items":[
            {"id":"cancelled","status":"cancelled","start":{"date":"2026-10-20"}},
            {"id":"broken","start":{"dateTime":"not-a-date"}},
            {"id":"valid","summary":null,"start":{"date":"2026-10-20"}},
            {"id":"valid","start":{"date":"2026-10-20"}},
            {"summary":"Missing id","start":{"date":"2026-10-20"}}
        ]}""")
        assertEquals(1, result.size)
        assertEquals("Untitled event", result.single().title)
    }

    @Test fun emptyCalendarIsAValidResult() {
        assertTrue(GoogleCalendarApi.parseEvents("{}").isEmpty())
        assertTrue(GoogleCalendarApi.parseEvents("{\"items\":[]}").isEmpty())
    }
}
