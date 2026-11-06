package net.akehurst.datastore.gui.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import net.akehurst.datastore.api.DsConfigParameterChoice
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.datastore.api.DsConfigParameterGroup
import net.akehurst.datastore.api.DsConfigParameterInfo
import net.akehurst.datastore.gui.compose.ConfigurationDraft
import net.akehurst.datastore.gui.compose.EditorCustomisation
import net.akehurst.datastore.gui.compose.ParameterDraft
import net.akehurst.datastore.gui.compose.ParameterFieldContext
import net.akehurst.datastore.gui.compose.ParameterKind
import net.akehurst.datastore.gui.compose.ValueMode
import net.akehurst.datastore.gui.compose.kind

/**
 * A form generated from the parameter set of 'draft': a field for each parameter, a titled section for each group,
 * and radio buttons for the alternatives of each choice, followed by the chosen alternative's parameters.
 *
 * @param errors the error of each parameter, by parameter identity
 */
@Composable
fun ParameterForm(
    draft: ConfigurationDraft,
    errors: Map<String, String>,
    secretSourceIds: List<String>,
    customisation: EditorCustomisation = EditorCustomisation.Default,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ParameterInfos(draft, draft.parameterSet.parameterInfo, "", errors, secretSourceIds, customisation, enabled)
    }
}

@Composable
private fun ParameterInfos(
    draft: ConfigurationDraft,
    infos: List<DsConfigParameterInfo>,
    prefix: String,
    errors: Map<String, String>,
    secretSourceIds: List<String>,
    customisation: EditorCustomisation,
    enabled: Boolean,
) {
    infos.forEachIndexed { i, info ->
        val path = prefix + i
        when (info) {
            is DsConfigParameterDefinition -> draft.parameter[info.identity]?.let { pd ->
                ParameterField(draft, pd, errors[info.identity], secretSourceIds, customisation, enabled)
            }

            is DsConfigParameterGroup -> OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(info.name, style = MaterialTheme.typography.titleSmall)
                    ParameterInfos(draft, info.parameterInfo, "$path/", errors, secretSourceIds, customisation, enabled)
                }
            }

            is DsConfigParameterChoice -> OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chosen = draft.chosenAlternative(path, info)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        info.alternative.keys.forEach { alt ->
                            Row(
                                Modifier.selectable(selected = alt == chosen, enabled = enabled, role = Role.RadioButton) { draft.choose(path, alt) }.padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = alt == chosen, onClick = null, enabled = enabled)
                                Spacer(Modifier.width(4.dp))
                                Text(alt)
                            }
                        }
                    }
                    val ai = info.alternative.keys.indexOf(chosen)
                    ParameterInfos(draft, info.alternative[chosen]!!.parameterInfo, "$path/$ai/", errors, secretSourceIds, customisation, enabled)
                }
            }
        }
    }
}

/** The field of one parameter: the host's renderer if it gives one, otherwise a field for its type. */
@Composable
fun ParameterField(
    draft: ConfigurationDraft,
    parameter: ParameterDraft,
    error: String?,
    secretSourceIds: List<String>,
    customisation: EditorCustomisation = EditorCustomisation.Default,
    enabled: Boolean = true,
) {
    val def = parameter.definition
    val strings = customisation.strings
    val custom = customisation.rendererFor(draft.providerIdentity, def)
    val label: @Composable () -> Unit = {
        LabelWithHelp(if (def.optional) def.name else "${def.name} (${strings.required})", def.description, strings.help)
    }
    Column(Modifier.fillMaxWidth()) {
        if (def.sensitive) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModeOption(strings.value, parameter.mode == ValueMode.VALUE, enabled) { parameter.mode = ValueMode.VALUE }
                ModeOption(strings.secretReference, parameter.mode == ValueMode.SECRET_REFERENCE, enabled) { parameter.mode = ValueMode.SECRET_REFERENCE }
            }
        }
        when {
            def.sensitive && parameter.mode == ValueMode.SECRET_REFERENCE -> SecretReferenceFields(parameter, error, secretSourceIds, customisation, enabled, label)
            null != custom -> custom(ParameterFieldContext(draft.providerIdentity, def, parameter, error, strings))
            else -> ValueField(def, parameter, error, customisation, enabled, label)
        }
    }
}

@Composable
private fun ModeOption(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick).padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ValueField(
    def: DsConfigParameterDefinition,
    parameter: ParameterDraft,
    error: String?,
    customisation: EditorCustomisation,
    enabled: Boolean,
    label: @Composable () -> Unit,
) {
    val strings = customisation.strings
    when (val kind = def.kind) {
        ParameterKind.BOOLEAN -> Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                label()
                Spacer(Modifier.width(12.dp))
                Switch(checked = parameter.boolean ?: false, onCheckedChange = { parameter.boolean = it }, enabled = enabled)
                if (def.optional) {
                    Spacer(Modifier.width(8.dp))
                    if (null == parameter.boolean) {
                        Text(strings.notSet, style = MaterialTheme.typography.bodySmall)
                    } else {
                        TextButton(onClick = { parameter.boolean = null }, enabled = enabled) { Text(strings.clear) }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }

        else -> {
            var shown by remember { mutableStateOf(false) }
            TextFieldWithError(
                value = parameter.text,
                onValueChange = { if (ParameterDraft.accepts(kind, it)) parameter.text = it },
                label = label,
                error = error,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                visualTransformation = if (def.sensitive && !shown) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = when (kind) {
                    ParameterKind.INTEGER, ParameterKind.REAL -> KeyboardOptions(keyboardType = KeyboardType.Number)
                    else -> if (def.sensitive) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default
                },
                trailingIcon = if (def.sensitive) {
                    { TextButton(onClick = { shown = !shown }) { Text(if (shown) strings.hide else strings.show) } }
                } else null,
            )
        }
    }
}

@Composable
private fun SecretReferenceFields(
    parameter: ParameterDraft,
    error: String?,
    secretSourceIds: List<String>,
    customisation: EditorCustomisation,
    enabled: Boolean,
    label: @Composable () -> Unit,
) {
    val strings = customisation.strings
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        label()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            SuggestionTextField(
                value = parameter.secretSource,
                onValueChange = { parameter.secretSource = it },
                label = { Text(strings.secretSource) },
                suggestions = secretSourceIds,
                error = null,
                chooseLabel = strings.choose,
                modifier = Modifier.weight(1f),
                enabled = enabled,
            )
            TextFieldWithError(
                value = parameter.secretKey,
                onValueChange = { parameter.secretKey = it },
                label = { Text(strings.secretKey) },
                error = null,
                modifier = Modifier.weight(1f),
                enabled = enabled,
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
