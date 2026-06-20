package com.langtrainer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.langtrainer.about.SourcesScreen
import com.langtrainer.feature.kanjisrs.Deck
import com.langtrainer.feature.kanjisrs.KanjiSrsScreen
import com.langtrainer.feature.kanjisrs.collection.KanjiCollectionScreen
import com.langtrainer.feature.kanjisrs.collection.KanjiDetailScreen
import com.langtrainer.home.HomeScreen
import com.langtrainer.nav.Routes
import com.langtrainer.ui.theme.LanguageTrainerTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.ak65477.kanjitrainer.R

private const val APP_PREFS = "kanji_trainer_prefs"
private const val INTRO_DIALOG_SEEN_KEY = "intro_dialog_seen_v1"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)

        setContent {
            LanguageTrainerTheme {
                var showIntroDialog by remember {
                    mutableStateOf(!prefs.getBoolean(INTRO_DIALOG_SEEN_KEY, false))
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                ) { inner ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier.padding(inner),
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                onStartKanjiSrs = { navController.navigate(Routes.kanjiSrs("GENERAL")) },
                                onStartNameKanji = { navController.navigate(Routes.kanjiSrs("NAME")) },
                                onOpenKanjiCollection = { navController.navigate(Routes.KANJI_COLLECTION) },
                                onOpenSources = { navController.navigate(Routes.SOURCES) },
                            )
                        }
                        composable(Routes.SOURCES) {
                            SourcesScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(
                            route = "${Routes.KANJI_SRS}?${Routes.ARG_DECK}={${Routes.ARG_DECK}}",
                            arguments = listOf(
                                navArgument(Routes.ARG_DECK) {
                                    type = NavType.StringType
                                    defaultValue = "GENERAL"
                                },
                            ),
                        ) { backStackEntry ->
                            val deck = when (backStackEntry.arguments?.getString(Routes.ARG_DECK)) {
                                "NAME" -> Deck.NAME
                                else -> Deck.GENERAL
                            }
                            KanjiSrsScreen(
                                onExit = { navController.popBackStack() },
                                deck = deck,
                            )
                        }
                        composable(Routes.KANJI_COLLECTION) {
                            KanjiCollectionScreen(
                                onBack = { navController.popBackStack() },
                                onOpenDetail = { id -> navController.navigate(Routes.kanjiDetail(id)) },
                            )
                        }
                        composable(
                            route = "${Routes.KANJI_DETAIL_PREFIX}/{kanjiId}",
                            arguments = listOf(navArgument("kanjiId") { type = NavType.StringType }),
                        ) {
                            KanjiDetailScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }

                if (showIntroDialog) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(text = stringResource(R.string.intro_dialog_title)) },
                        text = { Text(text = stringResource(R.string.intro_dialog_body)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    prefs.edit().putBoolean(INTRO_DIALOG_SEEN_KEY, true).apply()
                                    showIntroDialog = false
                                },
                            ) {
                                Text(text = stringResource(R.string.intro_dialog_confirm))
                            }
                        },
                    )
                }
            }
        }
    }
}
