package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.Typography

@Composable
fun ProfileCustomizerDialog(
    initialName: String,
    initialRole: String,
    initialAvatar: String,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, avatarKey: String) -> Unit
) {
    var tempName by remember(initialName) { mutableStateOf(initialName) }
    var tempRole by remember(initialRole) { mutableStateOf(initialRole) }
    var selectedAvatarKey by remember(initialAvatar) { mutableStateOf(initialAvatar) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Customize Profile",
                style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Choose Avatar",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GrayText)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarPresets.options.forEach { option ->
                        val isSelected = option.id == selectedAvatarKey
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(option.backgroundColor, CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) BlackInk else BlackInk.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatarKey = option.id },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.name,
                                tint = BlackInk,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = BlackInk,
                        unfocusedIndicatorColor = GrayText,
                        focusedContainerColor = CreamBackground,
                        unfocusedContainerColor = CreamBackground
                    )
                )

                OutlinedTextField(
                    value = tempRole,
                    onValueChange = { tempRole = it },
                    label = { Text("Role / Headline (e.g. Student, Founder)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = BlackInk,
                        unfocusedIndicatorColor = GrayText,
                        focusedContainerColor = CreamBackground,
                        unfocusedContainerColor = CreamBackground
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tempName.isNotBlank()) {
                        onSave(tempName.trim(), tempRole.trim(), selectedAvatarKey)
                    }
                }
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold, color = BlackInk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GrayText)
            }
        },
        containerColor = CreamBackground,
        shape = RoundedCornerShape(20.dp)
    )
}
