package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.BrandOrangeBright
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningYellow
import com.example.viewmodel.SearchSuggestion

@Composable
fun Omnibar(
    url: String,
    isLoading: Boolean,
    progress: Int,
    isHttps: Boolean,
    hasSslError: Boolean,
    isDarkTheme: Boolean,
    isHomePage: Boolean,
    inputText: String,
    isFocused: Boolean,
    searchTriggerTimestamp: Long = 0L,
    onInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSubmit: (String) -> Unit,
    onFireClick: () -> Unit,
    hasAutomationFields: Boolean = false,
    automationFieldCount: Int = 0,
    onAutomationClick: () -> Unit = {},
    onAutomationLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Internal text state with selection tracking
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = inputText, selection = TextRange(inputText.length)))
    }

    // Keep internal text state in sync when not focused or when inputText changes externally
    LaunchedEffect(inputText, isFocused) {
        if (!isFocused) {
            val display = if (isHomePage || url.isBlank() || url.startsWith("about:")) "" else {
                try {
                    val host = Uri.parse(url).host
                    host?.removePrefix("www.") ?: url
                } catch (e: Exception) {
                    url
                }
            }
            textFieldValue = TextFieldValue(text = display, selection = TextRange(display.length))
        } else if (textFieldValue.text != inputText) {
            textFieldValue = TextFieldValue(text = inputText, selection = TextRange(inputText.length))
        }
    }

    // Handle search trigger from bottom navigation bar (selects all text & requests focus)
    LaunchedEffect(searchTriggerTimestamp) {
        if (searchTriggerTimestamp > 0L) {
            onFocusChanged(true)
            val currentVal = textFieldValue.text
            textFieldValue = TextFieldValue(text = currentVal, selection = TextRange(0, currentVal.length))
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {}
        } else {
            focusManager.clearFocus()
        }
    }

    val fieldBg = if (isDarkTheme) Color(0xFF2F2F33) else Color(0xFFF0F0F2)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)
    val placeholderColor = if (isDarkTheme) Color(0xFFA0A0A5) else Color(0xFF7C7C80)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isDarkTheme) Color(0xFF1F1F23) else Color.White)
    ) {
        // Single row top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // One rounded address field (corner 16dp, height 48dp)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isFocused) {
                            onFocusChanged(true)
                            val initial = if (isHomePage || url.isBlank() || url.startsWith("about:")) "" else url
                            onInputChanged(initial)
                            textFieldValue = TextFieldValue(text = initial, selection = TextRange(0, initial.length))
                        }
                        try {
                            focusRequester.requestFocus()
                        } catch (e: Exception) {}
                    }
                    .testTag("omnibar_container"),
                shape = RoundedCornerShape(16.dp),
                color = fieldBg
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leading Icon
                    val leadingIconRes = when {
                        isHomePage || url.isBlank() || url.startsWith("about:") -> R.drawable.ic_search
                        hasSslError -> R.drawable.ic_warning
                        isHttps -> R.drawable.ic_lock
                        else -> R.drawable.ic_warning
                    }
                    val leadingIconTint = when {
                        isHomePage || url.isBlank() || url.startsWith("about:") -> placeholderColor
                        hasSslError -> WarningYellow
                        isHttps -> SuccessGreen
                        else -> WarningYellow
                    }

                    Icon(
                        painter = painterResource(id = leadingIconRes),
                        contentDescription = "Security Status",
                        tint = leadingIconTint,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Text Field Area
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newVal ->
                                textFieldValue = newVal
                                if (!isFocused) {
                                    onFocusChanged(true)
                                }
                                onInputChanged(newVal.text)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused != isFocused) {
                                        onFocusChanged(focusState.isFocused)
                                        if (focusState.isFocused) {
                                            val initialText = if (isHomePage || url.isBlank() || url.startsWith("about:")) "" else url
                                            onInputChanged(initialText)
                                            textFieldValue = TextFieldValue(text = initialText, selection = TextRange(0, initialText.length))
                                        }
                                    }
                                }
                                .testTag("omnibar_input"),
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(BrandOrange),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    onSubmit(textFieldValue.text)
                                },
                                onGo = {
                                    focusManager.clearFocus()
                                    onSubmit(textFieldValue.text)
                                }
                            ),
                            decorationBox = { innerTextField ->
                                val showPlaceholder = if (isFocused) textFieldValue.text.isEmpty() else (isHomePage || url.isBlank())
                                if (showPlaceholder) {
                                    Text(
                                        text = "Search DuckDuckGo or type URL",
                                        color = placeholderColor,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    // Automation Quick Fill pill icon (to the left of X if focused)
                    val showAutomation = hasAutomationFields && !isHomePage
                    if (showAutomation) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onAutomationClick() },
                                        onLongPress = { onAutomationLongClick() }
                                    )
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("omnibar_automation_fill_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_automation),
                                    contentDescription = "Fill form",
                                    tint = BrandOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (automationFieldCount > 0) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BrandOrange.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = automationFieldCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandOrange
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Clear (X) button when focused and text exists
                    if (isFocused && textFieldValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onInputChanged("")
                                textFieldValue = TextFieldValue("", TextRange(0))
                                try {
                                    focusRequester.requestFocus()
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Clear text",
                                tint = placeholderColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // RIGHT SIDE OF THE BAR: ONLY THE FIRE BUTTON (flame icon)
            IconButton(
                onClick = onFireClick,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("browser_fire_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fire),
                    contentDescription = "Fire Button",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Thin Firefox-style loading bar directly under top bar
        if (isLoading && progress in 1..99) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress / 100f,
                label = "progress_bar_anim"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(2.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(BrandOrange, BrandOrangeBright, Color(0xFFFFAB40))
                        )
                    )
            )
        }
    }
}

@Composable
fun SearchSuggestionsList(
    suggestions: List<SearchSuggestion>,
    onSubmit: (String) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
        ) {
            items(suggestions) { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val target = suggestion.url ?: suggestion.title
                            onSubmit(target)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (suggestion.isBookmarkOrHistory) R.drawable.ic_bookmark else R.drawable.ic_search
                        ),
                        contentDescription = null,
                        tint = if (suggestion.isBookmarkOrHistory) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = suggestion.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!suggestion.url.isNullOrBlank()) {
                            Text(
                                text = suggestion.url,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = { onCopy(suggestion.title) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Copy to field",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
            }
        }
    }
}
