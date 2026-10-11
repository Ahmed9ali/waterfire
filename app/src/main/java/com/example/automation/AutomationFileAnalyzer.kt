package com.example.automation

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

enum class ContentFormat(val displayName: String) {
    JSON("JSON"),
    NDJSON("JSON Lines"),
    KEY_VALUE("Key-Value"),
    DELIMITED("CSV / Delimited"),
    HTML_TABLE("HTML Table"),
    XML("XML"),
    FREE_TEXT("Free Text"),
    UNKNOWN("Unknown")
}

data class SmartAnalysisResult(
    val format: ContentFormat,
    val formatConfidence: Float,
    val columns: List<DetectedColumn>,
    val records: List<Map<String, String>>,
    val totalLines: Int,
    val validRecordCount: Int,
    val skippedLinesCount: Int,
    val needsUserInput: Boolean = false,
    val userPrompt: String? = null,
    val suggestedName: String? = null
) {
    fun toLegacy() = FileAnalysisResult(
        detectedColumns = columns,
        records = records,
        totalLines = totalLines,
        validRecordCount = validRecordCount,
        skippedLinesCount = skippedLinesCount
    )
}

object AutomationFileAnalyzer {

    private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    private val PHONE_REGEX = Regex("^(\\+?\\d{1,4}[\\s.-]?)?\\(?\\d{2,4}\\)?[\\s.-]?\\d{3,4}[\\s.-]?\\d{3,4}\$")
    private val URL_REGEX = Regex("^https?://[A-Za-z0-9.-]+(/.*)?\$")
    private val DATE_ISO_REGEX = Regex("^\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\$")
    private val DATE_EU_REGEX = Regex("^\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4}\$")
    private val ZIP_REGEX = Regex("^\\d{4,6}(-\\d{3,4})?\$")

    private val EMAIL_FIND = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val URL_FIND = Regex("https?://[^\\s]+")
    private val HANDLE_FIND = Regex("(?<![\\w])@([A-Za-z0-9_.]{2,30})")
    private val DATE_FIND = Regex("\\b(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4})\\b")
    private val PHONE_FIND = Regex("(\\+\\d{1,3}[\\s.-]?)?(\\(?\\d{2,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}")
    private val PROPER_NAME_FIND = Regex("\\b([A-Z][a-z]{1,20})(\\s+[A-Z][a-z]{1,20}){1,3}\\b")

    private val NAME_STOPWORDS = setOf(
        "The", "A", "An", "And", "Or", "For", "To", "From", "With", "Of", "In", "On", "At", "By",
        "Is", "Are", "Was", "Were", "Be", "This", "That", "These", "Those", "It", "We", "You",
        "He", "She", "They", "Note", "Re", "Fwd"
    )

    private val SYNONYMS: Map<String, List<String>> = mapOf(
        "first_name" to listOf("first name", "firstname", "fname", "given name", "first"),
        "last_name" to listOf("last name", "lastname", "lname", "surname", "family name", "last"),
        "full_name" to listOf("full name", "fullname", "name", "customer name", "contact name", "client name", "your name", "display name"),
        "middle_name" to listOf("middle name", "middlename", "mname"),
        "username" to listOf("username", "user name", "userid", "user id", "login", "handle", "nickname", "account", "screen name"),
        "email" to listOf("email", "e mail", "mail", "email address", "user email", "login email", "e-mail"),
        "confirm_email" to listOf("confirm email", "verify email", "repeat email", "re-enter email"),
        "password" to listOf("password", "pass", "pwd", "login password", "secret", "passcode"),
        "confirm_password" to listOf("confirm password", "verify password", "repeat password", "re-enter password"),
        "phone" to listOf("phone", "mobile", "telephone", "tel", "cell", "phone number", "mobile number", "contact number", "whatsapp", "msisdn"),
        "country_code" to listOf("country code", "dial code", "calling code"),
        "date_of_birth" to listOf("date of birth", "dob", "birthday", "birth date"),
        "age" to listOf("age", "years old"),
        "gender" to listOf("gender", "sex"),
        "title" to listOf("title", "prefix", "salutation"),
        "company" to listOf("company", "organization", "organisation", "business", "employer", "firm"),
        "job_title" to listOf("job title", "position", "role", "occupation", "designation"),
        "website" to listOf("website", "url", "site", "homepage", "web address"),
        "address_line_1" to listOf("address line 1", "address 1", "street address", "street", "address"),
        "address_line_2" to listOf("address line 2", "address 2", "apt", "suite", "unit", "building", "apartment"),
        "city" to listOf("city", "town", "locality", "municipality"),
        "state" to listOf("state", "province", "region", "county"),
        "zip_code" to listOf("zip code", "zip", "postal code", "postcode", "pin code"),
        "country" to listOf("country", "nation"),
        "subject" to listOf("subject", "topic", "heading"),
        "message" to listOf("message", "comment", "feedback", "inquiry", "description", "details", "notes", "body"),
        "rating" to listOf("rating", "score", "stars"),
        "security_question" to listOf("security question", "secret question", "recovery question"),
        "security_answer" to listOf("security answer", "secret answer", "recovery answer", "answer"),
        "amount" to listOf("amount", "price", "total", "cost", "value"),
        "id_number" to listOf("id", "id number", "passport", "ssn", "aadhaar", "pan"),
        "otp" to listOf("otp", "one time password", "verification code", "token", "pin")
    )

    // ---------------- Binary / encoding ----------------

    fun isBinaryFile(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val n = minOf(bytes.size, 1024)
        var nulls = 0
        for (i in 0 until n) if (bytes[i] == 0.toByte()) nulls++
        return (nulls.toFloat() / n) > 0.05f
    }

    fun detectEncodingAndRead(bytes: ByteArray): String {
        if (bytes.size > MAX_FILE_SIZE_BYTES || isBinaryFile(bytes)) return ""
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte())
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte())
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte())
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        return try {
            val s = String(bytes, StandardCharsets.UTF_8)
            if (s.contains('\uFFFD')) String(bytes, StandardCharsets.ISO_8859_1) else s
        } catch (_: Exception) {
            String(bytes, StandardCharsets.ISO_8859_1)
        }
    }

    // ---------------- Public API ----------------

    fun analyzeText(content: String): FileAnalysisResult = analyzeSmart(content).toLegacy()

    fun analyzeSmart(content: String, hint: ContentFormat? = null): SmartAnalysisResult {
        val text = content.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (text.isEmpty()) return SmartAnalysisResult(
            format = ContentFormat.UNKNOWN,
            formatConfidence = 0f,
            columns = emptyList(),
            records = emptyList(),
            totalLines = 0,
            validRecordCount = 0,
            skippedLinesCount = 0,
            needsUserInput = true,
            userPrompt = "Content is empty."
        )

        val candidates = mutableListOf<Triple<ContentFormat, Float, SmartAnalysisResult>>()

        if (text.startsWith("{") || text.startsWith("[")) {
            parseJson(text)?.let { candidates += Triple(ContentFormat.JSON, 0.95f, it) }
        }
        if (text.lines().count { it.trim().startsWith("{") && it.trim().endsWith("}") } >= 2) {
            parseNdjson(text)?.let { candidates += Triple(ContentFormat.NDJSON, 0.9f, it) }
        }
        if (text.contains("<table", ignoreCase = true)) {
            parseHtmlTable(text)?.let { candidates += Triple(ContentFormat.HTML_TABLE, 0.9f, it) }
        }
        if (text.startsWith("<") && !text.contains("<table", true)) {
            parseXml(text)?.let { candidates += Triple(ContentFormat.XML, 0.75f, it) }
        }
        if (isKeyValueBlockFormat(text)) {
            parseKv(text)?.let { candidates += Triple(ContentFormat.KEY_VALUE, 0.85f, it) }
        }
        parseDelimited(text)?.let { candidates += Triple(ContentFormat.DELIMITED, 0.88f, it) }
        candidates += Triple(ContentFormat.FREE_TEXT, 0.4f, parseFreeText(text))

        // Always non-empty because we appended FREE_TEXT above, but keep the
        // type checker happy with an explicit non-nullable type.
        val best: Triple<ContentFormat, Float, SmartAnalysisResult> =
            if (hint != null) {
                candidates.firstOrNull { it.first == hint }
                    ?: candidates.maxByOrNull { it.second }
                    ?: candidates.last()
            } else {
                candidates.maxByOrNull { it.second }
                    ?: candidates.last()
            }

        val fmt = best.first
        val conf = best.second
        val base = best.third

        val weak = base.validRecordCount == 0 ||
                base.columns.isEmpty() ||
                (fmt == ContentFormat.FREE_TEXT && base.columns.size < 2)

        return SmartAnalysisResult(
            format = fmt,
            formatConfidence = conf,
            columns = base.columns,
            records = base.records,
            totalLines = base.totalLines,
            validRecordCount = base.validRecordCount,
            skippedLinesCount = base.skippedLinesCount,
            needsUserInput = weak,
            userPrompt = if (weak) "We couldn't detect the structure with confidence. Confirm the format below."
            else null,
            suggestedName = "Imported ${fmt.displayName} ${System.currentTimeMillis() % 100000}"
        )
    }

    // ---------------- Field inference ----------------

    private fun normalizeLabel(label: String): String =
        label.lowercase()
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()

    fun inferFieldType(label: String, samples: List<String>): Pair<String, Float> {
        val norm = normalizeLabel(label)
        if (norm.isNotBlank()) {
            for ((field, syns) in SYNONYMS) if (syns.any { norm == it }) return field to 0.97f
            for ((field, syns) in SYNONYMS) if (syns.any { it.length >= 4 && norm.contains(it) }) return field to 0.82f
        }
        var email = 0; var phone = 0; var url = 0; var date = 0; var zip = 0
        val valid = samples.filter { it.isNotBlank() }
        for (v in valid) {
            when {
                EMAIL_REGEX.matches(v) -> email++
                URL_REGEX.matches(v) -> url++
                DATE_ISO_REGEX.matches(v) || DATE_EU_REGEX.matches(v) -> date++
                PHONE_REGEX.matches(v) && v.length in 7..18 -> phone++
                ZIP_REGEX.matches(v) -> zip++
            }
        }
        val total = valid.size.coerceAtLeast(1).toFloat()
        if (email / total >= 0.5f) return "email" to 0.9f
        if (url / total >= 0.5f) return "website" to 0.85f
        if (date / total >= 0.5f) return "date_of_birth" to 0.8f
        if (phone / total >= 0.5f) return "phone" to 0.8f
        if (zip / total >= 0.6f) return "zip_code" to 0.7f
        return (norm.replace(' ', '_').ifBlank { "custom" }) to 0.5f
    }

    fun deriveMissingFields(record: MutableMap<String, String>) {
        if (record["full_name"].isNullOrBlank()) {
            val f = record["first_name"] ?: ""
            val l = record["last_name"] ?: ""
            if (f.isNotBlank() || l.isNotBlank()) record["full_name"] = "$f $l".trim()
        }
        record["full_name"]?.takeIf { it.isNotBlank() }?.let { full ->
            if (record["first_name"].isNullOrBlank() && record["last_name"].isNullOrBlank()) {
                val p = full.trim().split(Regex("\\s+"), 2)
                record["first_name"] = p.getOrNull(0) ?: ""
                if (p.size > 1) record["last_name"] = p[1]
            }
        }
        record["email"]?.let { if (record["confirm_email"].isNullOrBlank()) record["confirm_email"] = it }
        record["password"]?.let { if (record["confirm_password"].isNullOrBlank()) record["confirm_password"] = it }
    }

    // ---------------- Helpers ----------------

    private fun dedupe(records: List<Map<String, String>>): List<Map<String, String>> {
        val seen = HashSet<String>()
        return records.filter { r ->
            val k = r.entries.sortedBy { it.key }.joinToString("||") { "${it.key}=${it.value.trim()}" }
            k.isNotBlank() && seen.add(k)
        }
    }

    private fun buildResult(
        rawRecords: List<MutableMap<String, String>>,
        keys: Collection<String>,
        text: String
    ): SmartAnalysisResult {
        val cols = keys.map { k ->
            val samples = rawRecords.mapNotNull { it[k] }.take(5)
            val (type, conf) = inferFieldType(k, samples)
            DetectedColumn(rawKey = k, normalizedType = type, sampleValues = samples, confidence = conf)
        }
        val mapped = rawRecords.map { rec ->
            val out = mutableMapOf<String, String>()
            for (c in cols) rec[c.rawKey]?.takeIf { it.isNotBlank() }?.let { out[c.normalizedType] = it }
            deriveMissingFields(out)
            out
        }
        val distinct = dedupe(mapped)
        return SmartAnalysisResult(
            format = ContentFormat.UNKNOWN,
            formatConfidence = 0f,
            columns = cols,
            records = distinct,
            totalLines = text.lines().size,
            validRecordCount = distinct.size,
            skippedLinesCount = 0
        )
    }

    private fun stripHtml(s: String) = s.replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ").replace("&amp;", "&")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")

    // ---------------- JSON ----------------

    private fun parseJson(text: String): SmartAnalysisResult? = try {
        val arr = if (text.startsWith("[")) JSONArray(text) else JSONArray().put(JSONObject(text))
        val raw = mutableListOf<MutableMap<String, String>>()
        val keys = LinkedHashSet<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val flat = mutableMapOf<String, String>()
            flattenJson("", obj, flat)
            if (flat.isNotEmpty()) { raw += flat; keys += flat.keys }
        }
        if (raw.isEmpty()) null else buildResult(raw, keys, text)
    } catch (_: Exception) { null }

    private fun flattenJson(prefix: String, obj: JSONObject, out: MutableMap<String, String>) {
        val it = obj.keys()
        while (it.hasNext()) {
            val k = it.next()
            val full = if (prefix.isEmpty()) k else "$prefix.$k"
            val v = obj.opt(k)
            when (v) {
                is JSONObject -> flattenJson(full, v, out)
                is JSONArray -> for (i in 0 until v.length()) {
                    val item = v.opt(i)
                    if (item !is JSONObject && item !is JSONArray) out["$full[$i]"] = item.toString()
                }
                null -> {}
                else -> out[full] = v.toString()
            }
        }
    }

    private fun parseNdjson(text: String): SmartAnalysisResult? = try {
        val raw = mutableListOf<MutableMap<String, String>>()
        val keys = LinkedHashSet<String>()
        for (line in text.lines()) {
            val t = line.trim()
            if (!t.startsWith("{") || !t.endsWith("}")) continue
            val flat = mutableMapOf<String, String>()
            flattenJson("", JSONObject(t), flat)
            if (flat.isNotEmpty()) { raw += flat; keys += flat.keys }
        }
        if (raw.isEmpty()) null else buildResult(raw, keys, text)
    } catch (_: Exception) { null }

    // ---------------- HTML / XML ----------------

    private fun parseHtmlTable(text: String): SmartAnalysisResult? {
        val rows = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .findAll(text).map { it.groupValues[1] }.toList()
        if (rows.size < 2) return null
        val cellRe = Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val rowsCells = rows.map { r -> cellRe.findAll(r).map { stripHtml(it.groupValues[1]).trim() }.toList() }
            .filter { it.isNotEmpty() }
        if (rowsCells.size < 2) return null
        val headers = rowsCells.first().mapIndexed { i, h -> h.ifBlank { "Column ${i + 1}" } }
        val raw = rowsCells.drop(1).map { cells ->
            val m = mutableMapOf<String, String>()
            headers.forEachIndexed { i, h -> if (i < cells.size) m[h] = cells[i] }
            m
        }.toMutableList()
        return buildResult(raw, headers, text)
    }

    private fun parseXml(text: String): SmartAnalysisResult? {
        val tagRe = Regex("<([A-Za-z_][\\w.-]*)(?:\\s[^>]*)?>(.*?)</\\1>", setOf(RegexOption.DOT_MATCHES_ALL))
        val top = tagRe.findAll(text).map { it.groupValues[1] to it.groupValues[2] }.toList()
        if (top.size < 2) return null
        val recordTag = top.groupBy { it.first }.maxByOrNull { it.value.size }?.takeIf { it.value.size >= 2 }?.key ?: return null
        val raw = mutableListOf<MutableMap<String, String>>()
        val keys = LinkedHashSet<String>()
        for ((_, inner) in top.filter { it.first == recordTag }) {
            val m = mutableMapOf<String, String>()
            tagRe.findAll(inner).forEach { mr ->
                val k = mr.groupValues[1]
                m[k] = stripHtml(mr.groupValues[2]).trim()
                keys += k
            }
            if (m.isNotEmpty()) raw += m
        }
        if (raw.isEmpty()) return null
        return buildResult(raw, keys, text)
    }

    // ---------------- KV ----------------

    private fun isKeyValueBlockFormat(text: String): Boolean {
        if (!text.contains(Regex("(\\n\\s*\\n)+|\\n[-=#]{3,}\\n"))) return false
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var count = 0
        for (l in lines.take(20)) {
            val k = l.substringBefore(":").substringBefore("=").trim().lowercase()
            if (k.length in 2..40 && !k.contains("@") &&
                SYNONYMS.values.any { syns -> syns.any { k == it || k.contains(it) } }) count++
        }
        return count >= 2
    }

    private fun parseKv(text: String): SmartAnalysisResult? {
        val blocks = text.split(Regex("(\\n\\s*\\n)+|\\n[-=#]{3,}\\n"))
        val raw = mutableListOf<MutableMap<String, String>>()
        val keys = LinkedHashSet<String>()
        for (b in blocks) {
            val rec = mutableMapOf<String, String>()
            for (line in b.lines()) {
                val t = line.trim()
                if (t.isEmpty() || t.startsWith("#")) continue
                val sep = when {
                    t.contains(":") -> ":"
                    t.contains("=") -> "="
                    t.contains(" - ") -> " - "
                    else -> null
                } ?: continue
                val k = t.substringBefore(sep).trim()
                val v = t.substringAfter(sep).trim()
                if (k.isNotEmpty() && v.isNotEmpty()) { rec[k] = v; keys += k }
            }
            if (rec.isNotEmpty()) raw += rec
        }
        if (raw.isEmpty()) return null
        return buildResult(raw, keys, text)
    }

    // ---------------- Delimited ----------------

    private fun parseDelimited(text: String): SmartAnalysisResult? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return null
        val candidates = listOf(',', ';', '\t', '|', ':')
        var best: Char? = null
        var bestCount = 0
        for (d in candidates) {
            val sample = lines.take(8)
            val count = sample.first().count { it == d }
            if (count == 0) continue
            val consistent = sample.count { it.count { c -> c == d } > 0 }
            if (consistent >= (sample.size / 2).coerceAtLeast(1) && count > bestCount) {
                if (d == ':') {
                    val before = sample.first().substringBefore(':').trim()
                    if (before.contains(' ') && before.split(' ').size > 3) continue
                }
                best = d; bestCount = count
            }
        }
        val delim = best ?: return null
        val firstParts = splitLine(lines[0], delim)
        val hasHeader = firstParts.all { p -> p.any { it.isLetter() } && !EMAIL_REGEX.matches(p) }
        val headers: List<String>
        val dataLines: List<String>
        if (hasHeader && lines.size > 1) { headers = firstParts; dataLines = lines.drop(1) }
        else { headers = (1..firstParts.size).map { "Column $it" }; dataLines = lines }

        val raw = mutableListOf<MutableMap<String, String>>()
        for (line in dataLines) {
            val parts = splitLine(line, delim)
            if (parts.isEmpty()) continue
            val m = mutableMapOf<String, String>()
            headers.forEachIndexed { i, h -> if (i < parts.size) m[h] = parts[i] }
            raw += m
        }
        if (raw.isEmpty()) return null
        return buildResult(raw, headers, text)
    }

    private fun splitLine(line: String, delim: Char): List<String> {
        val out = mutableListOf<String>()
        var inQ = false
        val sb = StringBuilder()
        for (c in line) {
            when {
                c == '"' -> inQ = !inQ
                c == delim && !inQ -> { out += sb.toString().trim(); sb.setLength(0) }
                else -> sb.append(c)
            }
        }
        out += sb.toString().trim()
        return out
    }

    // ---------------- Free text ----------------

    private fun parseFreeText(text: String): SmartAnalysisResult {
        val paragraphs = text.split(Regex("(\\n\\s*\\n)+")).map { it.trim() }.filter { it.isNotEmpty() }
        val raw = mutableListOf<MutableMap<String, String>>()
        if (paragraphs.size >= 2) for (p in paragraphs) raw += extractBlock(p)
        else raw += extractBlock(text)
        raw.removeAll { it.isEmpty() }
        val keys = LinkedHashSet<String>().also { k -> raw.forEach { k += it.keys } }
        return buildResult(raw, keys, text)
    }

    private fun extractBlock(block: String): MutableMap<String, String> {
        val m = mutableMapOf<String, String>()
        EMAIL_FIND.find(block)?.let { m["email"] = it.value }
        URL_FIND.find(block)?.let { m["website"] = it.value }
        HANDLE_FIND.find(block)?.let { m["username"] = it.groupValues[1] }
        DATE_FIND.find(block)?.let { m["date_of_birth"] = it.value }
        PHONE_FIND.findAll(block).map { it.value.trim() }
            .filter { it.length in 7..25 && it.count { c -> c.isDigit() } >= 6 }
            .maxByOrNull { it.length }?.let { m["phone"] = it }
        PROPER_NAME_FIND.findAll(block).map { it.value.trim() }
            .firstOrNull { cand ->
                val words = cand.split(' ')
                words.size in 2..4 && words.none { it in NAME_STOPWORDS }
            }?.let { m["full_name"] = it }
        for (line in block.lines()) {
            val t = line.trim()
            val sep = when { t.contains(":") -> ":"; t.contains("=") -> "="; else -> null } ?: continue
            val k = t.substringBefore(sep).trim()
            val v = t.substringAfter(sep).trim()
            if (k.length in 2..40 && v.isNotEmpty() && !EMAIL_REGEX.matches(k)) {
                val (type, _) = inferFieldType(k, listOf(v))
                if (type != "custom" && !m.containsKey(type)) m[type] = v
            }
        }
        if (m.isNotEmpty()) deriveMissingFields(m)
        return m
    }
}