package com.cocido.morfipolo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cocido.morfipolo.ui.theme.*

@Composable
fun MorfiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MorfiTypography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MorfiTypography.bodyLarge.copy(color = MorfiGrayMedium.copy(alpha = 0.6f))
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = MorfiTypography.bodyLarge.copy(color = MorfiGrayDark),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MorfiGrayLight,
                unfocusedContainerColor = MorfiGrayLight,
                disabledContainerColor = MorfiGrayLight,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MorfiOrange
            )
        )
    }
}
