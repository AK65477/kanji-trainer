package com.langtrainer.feature.kanjisrs.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.langtrainer.core.database.entity.SentenceCardEntity
import com.langtrainer.core.model.Bucket
import com.langtrainer.feature.kanjisrs.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanjiDetailScreen(
    onBack: () -> Unit,
    viewModel: KanjiDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.flushMnemonic() }
    }

    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.detail_title), fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.flushMnemonic()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )

        if (state.kanji == null) {
            Text(
                stringResource(R.string.detail_missing),
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HeaderCard(state = state)
            }
            item {
                MnemonicCard(
                    value = state.mnemonic,
                    onValueChange = viewModel::saveMnemonic,
                )
            }
            item {
                Text(
                    stringResource(R.string.cards_using_kanji),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(state.cards, key = { it.card.id }) { card ->
                SentenceCardRow(item = card)
            }
        }
    }
}

@Composable
private fun HeaderCard(state: KanjiDetailUiState) {
    val kanji = state.kanji ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    kanji.char,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                state.status?.let {
                    AssistChip(
                        onClick = {},
                        label = { Text(it.status.label()) },
                    )
                }
            }

            kanji.keywordKo?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ReadingBlock(readings = state.readings)

            if (state.components.isNotEmpty()) {
                Text(
                    stringResource(R.string.components),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.components) { component ->
                        AssistChip(
                            onClick = {},
                            label = { Text(component) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingBlock(readings: KanjiReadings) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ReadingLine(label = stringResource(R.string.onyomi), readings = readings.on)
        ReadingLine(label = stringResource(R.string.kunyomi), readings = readings.kun)
    }
}

@Composable
private fun ReadingLine(label: String, readings: List<String>) {
    Text(
        "$label  ${readings.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "-"}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MnemonicCard(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.optional_memo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text(stringResource(R.string.memo_placeholder)) },
            )
        }
    }
}

@Composable
private fun SentenceCardRow(item: SentenceCardWithSrs) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.bucket) {
                Bucket.LAPSED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                Bucket.SLUGGISH -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                Bucket.MASTERED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                highlightedSentence(item.card),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.reading_value, item.card.correctReading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    item.bucket?.label() ?: stringResource(R.string.not_reviewed_yet),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.recentResponseMs?.let {
                    Text(
                        "${it}ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun highlightedSentence(card: SentenceCardEntity) = buildAnnotatedString {
    val start = card.targetStart.coerceIn(0, card.sentenceJp.length)
    val end = card.targetEnd.coerceIn(start, card.sentenceJp.length)
    append(card.sentenceJp.substring(0, start))
    withStyle(
        SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        ),
    ) {
        append(card.sentenceJp.substring(start, end))
    }
    append(card.sentenceJp.substring(end))
}

@Composable
private fun KanjiStatus.label(): String = when (this) {
    KanjiStatus.MASTERED -> stringResource(R.string.filter_mastered)
    KanjiStatus.NEAR -> stringResource(R.string.filter_near)
    KanjiStatus.WOBBLING -> stringResource(R.string.filter_wobbling)
    KanjiStatus.LEARNING -> stringResource(R.string.filter_learning)
    KanjiStatus.NEW -> stringResource(R.string.filter_new)
}

@Composable
private fun Bucket.label(): String = when (this) {
    Bucket.NEW -> stringResource(R.string.bucket_new_card)
    Bucket.MASTERED -> stringResource(R.string.bucket_mastered)
    Bucket.SLOW -> stringResource(R.string.bucket_slow)
    Bucket.SLUGGISH -> stringResource(R.string.bucket_sluggish)
    Bucket.LAPSED -> stringResource(R.string.bucket_lapsed)
}
