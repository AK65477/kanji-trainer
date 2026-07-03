package com.langtrainer.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Cross-device progress backup: export the learner's progress to a file (share it
 * to another device via Quick Share / Drive / etc.), and import it back — which
 * overwrites this device's progress after an explicit confirmation.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportTo(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.stageImportFrom(uri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "진도 백업 · 기기 간 이동",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "학습 진도(복습 일정 · 숙련도 · 기록)를 파일로 내보내 다른 기기로 옮기거나, 그 파일을 가져와 이 기기에 복원할 수 있습니다. " +
                "카드 자체는 앱에 들어 있으므로 진도만 옮깁니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { exportLauncher.launch("kanji-trainer-progress.json") },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("진도 내보내기 (파일로 저장)") }

        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json")) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("진도 가져오기 (이 기기 덮어쓰기)") }

        Text(
            "저장한 파일은 삼성 Quick Share, 드라이브, 파일 관리자 등으로 다른 기기에 옮긴 뒤 그 기기에서 가져오면 됩니다. " +
                "같은 버전의 앱끼리만 가져올 수 있습니다.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.busy) {
            CircularProgressIndicator()
        }
        state.message?.let { msg ->
            Text(msg, style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로")
        }
    }

    if (state.pendingImportText != null) {
        val warning = state.importWarning
        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            title = {
                Text(if (warning != null) "정말 덮어쓸까요? (최신 진도 손실 위험)" else "이 기기의 진도를 덮어쓸까요?")
            },
            text = {
                val base = "가져오면 이 기기의 현재 학습 진도가 백업 파일 내용으로 완전히 교체됩니다. 되돌릴 수 없습니다."
                Text(if (warning != null) "$warning\n\n$base" else base)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport() }) {
                    Text(if (warning != null) "그래도 덮어쓰기" else "덮어쓰기")
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.cancelImport() }) { Text("취소") } },
        )
    }
}
