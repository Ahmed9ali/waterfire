package com.example.ui.components

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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

    // Keep internal text state in sync when not focused or when inputText changes externally.
    // url and isHomePage are keys so the displayed host updates when the page changes.
    LaunchedEffect(inputText, isFocused, url, isHomePage) {
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

    val fieldBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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

                    // Automation Quick Fill: gradient domino mask icon only (no field count)
                    val showAutomation = hasAutomationFields && !isHomePage
                    if (showAutomation) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onAutomationClick() },
                                        onLongPress = { onAutomationLongClick() }
                                    )
                                }
                                .testTag("omnibar_automation_fill_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.DominoMask,
                                contentDescription = "Fill form",
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(24.dp)
                                    // offscreen layer so the gradient only paints where the icon is
                                    .graphicsLayer { alpha = 0.99f }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    Color(0xFFFF3B30), // red
                                                    Color(0xFF2F6BFF), // blue
                                                    Color(0xFF9C3BFF)  // purple
                                                )
                                            ),
                                            blendMode = BlendMode.SrcIn
                                        )
                                    }
                            )
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

        // Thin Firefox-style loading bar directly under top bar.
        // Shows as soon as loading starts (minimum 8%) and animates smoothly.
        if (isLoading) {
            val animatedProgress by animateFloatAsState(
                targetValue = maxOf(progress, 8) / 100f,
                label = "progress_bar_anim"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .height(2.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        )
                    )
            )
        }
    }
}

@Composable
fun SearchSuggestionsList(
    suggestions: List<SearchSuggestion>,
    isDarkTheme: Boolean,
    onSubmit: (String) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    // Explicit colors so the list always follows the app theme
    val containerBg = MaterialTheme.colorScheme.surface
    val itemBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val iconBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val titleColor = MaterialTheme.colorScheme.onSurface
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        modifier = modifier.background(containerBg),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(suggestions) { index, suggestion ->
            val last = suggestions.lastIndex
            val shape = RoundedCornerShape(
                topStart = if (index == 0) 20.dp else 4.dp,
                topEnd = if (index == 0) 20.dp else 4.dp,
                bottomStart = if (index == last) 20.dp else 4.dp,
                bottomEnd = if (index == last) 20.dp else 4.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(itemBg)
                    .clickable {
                        val target = suggestion.url ?: suggestion.title
                        onSubmit(target)
                    }
                    .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = when {
                                suggestion.isRecent -> R.drawable.ic_history
                                suggestion.isBookmarkOrHistory -> R.drawable.ic_bookmark
                                else -> R.drawable.ic_search
                            }
                        ),
                        contentDescription = null,
                        tint = if (suggestion.isBookmarkOrHistory && !suggestion.isRecent) MaterialTheme.colorScheme.primary else subColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.title,
                        color = titleColor,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!suggestion.url.isNullOrBlank()) {
                        Text(
                            text = suggestion.url,
                            color = subColor,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = { onCopy(suggestion.title) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Copy to field",
                        tint = subColor,
                        modifier = Modifier.size(18.dp).rotate(45f)
                    )
                }
            }
        }
    }
}
