package net.akehurst.datastore.gui.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.akehurst.datastore.gui.compose.EditorStrings
import net.akehurst.datastore.gui.compose.TestResult

/**
 * A button that runs 'test' (supplied by the host application), with a progress indicator while it runs,
 * a cancel button, and the result below it. Disabled when 'test' is null or 'enabled' is false.
 * 'resetKey' clears the result when it changes (e.g. when the configuration changes).
 */
@Composable
internal fun TestButton(
    label: String,
    test: (suspend () -> TestResult)?,
    enabled: Boolean,
    strings: EditorStrings,
    resetKey: Any?,
) {
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var result by remember(resetKey) { mutableStateOf<TestResult?>(null) }
    val running = job?.isActive == true
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    val t = test ?: return@OutlinedButton
                    job?.cancel()
                    result = null
                    job = scope.launch {
                        result = try {
                            t()
                        } catch (ex: CancellationException) {
                            throw ex
                        } catch (ex: Throwable) {
                            TestResult.Failure(ex.message ?: ex::class.simpleName ?: "error", ex)
                        } finally {
                            job = null
                        }
                    }
                },
                enabled = enabled && null != test && !running,
            ) { Text(label) }
            if (running) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(4.dp))
                Text(strings.testing, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { job?.cancel(); job = null }) { Text(strings.cancel) }
            }
        }
        when (val r = result) {
            null -> Unit
            is TestResult.Success -> Text(
                listOfNotNull(strings.testPassed, r.message).joinToString(": "),
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall
            )

            is TestResult.Failure -> Text(
                "${strings.testFailed}: ${r.message}",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
