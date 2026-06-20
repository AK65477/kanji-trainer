package com.langtrainer.feature.kanjisrs.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.langtrainer.feature.kanjisrs.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanjiCollectionScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: KanjiCollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.collection_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.collection_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )

        FilterRow(
            selected = state.filter,
            counts = state.counts,
            onSelect = viewModel::setFilter,
        )

        if (state.rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.collection_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.rows, key = { it.kanji.id }) { item ->
                    KanjiTile(
                        item = item,
                        onClick = { onOpenDetail(item.kanji.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: KanjiCollectionFilter,
    counts: Map<KanjiCollectionFilter, Int>,
    onSelect: (KanjiCollectionFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(KanjiCollectionFilter.entries) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = {
                    Text("${filter.label()} ${counts[filter] ?: 0}")
                },
            )
        }
    }
}

@Composable
private fun KanjiTile(
    item: KanjiWithStatus,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = item.status.containerColor()),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StatusDot(
                color = item.status.dotColor(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    item.kanji.char,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.kanji.keywordKo?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color,
    ) {
        Box(modifier = Modifier.padding(4.dp))
    }
}

@Composable
private fun KanjiCollectionFilter.label(): String = when (this) {
    KanjiCollectionFilter.ALL -> stringResource(R.string.filter_all)
    KanjiCollectionFilter.MASTERED -> stringResource(R.string.filter_mastered)
    KanjiCollectionFilter.NEAR -> stringResource(R.string.filter_near)
    KanjiCollectionFilter.WOBBLING -> stringResource(R.string.filter_wobbling)
    KanjiCollectionFilter.LEARNING -> stringResource(R.string.filter_learning)
    KanjiCollectionFilter.NEW -> stringResource(R.string.filter_new)
}

@Composable
private fun KanjiStatus.containerColor(): Color = when (this) {
    KanjiStatus.MASTERED -> MaterialTheme.colorScheme.primaryContainer
    KanjiStatus.NEAR -> MaterialTheme.colorScheme.tertiaryContainer
    KanjiStatus.WOBBLING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
    KanjiStatus.LEARNING -> MaterialTheme.colorScheme.secondaryContainer
    KanjiStatus.NEW -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun KanjiStatus.dotColor(): Color = when (this) {
    KanjiStatus.MASTERED -> MaterialTheme.colorScheme.primary
    KanjiStatus.NEAR -> MaterialTheme.colorScheme.tertiary
    KanjiStatus.WOBBLING -> MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
    KanjiStatus.LEARNING -> MaterialTheme.colorScheme.secondary
    KanjiStatus.NEW -> MaterialTheme.colorScheme.outline
}
