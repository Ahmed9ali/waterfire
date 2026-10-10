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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    blockedTrackersCount: Int,
    inputText: String,
    isFocused: Boolean,
    suggestions: List<SearchSuggestion> = emptyList(),
    onInputChanged: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSubmit: (String) -> Unit,
    onShieldClick: () -> Unit,
    hasAutomationFields: Boolean = false,
    automationFieldCount: Int = 0,
    onAutomationClick: () -> Unit = {},
    onAutomationLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val displayCleanDomain = remember(url) {
        if (url.isBlank() || url.startsWith("about:")) {
            ""
        } else {
            try {
                val host = Uri.parse(url).host
                host?.removePrefix("www.") ?: url
            } catch (e: Exception) {
                url
            }
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

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isFocused) {
                        onFocusChanged(true)
                        val initial = if (url.isBlank() || url.startsWith("about:")) "" else url
                        onInputChanged(initial)
                    }
                    try {
                        focusRequester.requestFocus()
                    } catch (e: Exception) {}
                }
                .testTag("omnibar_container"),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            border = if (isFocused) {
                androidx.compose.foundation.BorderStroke(1.5.dp, BrandOrange)
            } else {
                null
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Privacy Shield / Security indicator
                Box(
                    modifier = Modifier
                        .clickable(onClick = onShieldClick)
                        .padding(4.dp)
                        .testTag("privacy_shield_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (blockedTrackersCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SuccessGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shield_protected),
                                contentDescription = "Privacy Shield",
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = blockedTrackersCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    } else if (hasSslError) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = "SSL Warning",
                            tint = WarningYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (isHttps && url.isNotBlank()) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock),
                            contentDescription = "Secure Connection",
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Search",
                            tint = if (isFocused) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Text Field Area
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = if (isFocused) inputText else (if (displayCleanDomain.isNotBlank()) displayCleanDomain else ""),
                        onValueChange = { newValue ->
                            if (!isFocused) {
                                onFocusChanged(true)
                            }
                            onInputChanged(newValue)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused != isFocused) {
                                    onFocusChanged(focusState.isFocused)
                                    if (focusState.isFocused) {
                                        val initialText = if (url.isBlank() || url.startsWith("about:")) "" else url
                                        onInputChanged(initialText)
                                    }
                                }
                            }
                            .testTag("omnibar_input"),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
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
                                onSubmit(inputText)
                            },
                            onGo = {
                                focusManager.clearFocus()
                                onSubmit(inputText)
                            }
                        ),
                        decorationBox = { innerTextField ->
                            val showPlaceholder = if (isFocused) inputText.isEmpty() else displayCleanDomain.isBlank()
                            if (showPlaceholder) {
                                Text(
                                    text = "Search or enter address",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Automation Quick Fill vector icon with count badge
                val showAutomationIcon = hasAutomationFields && displayCleanDomain.isNotBlank()
                if (showAutomationIcon) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
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
                                contentDescription = "Fill form with Automation",
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

                // Clear button when focused and text exists (placed to the right of automation icon)
                if (isFocused && inputText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onInputChanged("")
                            try {
                                focusRequester.requestFocus()
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Clear text",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Progress bar under omnibar
        if (isLoading && progress in 1..99) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress / 100f,
                label = "progress_anim"
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
