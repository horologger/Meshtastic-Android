/*
 * Copyright (c) 2025 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.geeksville.mesh.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geeksville.mesh.R
import com.geeksville.mesh.model.TestResult
import com.geeksville.mesh.model.TestViewModel
import com.geeksville.mesh.model.LogMessageType
import com.geeksville.mesh.ui.components.NfcSigningDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch

@Composable
internal fun TestPanel(
    viewModel: TestViewModel = hiltViewModel(),
) {
    var showNfcDialog by remember { mutableStateOf(false) }
    val testResults by viewModel.testResults.collectAsStateWithLifecycle()
    val logMessages by viewModel.logMessages.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(logMessages.size) {
        coroutineScope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { 
                showNfcDialog = true
                viewModel.addLogMessage("NFC Signing dialog started", LogMessageType.INFO)
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(text = stringResource(R.string.test_nfc_signing))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(testResults) { result ->
                TestResultCard(result)
            }
        }

        // Log Messages Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(top = 16.dp)
        ) {
            Column {
                // Log Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            val allMessages = logMessages.joinToString("\n") { 
                                "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it.timestamp))}] ${it.message}"
                            }
                            clipboardManager.setText(AnnotatedString(allMessages))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy logs",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.clearLogMessages() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear logs",
                            tint = Color.White
                        )
                    }
                }

                // Log Messages
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        logMessages.forEach { logMessage ->
                            Text(
                                text = "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(logMessage.timestamp))}] ${logMessage.message}",
                                color = logMessage.type.color,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNfcDialog) {
        NfcSigningDialog(
            onDismiss = { 
                showNfcDialog = false
                viewModel.addLogMessage("NFC Signing dialog dismissed", LogMessageType.INFO)
            },
            onSigningComplete = { signature ->
                viewModel.addTestResult("NFC Signing", "Success", signature)
                viewModel.addLogMessage("NFC Signing completed: $signature", LogMessageType.SUCCESS)
                showNfcDialog = false
            }
        )
    }
}

@Composable
private fun TestResultCard(result: TestResult) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = result.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = result.status,
                style = MaterialTheme.typography.bodyMedium
            )
            if (result.details.isNotEmpty()) {
                Text(
                    text = result.details,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 