package com.example.automation

enum class FieldType(val key: String, val label: String) {
    AUTO("auto", "Auto"),
    TEXT("text", "Text"),
    FULL_NAME("full_name", "Full name"),
    EMAIL("email", "Email"),
    PHONE("phone", "Phone number"),
    USERNAME("username", "Username"),
    PASSWORD("password", "Password"),
    DATE_OF_BIRTH("date_of_birth", "Date of birth"),
    COUNTRY("country", "Country"),
    ADDRESS("address_line_1", "Address"),
    CITY("city", "City"),
    STATE("state", "State"),
    ZIP("zip_code", "ZIP code");

    companion object {
        fun fromKey(k: String): FieldType? = entries.firstOrNull { it.key == k }

        fun infer(name: String): FieldType {
            val n = name.lowercase().replace('_', ' ').trim()
            return when {
                n.isEmpty() -> AUTO
                n.contains("email") || n == "e mail" || n == "mail" -> EMAIL
                n.contains("phone") || n.contains("mobile") || n == "tel" || n == "cell" -> PHONE
                n.contains("username") || n == "user" || n == "login" -> USERNAME
                n.contains("password") || n == "pass" || n == "pwd" -> PASSWORD
                n.contains("full") && n.contains("name") -> FULL_NAME
                n == "name" -> FULL_NAME
                n.contains("birth") || n == "dob" -> DATE_OF_BIRTH
                n.contains("country") || n.contains("nation") -> COUNTRY
                n.contains("address") || n == "street" -> ADDRESS
                n.contains("city") || n.contains("town") -> CITY
                n.contains("state") || n.contains("province") -> STATE
                n.contains("zip") || n.contains("postal") || n.contains("postcode") -> ZIP
                else -> TEXT
            }
        }
    }
}

object CountryList {
    val ALL: List<String> = listOf(
        "Afghanistan", "Albania", "Algeria", "Argentina", "Armenia", "Australia", "Austria",
        "Azerbaijan", "Bahrain", "Bangladesh", "Belarus", "Belgium", "Bolivia",
        "Bosnia and Herzegovina", "Brazil", "Bulgaria", "Cambodia", "Cameroon", "Canada",
        "Chile", "China", "Colombia", "Costa Rica", "Croatia", "Cuba", "Cyprus",
        "Czech Republic", "Denmark", "Dominican Republic", "Ecuador", "Egypt", "El Salvador",
        "Estonia", "Ethiopia", "Finland", "France", "Georgia", "Germany", "Ghana", "Greece",
        "Guatemala", "Honduras", "Hong Kong", "Hungary", "Iceland", "India", "Indonesia",
        "Iran", "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan",
        "Kazakhstan", "Kenya", "Kuwait", "Latvia", "Lebanon", "Libya", "Lithuania",
        "Luxembourg", "Malaysia", "Maldives", "Malta", "Mexico", "Moldova", "Monaco",
        "Mongolia", "Montenegro", "Morocco", "Myanmar", "Nepal", "Netherlands",
        "New Zealand", "Nicaragua", "Nigeria", "North Macedonia", "Norway", "Oman",
        "Pakistan", "Panama", "Paraguay", "Peru", "Philippines", "Poland", "Portugal",
        "Qatar", "Romania", "Russia", "Saudi Arabia", "Serbia", "Singapore", "Slovakia",
        "Slovenia", "South Africa", "South Korea", "Spain", "Sri Lanka", "Sudan",
        "Sweden", "Switzerland", "Syria", "Taiwan", "Tanzania", "Thailand", "Tunisia",
        "Turkey", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom",
        "United States", "Uruguay", "Uzbekistan", "Venezuela", "Vietnam", "Yemen",
        "Zambia", "Zimbabwe"
    )
}