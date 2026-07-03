package com.langtrainer.feature.kanjisrs

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.langtrainer.core.model.ReviewOutcome
import kotlinx.coroutines.delay

private const val SETTINGS_PREFS = "kanji_srs_settings"
private const val KEY_AUTO_ADVANCE = "auto_advance"

@Composable
fun KanjiSrsScreen(
    onExit: () -> Unit,
    deck: Deck = Deck.GENERAL,
    viewModel: KanjiSrsViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.onAppPaused()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (ui is KanjiSrsViewModel.UiState.Idle) viewModel.startSession(deck = deck)
    }

    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        when (val state = ui) {
            KanjiSrsViewModel.UiState.Idle,
            KanjiSrsViewModel.UiState.Loading -> CenteredSpinner()

            is KanjiSrsViewModel.UiState.Empty -> EmptyState(
                isNameDeck = state.isNameDeck,
                onExit = onExit,
            )

            is KanjiSrsViewModel.UiState.Reviewing -> ReviewingState(
                state = state,
                onAnswer = viewModel::onAnswer,
                onUnsure = viewModel::onUnsure,
                onContinue = viewModel::onContinueAfterReveal,
                onCardRendered = viewModel::onCardRendered,
            )

            is KanjiSrsViewModel.UiState.Finished -> FinishedState(
                summary = state.summary,
                onRestart = viewModel::restart,
                onExit = onExit,
            )
        }
    }
}

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(isNameDeck: Boolean, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (isNameDeck) {
                stringResource(R.string.empty_name_deck)
            } else {
                stringResource(R.string.empty_general_deck)
            },
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onExit) { Text(stringResource(R.string.home)) }
    }
}

@Composable
private fun ReviewingState(
    state: KanjiSrsViewModel.UiState.Reviewing,
    onAnswer: (String) -> Unit,
    onUnsure: () -> Unit,
    onContinue: () -> Unit,
    onCardRendered: (Long) -> Unit,
) {
    val card = state.cards[state.index]
    val progress = (state.index + 1).toFloat() / state.cards.size.toFloat()
    var showHelp by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE) }
    var autoAdvance by remember { mutableStateOf(prefs.getBoolean(KEY_AUTO_ADVANCE, true)) }
    val tts = rememberSentenceTts()

    LaunchedEffect(state.index) {
        onCardRendered(System.currentTimeMillis())
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.help_1))
                    Text(stringResource(R.string.help_2))
                    Text(stringResource(R.string.help_3))
                    Text(stringResource(R.string.help_4))
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${state.index + 1} / ${state.cards.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.auto_advance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Switch(
                    checked = autoAdvance,
                    onCheckedChange = {
                        autoAdvance = it
                        prefs.edit().putBoolean(KEY_AUTO_ADVANCE, it).apply()
                    },
                )
                IconButton(onClick = { showHelp = true }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.help_content_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.isNameDeck) {
            Spacer(Modifier.height(8.dp))
            NameDeckBanner()
        }

        Spacer(Modifier.height(40.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    buildHighlightedSentence(
                        sentence = card.sentenceJp,
                        start = card.targetStart,
                        end = card.targetEnd,
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                if (tts.isReady) {
                    IconButton(onClick = { tts.speak(card.sentenceJp) }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = stringResource(R.string.listen_sentence),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.how_read),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(36.dp))

        val outcome = state.revealedOutcome

        // Auto-advance only the "good" counted outcomes. LAPSED and the skip
        // notices (ABANDONED / BACKGROUNDED) wait for a tap so they can be read.
        LaunchedEffect(state.index, outcome, autoAdvance) {
            if (autoAdvance && outcome != null &&
                outcome.countsForSrsUpdate && outcome != ReviewOutcome.LAPSED
            ) {
                delay(1500)
                onContinue()
            }
        }

        if (outcome == null) {
            AnswerChoices(state = state, onAnswer = onAnswer)

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onUnsure,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.unsure), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            RevealedAnswer(
                state = state,
                autoAdvance = autoAdvance,
                onContinue = onContinue,
            )
        }
    }
}

@Composable
private fun AnswerChoices(
    state: KanjiSrsViewModel.UiState.Reviewing,
    onAnswer: (String) -> Unit,
) {
    state.shuffledChoices.forEach { choice ->
        Button(
            onClick = { onAnswer(choice) },
            enabled = !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(choice, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun RevealedAnswer(
    state: KanjiSrsViewModel.UiState.Reviewing,
    autoAdvance: Boolean,
    onContinue: () -> Unit,
) {
    val card = state.cards[state.index]
    val outcome = state.revealedOutcome ?: return
    val correctAnswer = state.revealedCorrect ?: card.correctReading
    val userPick = state.revealedUserPick
    val localeLanguage = LocalConfiguration.current.locales[0]?.language
    val meaning = when (localeLanguage) {
        "ko" -> card.translationKo ?: card.translationEn
        else -> card.translationEn ?: card.translationKo
    }
    var showMeaning by remember(card.cardId, outcome) { mutableStateOf(false) }

    state.shuffledChoices.forEach { choice ->
        val isCorrect = choice == correctAnswer
        val isWrongPick = !isCorrect && userPick != null && choice == userPick
        Button(
            onClick = { },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = when {
                    isCorrect -> MaterialTheme.colorScheme.primary
                    isWrongPick -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                disabledContentColor = when {
                    isCorrect -> MaterialTheme.colorScheme.onPrimary
                    isWrongPick -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(choice, style = MaterialTheme.typography.titleMedium)
        }
    }

    Spacer(Modifier.height(12.dp))

    Text(
        "${formatResponseTime(state.revealedResponseMs)} · ${outcome.displayLabel()}",
        style = MaterialTheme.typography.titleMedium,
        color = if (outcome == ReviewOutcome.MASTERED) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.fillMaxWidth(),
    )

    if (meaning != null) {
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showMeaning = !showMeaning }) {
            Text(
                if (showMeaning) {
                    stringResource(R.string.hide_meaning)
                } else {
                    stringResource(R.string.show_meaning)
                },
            )
        }
        if (showMeaning) {
            Text(
                meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (outcome == ReviewOutcome.LAPSED) {
        Spacer(Modifier.height(8.dp))
        Text(
            if (userPick != null) {
                stringResource(R.string.correct_answer_wrong_pick, correctAnswer)
            } else {
                stringResource(R.string.correct_answer, correctAnswer)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    } else if (!outcome.countsForSrsUpdate) {
        // ABANDONED / BACKGROUNDED: show the answer and explain that, since this
        // attempt did not count, the card will simply come back later.
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.correct_answer, correctAnswer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when (outcome) {
                // Over the 30s think-time limit: nudge the user to answer faster.
                ReviewOutcome.ABANDONED -> stringResource(R.string.skipped_abandoned)
                else -> stringResource(R.string.skipped_will_return)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // LAPSED and the skip notices always wait for a tap; "good" answers wait too
    // only when auto-advance is off (otherwise the screen advances after 1.5s).
    if (outcome == ReviewOutcome.LAPSED || !outcome.countsForSrsUpdate || !autoAdvance) {
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.next), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FinishedState(
    summary: KanjiSrsViewModel.SessionSummary,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.session_finished), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        SummaryRow(label = stringResource(R.string.summary_mastered), value = summary.mastered)
        SummaryRow(label = stringResource(R.string.summary_slow), value = summary.slow)
        SummaryRow(label = stringResource(R.string.summary_sluggish), value = summary.sluggish)
        SummaryRow(label = stringResource(R.string.summary_lapsed), value = summary.lapsed)
        if (summary.noisyAttempts > 0) {
            SummaryRow(
                label = stringResource(R.string.summary_noisy),
                value = summary.noisyAttempts,
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.again))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun NameDeckBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            stringResource(R.string.name_deck_banner),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

private fun buildHighlightedSentence(sentence: String, start: Int, end: Int) =
    buildAnnotatedString {
        if (start < 0 || end > sentence.length || start >= end) {
            append(sentence)
            return@buildAnnotatedString
        }
        append(sentence.substring(0, start))
        withStyle(
            // Highlighter look: dark text on a bright amber fill. Fixing both
            // colors keeps it clearly visible on dark and light themes alike
            // (the previous 20%-alpha yellow was nearly invisible on black).
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                background = Color(0xFFFFE082),
            ),
        ) {
            append(sentence.substring(start, end))
        }
        append(sentence.substring(end))
    }

@Composable
private fun formatResponseTime(responseMs: Long?): String =
    responseMs?.let { stringResource(R.string.response_seconds, it / 1000.0) }
        ?: stringResource(R.string.no_time)

@Composable
private fun ReviewOutcome.displayLabel(): String = when (this) {
    ReviewOutcome.MASTERED -> stringResource(R.string.outcome_mastered)
    ReviewOutcome.SLOW -> stringResource(R.string.outcome_slow)
    ReviewOutcome.SLUGGISH -> stringResource(R.string.outcome_sluggish)
    ReviewOutcome.LAPSED -> stringResource(R.string.outcome_lapsed)
    ReviewOutcome.EARLY_TAP -> stringResource(R.string.outcome_early_tap)
    ReviewOutcome.ABANDONED -> stringResource(R.string.outcome_abandoned)
    ReviewOutcome.BACKGROUNDED -> stringResource(R.string.outcome_backgrounded)
}
