package com.sergio.myapplication.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun outlinedTextFieldColors(
    focusedBorderColor: Color = Color(0xFF5b9bd5),
    unfocusedBorderColor: Color = Color(0xFF1e3a5f)
) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedLabelColor = Color(0xFF5b9bd5),
    unfocusedLabelColor = Color(0xFF4a7ab5),
    cursorColor = Color(0xFF5b9bd5),
    focusedTextColor = Color(0xFFe8f4ff),
    unfocusedTextColor = Color(0xFF8ab8e8),
    focusedLeadingIconColor = Color(0xFF5b9bd5),
    unfocusedLeadingIconColor = Color(0xFF4a7ab5),
    focusedTrailingIconColor = Color(0xFF5b9bd5),
    unfocusedTrailingIconColor = Color(0xFF4a7ab5)
)