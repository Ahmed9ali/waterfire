package com.example.automation

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object AutomationFileAnalyzer {

    private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    private val PHONE_REGEX = Regex("^(\\+?\\d{1,4}[\\s.-]?)?\\(?\\d{2,4}\\)?[\\s.-]?\\d{3,4}[\\s.-]?\\d{3,4}\$")
    private val URL_REGEX = Regex("^https?://[A-Za-z0-9.-]+(/.*)?\$")
    private val DATE_ISO_REGEX = Regex("^\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\$")
    private val DATE_EU_REGEX = Regex("^\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4}\$")

    private val SYNONYMS = mapOf(
        "first_name" to listOf("first name", "firstname", "fname", "given name", "first"),
        "last_name" to listOf("last name", "lastname", "lname", "surname", "family name", "last"),
        "full_name" to listOf("full name", "fullname", "name", "customer name", "contact name", "client name"),
        "middle_name" to listOf("middle name", "middlename", "mname"),
        "username" to listOf("username", "user name", "userid", "user id", "login", "handle", "nickname", "user", "account"),
        "email" to listOf("email", "e mail", "mail", "email address", "e mail address", "user email", "login email"),
        "confirm_email" to listOf("confirm email", "verify email", "repeat email", "email confirmation", "re enter email"),
        "password" to listOf("password", "pass", "pwd", "user password", "login password", "secret"),
        "confirm_password" to listOf("confirm password", "verify password", "repeat password", "password confirmation", "re enter password"),
        "phone" to listOf("phone", "mobile", "telephone", "tel", "cell", "cellphone", "phone number", "mobile number", "contact number"),
        "country_code" to listOf("country code", "dial code", "calling code", "phone code"),
        "date_of_birth" to listOf("date of birth", "dob", "birthday", "birth date"),
        "birth_day" to listOf("birth day", "day of birth", "bday day", "day"),
        "birth_month" to listOf("birth month", "month of birth", "bday month", "month"),
        "birth_year" to listOf("birth year", "year of birth", "bday year", "year"),
        "age" to listOf("age", "years old"),
        "gender" to listOf("gender", "sex"),
        "title" to listOf("title", "prefix", "salutation"),
        "company" to listOf("company", "organization", "organisation", "business", "employer"),
        "job_title" to listOf("job title", "job", "position", "role", "occupation"),
        "website" to listOf("website", "url", "site", "homepage", "web address"),
        "address_line_1" to listOf("address line 1", "address 1", "street address", "street", "address"),
        "address_line_2" to listOf("address line 2", "address 2", "apt", "suite", "unit", "building", "apartment", "floor"),
        "city" to listOf("city", "town", "locality", "municipality"),
        "state" to listOf("state", "province", "region", "county"),
        "zip_code" to listOf("zip code", "zip", "postal code", "postcode", "pin code"),
        "country" to listOf("country", "nation"),
        "subject" to listOf("subject", "topic", "heading"),
        "message" to listOf("message", "comment", "feedback", "inquiry", "description", "details", "notes", "body"),
        "rating" to listOf("rating", "score", "stars"),
        "security_question" to listOf("security question", "security question 1", "secret question", "recovery question"),
        "security_answer" to listOf("security answer", "security answer 1", "secret answer", "recovery answer", "answer")
    )

    fun isBinaryFile(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val sampleSize = minOf(bytes.size, 1024)
        var nullCount = 0
        for (i in 0 until sampleSize) {
            if (bytes[i] == 0.toByte()) {
                nullCount++
            }
        }
        return (nullCount.toFloat() / sampleSize) > 0.05f
    }

    fun detectEncodingAndRead(bytes: ByteArray): String {
        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            return ""
        }
        if (isBinaryFile(bytes)) {
            return ""
        }

        // BOM checks
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        }
        return try {
            val s = String(bytes, StandardCharsets.UTF_8)
            if (s.contains("\uFFFD")) {
                String(bytes, StandardCharsets.ISO_8859_1)
            } else {
                s
            }
        } catch (e: Exception) {
            String(bytes, StandardCharsets.ISO_8859_1)
        }
    }

    fun analyzeText(content: String): FileAnalysisResult {
        // Standardize line endings (CRLF -> LF)
        val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
        val trimmed = normalized.trim()
        if (trimmed.isEmpty()) {
            return FileAnalysisResult(emptyList(), emptyList(), 0, 0, 0)
        }

        // 1. Try JSON format
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
        ) {
            val jsonResult = parseJson(trimmed)
            if (jsonResult != null && jsonResult.validRecordCount > 0) {
                return deduplicateRecords(jsonResult)
            }
        }

        // 2. Try Key-Value blocks (requires block separators and label-like keys)
        if (isKeyValueBlockFormat(trimmed)) {
            val kvResult = parseKeyValueBlocks(trimmed)
            if (kvResult.validRecordCount > 0) {
                return deduplicateRecords(kvResult)
            }
        }

        // 3. Try CSV / TSV / Delimited
        val delimitedResult = parseDelimited(trimmed)
        if (delimitedResult != null && delimitedResult.validRecordCount > 0) {
            return deduplicateRecords(delimitedResult)
        }

        // 4. Free-text fallback
        return deduplicateRecords(parseFreeText(trimmed))
    }

    private fun deduplicateRecords(result: FileAnalysisResult): FileAnalysisResult {
        val seen = mutableSetOf<String>()
        val distinctRecords = mutableListOf<Map<String, String>>()
        for (r in result.records) {
            val key = r.entries.sortedBy { it.key }.joinToString("||") { "${it.key}=${it.value.trim()}" }
            if (key.isNotBlank() && !seen.contains(key)) {
                seen.add(key)
                distinctRecords.add(r)
            }
        }
        return result.copy(
            records = distinctRecords,
            validRecordCount = distinctRecords.size
        )
    }

    private fun normalizeLabel(label: String): String {
        return label.lowercase()
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()
    }

    fun inferFieldType(label: String, sampleValues: List<String>): Pair<String, Float> {
        val normLabel = normalizeLabel(label)

        // 1. Direct synonym matching on label
        if (normLabel.isNotBlank()) {
            for ((fieldType, synList) in SYNONYMS) {
                for (syn in synList) {
                    if (normLabel == syn || normLabel.contains(syn)) {
                        return Pair(fieldType, 0.95f)
                    }
                }
            }
        }

        // 2. Value pattern heuristics
        var emailHits = 0
        var phoneHits = 0
        var urlHits = 0
        var dateHits = 0

        val validSamples = sampleValues.filter { it.isNotBlank() }
        for (v in validSamples) {
            if (EMAIL_REGEX.matches(v)) emailHits++
            else if (URL_REGEX.matches(v)) urlHits++
            else if (PHONE_REGEX.matches(v) && v.length in 7..18) phoneHits++
            else if (DATE_ISO_REGEX.matches(v) || DATE_EU_REGEX.matches(v)) dateHits++
        }

        val total = validSamples.size.coerceAtLeast(1).toFloat()
        if (emailHits / total >= 0.5f) return Pair("email", 0.9f)
        if (urlHits / total >= 0.5f) return Pair("website", 0.85f)
        if (phoneHits / total >= 0.5f) return Pair("phone", 0.8f)
        if (dateHits / total >= 0.5f) return Pair("date_of_birth", 0.8f)

        // Looks like a password when beside an email/username without explicit label
        if (normLabel.startsWith("column") && validSamples.any { it.length >= 6 }) {
            return Pair("password", 0.75f)
        }

        // Keep raw label as custom field
        return Pair(if (normLabel.isNotBlank()) normLabel.replace(' ', '_') else "custom", 0.5f)
    }

    fun deriveMissingFields(record: MutableMap<String, String>) {
        // Full name from first + last
        if (!record.containsKey("full_name") || record["full_name"].isNullOrBlank()) {
            val first = record["first_name"] ?: ""
            val last = record["last_name"] ?: ""
            if (first.isNotBlank() || last.isNotBlank()) {
                record["full_name"] = "$first $last".trim()
            }
        }
        // Split full name into first + last if missing
        val fullName = record["full_name"]
        if (!fullName.isNullOrBlank()) {
            if (record["first_name"].isNullOrBlank() && record["last_name"].isNullOrBlank()) {
                val parts = fullName.trim().split(Regex("\\s+"), 2)
                record["first_name"] = parts.getOrNull(0) ?: ""
                if (parts.size > 1) {
                    record["last_name"] = parts[1]
                }
            }
        }
        // Confirm email = email
        record["email"]?.let { if (record["confirm_email"].isNullOrBlank()) record["confirm_email"] = it }
        // Confirm password = password
        record["password"]?.let { if (record["confirm_password"].isNullOrBlank()) record["confirm_password"] = it }

        // Date of birth parts
        record["date_of_birth"]?.let { dob ->
            val parts = dob.split(Regex("[-/.]"))
            if (parts.size == 3) {
                if (parts[0].length == 4) { // yyyy-mm-dd
                    if (record["birth_year"].isNullOrBlank()) record["birth_year"] = parts[0]
                    if (record["birth_month"].isNullOrBlank()) record["birth_month"] = parts[1]
                    if (record["birth_day"].isNullOrBlank()) record["birth_day"] = parts[2]
                } else if (parts[2].length == 4) { // dd-mm-yyyy or mm-dd-yyyy
                    if (record["birth_day"].isNullOrBlank()) record["birth_day"] = parts[0]
                    if (record["birth_month"].isNullOrBlank()) record["birth_month"] = parts[1]
                    if (record["birth_year"].isNullOrBlank()) record["birth_year"] = parts[2]
                }
            }
        }

        // Phone split into country code + phone
        record["phone"]?.let { phone ->
            if (phone.startsWith("+") && record["country_code"].isNullOrBlank()) {
                val spaceIndex = phone.indexOfAny(charArrayOf(' ', '-'))
                if (spaceIndex in 2..5) {
                    record["country_code"] = phone.substring(0, spaceIndex)
                }
            }
        }
    }

    private fun isKeyValueBlockFormat(text: String): Boolean {
        val hasBlockSeparators = text.contains(Regex("(\\n\\s*\\n)+|\\n[-=#]{3,}\\n"))
        if (!hasBlockSeparators) return false
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var labelCount = 0
        for (line in lines.take(15)) {
            val key = line.substringBefore(":").substringBefore("=").trim().lowercase()
            if (key.length in 2..35 && !key.contains("@") && SYNONYMS.keys.any { synKey ->
                SYNONYMS[synKey]?.any { s -> key == s || key.contains(s) } == true
            }) {
                labelCount++
            }
        }
        return labelCount >= 2
    }

    private fun parseKeyValueBlocks(text: String): FileAnalysisResult {
        val rawBlocks = text.split(Regex("(\\n\\s*\\n)+|\\n[-=#]{3,}\\n"))
        val rawRecords = mutableListOf<Map<String, String>>()
        val allKeys = LinkedHashSet<String>()

        for (block in rawBlocks) {
            val recordMap = mutableMapOf<String, String>()
            for (line in block.lines()) {
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) continue
                val separator = when {
                    trimmedLine.contains(":") -> ":"
                    trimmedLine.contains("=") -> "="
                    trimmedLine.contains(" - ") -> " - "
                    else -> null
                }
                if (separator != null) {
                    val key = trimmedLine.substringBefore(separator).trim()
                    val value = trimmedLine.substringAfter(separator).trim()
                    if (key.isNotEmpty()) {
                        recordMap[key] = value
                        allKeys.add(key)
                    }
                }
            }
            if (recordMap.isNotEmpty()) {
                rawRecords.add(recordMap)
            }
        }

        val detectedColumns = allKeys.map { key: String ->
            val samples = rawRecords.mapNotNull { it[key] }.take(5)
            val (inferredType, conf) = inferFieldType(key, samples)
            DetectedColumn(
                rawKey = key,
                normalizedType = inferredType,
                sampleValues = samples,
                confidence = conf
            )
        }

        val mappedRecords = rawRecords.map { rec: Map<String, String> ->
            val mapped = mutableMapOf<String, String>()
            for (col in detectedColumns) {
                val value = rec[col.rawKey] ?: ""
                if (value.isNotBlank()) {
                    mapped[col.normalizedType] = value
                }
            }
            deriveMissingFields(mapped)
            mapped
        }

        return FileAnalysisResult(
            detectedColumns = detectedColumns,
            records = mappedRecords,
            totalLines = text.lines().size,
            validRecordCount = mappedRecords.size,
            skippedLinesCount = 0
        )
    }

    private fun parseDelimited(text: String): FileAnalysisResult? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null

        val candidateDelimiters = listOf(',', ';', '\t', '|', ':')
        var bestDelimiter: Char? = null
        var maxCount = 0

        for (d in candidateDelimiters) {
            // Check if delimiter appears consistently across at least half of the first few lines
            val sampleLines = lines.take(5)
            val countFirst = sampleLines.first().count { it == d }
            if (countFirst > 0) {
                val consistentCount = sampleLines.count { it.count { c -> c == d } > 0 }
                if (consistentCount >= (sampleLines.size / 2).coerceAtLeast(1) && countFirst > maxCount) {
                    // If colon, check it's not prose with colons (e.g. "Note: hello world this is long")
                    if (d == ':') {
                        val partBefore = sampleLines.first().substringBefore(':').trim()
                        val partAfter = sampleLines.first().substringAfter(':').trim()
                        if (partBefore.contains(' ') && partBefore.split(' ').size > 3) {
                            continue
                        }
                    }
                    maxCount = countFirst
                    bestDelimiter = d
                }
            }
        }

        // Only consider space if no standard delimiter exists and lines look like 2-5 clean tabular tokens
        if (bestDelimiter == null) {
            val sampleLines = lines.take(5)
            val proseWords = listOf("the", "at", "or", "and", "is", "for", "with", "call", "contact")
            val hasProse = sampleLines.any { line -> proseWords.any { w -> line.contains(" $w ", ignoreCase = true) } }
            if (!hasProse) {
                val spaceTokensCount = sampleLines.map { it.split(Regex("\\s+")).size }
                if (spaceTokensCount.all { it in 2..6 } && spaceTokensCount.distinct().size <= 2) {
                    bestDelimiter = ' '
                }
            }
        }

        if (bestDelimiter == null) {
            return null
        }

        val delimiter = bestDelimiter
        val firstLineParts = splitLine(lines[0], delimiter)
        val hasLetterHeader = firstLineParts.all { part -> part.any { it.isLetter() } && !EMAIL_REGEX.matches(part) }

        val headers: List<String>
        val dataLines: List<String>

        if (hasLetterHeader && lines.size > 1) {
            headers = firstLineParts
            dataLines = lines.drop(1)
        } else {
            headers = (1..firstLineParts.size).map { "Column $it" }
            dataLines = lines
        }

        val rawRecords = mutableListOf<Map<String, String>>()
        for (line in dataLines) {
            val parts = splitLine(line, delimiter)
            if (parts.isNotEmpty()) {
                val map = mutableMapOf<String, String>()
                for (i in headers.indices) {
                    if (i < parts.size) {
                        map[headers[i]] = parts[i]
                    }
                }
                rawRecords.add(map)
            }
        }

        val detectedColumns = headers.map { h ->
            val samples = rawRecords.mapNotNull { it[h] }.take(5)
            val (inferredType, conf) = inferFieldType(h, samples)
            DetectedColumn(
                rawKey = h,
                normalizedType = inferredType,
                sampleValues = samples,
                confidence = conf
            )
        }

        val mappedRecords = rawRecords.map { rec ->
            val mapped = mutableMapOf<String, String>()
            for (col in detectedColumns) {
                val value = rec[col.rawKey] ?: ""
                if (value.isNotBlank()) {
                    mapped[col.normalizedType] = value
                }
            }
            deriveMissingFields(mapped)
            mapped
        }

        return FileAnalysisResult(
            detectedColumns = detectedColumns,
            records = mappedRecords,
            totalLines = lines.size,
            validRecordCount = mappedRecords.size,
            skippedLinesCount = lines.size - dataLines.size
        )
    }

    private fun splitLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()

        for (ch in line) {
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == delimiter && !inQuotes) {
                result.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(ch)
            }
        }
        result.add(sb.toString().trim())
        return result
    }

    private fun parseJson(text: String): FileAnalysisResult? {
        return try {
            val jsonArray = if (text.startsWith("[")) {
                JSONArray(text)
            } else {
                JSONArray().put(JSONObject(text))
            }

            val rawRecords = mutableListOf<Map<String, String>>()
            val allKeys = LinkedHashSet<String>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val flatMap = mutableMapOf<String, String>()
                flattenJsonObject("", obj, flatMap)
                rawRecords.add(flatMap)
                allKeys.addAll(flatMap.keys)
            }

            val detectedColumns = allKeys.map { key: String ->
                val samples = rawRecords.mapNotNull { it[key] }.take(5)
                val (inferredType, conf) = inferFieldType(key, samples)
                DetectedColumn(
                    rawKey = key,
                    normalizedType = inferredType,
                    sampleValues = samples,
                    confidence = conf
                )
            }

            val mappedRecords = rawRecords.map { rec: Map<String, String> ->
                val mapped = mutableMapOf<String, String>()
                for (col in detectedColumns) {
                    val value = rec[col.rawKey] ?: ""
                    if (value.isNotBlank()) {
                        mapped[col.normalizedType] = value
                        val leaf = col.rawKey.substringAfterLast('.')
                        val (leafType, _) = inferFieldType(leaf, listOf(value))
                        if (leafType.isNotBlank() && !mapped.containsKey(leafType)) {
                            mapped[leafType] = value
                        }
                    }
                }
                deriveMissingFields(mapped)
                mapped
            }

            FileAnalysisResult(
                detectedColumns = detectedColumns,
                records = mappedRecords,
                totalLines = text.lines().size,
                validRecordCount = mappedRecords.size,
                skippedLinesCount = 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun flattenJsonObject(prefix: String, obj: JSONObject, outMap: MutableMap<String, String>) {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
            val value = obj.opt(key)
            if (value is JSONObject) {
                flattenJsonObject(fullKey, value, outMap)
            } else if (value != null && value !is JSONArray) {
                outMap[fullKey] = value.toString()
            }
        }
    }

    private fun parseFreeText(text: String): FileAnalysisResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val rawRecords = mutableListOf<Map<String, String>>()

        val emailFindRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        val phoneFindRegex = Regex("(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}")

        for (line in lines) {
            val map = mutableMapOf<String, String>()
            emailFindRegex.find(line)?.let {
                map["email"] = it.value
            }
            phoneFindRegex.find(line)?.let {
                val candidate = it.value.trim()
                if (candidate.length in 7..25 && candidate.any { c -> c.isDigit() }) {
                    map["phone"] = candidate
                }
            }
            if (map.isNotEmpty()) {
                deriveMissingFields(map)
                rawRecords.add(map)
            }
        }

        val allKeys = rawRecords.flatMap { it.keys }.distinct()
        val detectedColumns = allKeys.map { key ->
            DetectedColumn(
                rawKey = key,
                normalizedType = key,
                sampleValues = rawRecords.mapNotNull { it[key] }.take(5),
                confidence = 0.9f
            )
        }

        return FileAnalysisResult(
            detectedColumns = detectedColumns,
            records = rawRecords,
            totalLines = lines.size,
            validRecordCount = rawRecords.size,
            skippedLinesCount = lines.size - rawRecords.size
        )
    }
}
