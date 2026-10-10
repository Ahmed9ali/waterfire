package com.example.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutomationFileAnalyzerTest {

    @Test
    fun testKeyValueBlockParsing() {
        val sample = """
            Name: John Doe
            Email: john@example.com
            Password: SecretPassword123!
            Phone: +1 555-0199
            Security Question: What was your first car?
            Security Answer: Honda Civic
            
            Name: Alice Smith
            Email: alice@example.com
            Password: AlicePassword456!
            Phone: +1 555-0288
            Security Question: What city were you born in?
            Security Answer: Boston
        """.trimIndent()

        val result = AutomationFileAnalyzer.analyzeText(sample)
        assertEquals(2, result.validRecordCount)

        val first = result.records[0]
        assertEquals("John Doe", first["full_name"])
        assertEquals("John", first["first_name"])
        assertEquals("Doe", first["last_name"])
        assertEquals("john@example.com", first["email"])
        assertEquals("john@example.com", first["confirm_email"])
        assertEquals("SecretPassword123!", first["password"])
        assertEquals("SecretPassword123!", first["confirm_password"])
        assertEquals("+1 555-0199", first["phone"])
        assertEquals("+1", first["country_code"])
        assertEquals("What was your first car?", first["security_question"])
        assertEquals("Honda Civic", first["security_answer"])
    }

    @Test
    fun testOneRecordPerLineDelimitedColon() {
        val sample = """
            john@x.com:Pass123
            alice@y.com:Pass456
            bob@z.com:Pass789
        """.trimIndent()

        val result = AutomationFileAnalyzer.analyzeText(sample)
        assertEquals(3, result.validRecordCount)
        assertEquals("john@x.com", result.records[0]["email"])
        assertEquals("Pass123", result.records[0]["password"])
    }

    @Test
    fun testTabAndPipeDelimitedLines() {
        val tabSample = "user1\tuser1@test.com\tSecret123\nuser2\tuser2@test.com\tSecret456"
        val tabResult = AutomationFileAnalyzer.analyzeText(tabSample)
        assertEquals(2, tabResult.validRecordCount)
        assertEquals("user1@test.com", tabResult.records[0]["email"])

        val pipeSample = "Bob|bob@mail.com|+1 555-9988\nAlice|alice@mail.com|+1 555-7766"
        val pipeResult = AutomationFileAnalyzer.analyzeText(pipeSample)
        assertEquals(2, pipeResult.validRecordCount)
        assertEquals("bob@mail.com", pipeResult.records[0]["email"])
    }

    @Test
    fun testCsvWithQuotedCommas() {
        val sample = """
            Full Name,Email,Address,Password
            "Doe, John",john@example.com,"123 Main St, Apt 4",Pass123!
            "Smith, Alice",alice@example.com,"456 Elm Ave, Suite B",Pass456!
        """.trimIndent()

        val result = AutomationFileAnalyzer.analyzeText(sample)
        assertEquals(2, result.validRecordCount)
        assertEquals("Doe, John", result.records[0]["full_name"])
        assertEquals("123 Main St, Apt 4", result.records[0]["address_line_1"])
    }

    @Test
    fun testCsvWithHeaderRow() {
        val sample = """
            First Name,Last Name,Email,Password,Phone
            John,Doe,john@example.com,Pass123,+1 555-1234
            Sarah,Connor,sarah@example.com,Pass456,+1 555-5678
        """.trimIndent()

        val result = AutomationFileAnalyzer.analyzeText(sample)
        assertEquals(2, result.validRecordCount)
        assertEquals("John", result.records[0]["first_name"])
        assertEquals("Doe", result.records[0]["last_name"])
        assertEquals("John Doe", result.records[0]["full_name"])
        assertEquals("john@example.com", result.records[0]["email"])
        assertEquals("Pass123", result.records[0]["password"])
    }

    @Test
    fun testCsvWithoutHeaderRow() {
        val sample = """
            John,Doe,john@example.com,Pass123
            Sarah,Connor,sarah@example.com,Pass456
        """.trimIndent()

        val result = AutomationFileAnalyzer.analyzeText(sample)
        assertEquals(2, result.validRecordCount)
        assertEquals("john@example.com", result.records[0]["email"])
    }

    @Test
    fun testJsonArray() {
        val sample = """
            [
                {
                    "user": {
                        "name": "Bruce Wayne",
                        "email": "bruce@waynecorp.com"
                    },
                    "password": "I Am Batman",
                    "dob": "1980-02-19"
                },
                {
                    "user": {
                        "name": "Clark Kent",
                        "email": "clark@dailyplanet.com"
                    },
                    "password": "Super Secret",
                    "dob": "1985-06-18"
                }
            ]
        """.trimIndent()

        val result = AutomationFileAnalyzer.analyzeText(sample)
        assertEquals(2, result.validRecordCount)
        assertEquals("Bruce Wayne", result.records[0]["full_name"])
        assertEquals("bruce@waynecorp.com", result.records[0]["email"])
        assertEquals("I Am Batman", result.records[0]["password"])
        assertEquals("1980", result.records[0]["birth_year"])
        assertEquals("02", result.records[0]["birth_month"])
        assertEquals("19", result.records[0]["birth_day"])
    }

    @Test
    fun testFreeTextPatternDetection() {
        val sample = """
            Contact us at support@waterfox.org or call +1 555-4321
            Alternative sales@waterfox.org or call +1 555-8765
        """.trimIndent()

        val result = AutomationFileAnalyzer.analyzeText(sample)
        assertEquals(2, result.validRecordCount)
        assertEquals("support@waterfox.org", result.records[0]["email"])
        assertTrue(result.records[0]["phone"]?.contains("555-4321") == true)
    }

    @Test
    fun testEncodingDetectionWithBom() {
        val bomBytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "Email: test@test.com".toByteArray(Charsets.UTF_8)
        val decoded = AutomationFileAnalyzer.detectEncodingAndRead(bomBytes)
        assertEquals("Email: test@test.com", decoded)
    }

    @Test
    fun testBinaryFileDetection() {
        val binaryBytes = byteArrayOf(0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00)
        assertTrue(AutomationFileAnalyzer.isBinaryFile(binaryBytes))
        val textBytes = "Hello World, this is normal text".toByteArray()
        assertFalse(AutomationFileAnalyzer.isBinaryFile(textBytes))
    }

    @Test
    fun testDeduplication() {
        val duplicateSample = """
            user@example.com:Secret123
            user@example.com:Secret123
            other@example.com:Secret456
        """.trimIndent()
        val result = AutomationFileAnalyzer.analyzeText(duplicateSample)
        assertEquals(2, result.validRecordCount)
    }
}
