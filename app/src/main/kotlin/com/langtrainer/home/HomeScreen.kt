package com.langtrainer.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.ak65477.kanjitrainer.R

@Composable
fun HomeScreen(
    onStartKanjiSrs: () -> Unit,
    onStartNameKanji: () -> Unit,
    onOpenKanjiCollection: () -> Unit,
    onOpenSources: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = stringResource(R.string.home_mastered_kanji),
                value = state.masteredKanji.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.home_today_reviews),
                value = stringResource(R.string.home_review_count, state.todayReviews),
                helper = stringResource(R.string.home_minute_count, state.todayMinutes),
                modifier = Modifier.weight(1f),
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.home_srs_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.home_srs_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Button(
                    onClick = onStartKanjiSrs,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text(stringResource(R.string.home_general_session), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = onStartNameKanji,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null)
                    Text(stringResource(R.string.home_name_kanji), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = onOpenKanjiCollection,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.ViewModule, contentDescription = null)
                    Text(stringResource(R.string.home_collection), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        OutlinedButton(
            onClick = onOpenSources,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Text(stringResource(R.string.sources_title), modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (helper != null) {
                Text(
                    helper,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
