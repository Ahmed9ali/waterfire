package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.browser.GeolocationPermissionState
import com.example.browser.JsDialogState
import com.example.browser.LinkContextMenuState
import com.example.browser.SslErrorState
import com.example.browser.WebPermissionState
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.WarningYellow

@Composable
fun JsDialogRenderer(
    dialogState: JsDialogState?,
    onDismiss: () -> Unit
) {
    if (dialogState == null) return

    when (dialogState) {
        is JsDialogState.Alert -> {
            AlertDialog(
                onDismissRequest = {
                    dialogState.result.cancel()
                    onDismiss()
                },
                title = { Text(text = "Page Alert", fontWeight = FontWeight.Bold) },
                text = { Text(text = dialogState.message) },
                confirmButton = {
                    Button(
                        onClick = {
                            dialogState.result.confirm()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        is JsDialogState.Confirm -> {
            AlertDialog(
                onDismissRequest = {
                    dialogState.result.cancel()
                    onDismiss()
                },
                title = { Text(text = "Confirm", fontWeight = FontWeight.Bold) },
                text = { Text(text = dialogState.message) },
                confirmButton = {
                    Button(
                        onClick = {
                            dialogState.result.confirm()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            dialogState.result.cancel()
                            onDismiss()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        is JsDialogState.Prompt -> {
            var promptInput by remember { mutableStateOf(dialogState.defaultValue) }
            AlertDialog(
                onDismissRequest = {
                    dialogState.result.cancel()
                    onDismiss()
                },
                title = { Text(text = "Prompt", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(text = dialogState.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dialogState.result.confirm(promptInput)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            dialogState.result.cancel()
                            onDismiss()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SslErrorDialog(
    sslErrorState: SslErrorState?,
    onGoBack: () -> Unit,
    onProceedAnyway: () -> Unit
) {
    if (sslErrorState == null) return

    AlertDialog(
        onDismissRequest = onGoBack,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning),
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Security Warning",
                fontWeight = FontWeight.Bold,
                color = DangerRed
            )
        },
        text = {
            Text(
                text = "The certificate for ${sslErrorState.url} could not be verified. Continuing may expose your private information."
            )
        },
        confirmButton = {
            Button(
                onClick = onGoBack,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                Text("Go Back (Recommended)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onProceedAnyway) {
                Text("Proceed anyway (unsafe)", color = DangerRed)
            }
        }
    )
}

@Composable
fun WebPermissionDialog(
    permissionState: WebPermissionState?,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    if (permissionState == null) return

    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text(text = "Permission Request", fontWeight = FontWeight.Bold) },
        text = {
            Text(text = "${permissionState.origin} requests access to: ${permissionState.resources.joinToString()}")
        },
        confirmButton = {
            Button(onClick = onAllow, colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Block")
            }
        }
    )
}

@Composable
fun GeolocationDialog(
    geoState: GeolocationPermissionState?,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    if (geoState == null) return

    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text(text = "Location Access", fontWeight = FontWeight.Bold) },
        text = {
            Text(text = "${geoState.origin} wants to know your location.")
        },
        confirmButton = {
            Button(onClick = onAllow, colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Block")
            }
        }
    )
}

@Composable
fun LinkContextMenuSheet(
    state: LinkContextMenuState?,
    onOpenInNewTab: (String) -> Unit,
    onOpenInBackgroundTab: (String) -> Unit,
    onOpenInPrivateTab: (String) -> Unit,
    onCopyLink: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onDownloadImage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    CustomBottomSheet(
        visible = state != null,
        onDismissRequest = onDismiss
    ) {
        if (state == null) return@CustomBottomSheet

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = state.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            ContextMenuItem(icon = R.drawable.ic_add, title = "Open in new tab") {
                onDismiss()
                onOpenInNewTab(state.url)
            }
            ContextMenuItem(icon = R.drawable.ic_desktop, title = "Open in background tab") {
                onDismiss()
                onOpenInBackgroundTab(state.url)
            }
            ContextMenuItem(icon = R.drawable.ic_shield, title = "Open in private tab") {
                onDismiss()
                onOpenInPrivateTab(state.url)
            }
            ContextMenuItem(icon = R.drawable.ic_edit, title = "Copy link address") {
                onDismiss()
                onCopyLink(state.url)
            }
            ContextMenuItem(icon = R.drawable.ic_share, title = "Share link") {
                onDismiss()
                onShareLink(state.url)
            }
            if (state.isImage && state.imageUrl != null) {
                ContextMenuItem(icon = R.drawable.ic_download, title = "Download image") {
                    onDismiss()
                    onDownloadImage(state.imageUrl)
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: Int,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
