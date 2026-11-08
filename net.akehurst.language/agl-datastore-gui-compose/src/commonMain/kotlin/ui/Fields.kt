package net.akehurst.datastore.gui.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.PopupProperties

/** A label followed by a help icon for 'help'. */
@Composable
internal fun LabelWithHelp(label: String, help: String, helpDescription: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        if (help.isNotBlank()) {
            Spacer(Modifier.width(4.dp))
            HelpIcon(help, helpDescription)
        }
    }
}

/** An outlined text field showing 'error' below it. */
@Composable
internal fun TextFieldWithError(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        isError = null != error,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
    )
}

/** A text field that can also be filled by choosing one of 'suggestions' from a menu. */
@Composable
internal fun SuggestionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    suggestions: List<String>,
    error: String?,
    chooseLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** called when a suggestion is chosen from the menu; by default 'onValueChange' */
    onChoose: ((String) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    TextFieldWithError(
        value = value,
        onValueChange = onValueChange,
        label = label,
        error = error,
        modifier = modifier,
        enabled = enabled,
        trailingIcon = if (suggestions.isEmpty()) null else {
            {
                Box {
                    TextButton(onClick = { expanded = true }, enabled = enabled) { Text("▾") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        suggestions.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = { (onChoose ?: onValueChange)(s); expanded = false })
                        }
                    }
                }
            }
        },
    )
}

/**
 * A text field that, while it has the focus, shows 'suggestions' in a menu below it; choosing one sets the value.
 * Free typing still works: the menu does not take the focus.
 */
@Composable
internal fun CompletionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    suggestions: List<String>,
    error: String?,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val shown = suggestions.filter { it != value }.take(MAX_COMPLETIONS)
    Box(modifier) {
        TextFieldWithError(
            value = value,
            onValueChange = { dismissed = false; onValueChange(it) },
            label = label,
            error = error,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
        DropdownMenu(
            expanded = focused && !dismissed && shown.isNotEmpty(),
            onDismissRequest = { dismissed = true },
            properties = PopupProperties(focusable = false),
        ) {
            shown.forEach { s ->
                DropdownMenuItem(text = { Text(s) }, onClick = { onValueChange(s) })
            }
        }
    }
}

private const val MAX_COMPLETIONS = 20

/** A button showing the current choice, which opens a menu of 'options'. */
@Composable
internal fun <T> ChoiceButton(
    current: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onChoose: (T) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, enabled = enabled) { Text("$current ▾") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(optionLabel(o)) }, onClick = { onChoose(o); expanded = false })
            }
        }
    }
}
