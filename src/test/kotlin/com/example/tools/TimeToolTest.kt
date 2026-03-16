package com.example.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TimeToolTest {

    private val timeTool = TimeTool()

    @Test
    fun testToolNameAndDescription() {
        assertEquals("get_current_time", timeTool.name)
        assertNotNull(timeTool.description)
    }

    @Test
    fun testExecuteReturnsValidDateTime() {
        val before = ZonedDateTime.now().minusSeconds(1)
        val result = timeTool.execute("{}")
        val after = ZonedDateTime.now().plusSeconds(1)

        assertTrue(result.contains("\"currentTime\""), "Result should contain currentTime property")

        val timeStr = result.substringAfter("\"currentTime\":\"").substringBefore("\"}")
        val parsed = ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        assertTrue(
            !parsed.isBefore(before.truncatedTo(ChronoUnit.SECONDS)) &&
                !parsed.isAfter(after),
            "Returned time should be close to current time"
        )
    }

    @Test
    fun testToolDefinitionIsValid() {
        val toolDef = timeTool.toToolDefinition()
        assertNotNull(toolDef)
    }
}
