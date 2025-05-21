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
import com.geeksville.mesh.ui.components.NfcSigningDialog

@Composable
internal fun TestPanel(
    viewModel: TestViewModel = hiltViewModel(),
) {
    var showNfcDialog by remember { mutableStateOf(false) }
    val testResults by viewModel.testResults.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { showNfcDialog = true },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(text = stringResource(R.string.test_nfc_signing))
        }

        LazyColumn {
            items(testResults) { result ->
                TestResultCard(result)
            }
        }
    }

    if (showNfcDialog) {
        NfcSigningDialog(
            onDismiss = { showNfcDialog = false },
            onSigningComplete = { signature ->
                viewModel.addTestResult("NFC Signing", "Success", signature)
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

@Composable
fun TestMenuActions(
    viewModel: TestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = viewModel::clearTestResults,
        modifier = modifier,
    ) {
        Text(text = stringResource(R.string.clear))
    }
} 