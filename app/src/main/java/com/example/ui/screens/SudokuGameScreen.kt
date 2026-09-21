package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Difficulty
import com.example.ui.components.GameHeaderView
import com.example.ui.components.GameWonDialog
import com.example.ui.components.HintBanner
import com.example.ui.components.NumberPadView
import com.example.ui.components.SudokuBoardView
import com.example.ui.viewmodel.SudokuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuGameScreen(
    viewModel: SudokuViewModel,
    onNavigateToStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val board by viewModel.board.collectAsState()
    val selectedCell by viewModel.selectedCell.collectAsState()
    val isNotesMode by viewModel.isNotesMode.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val activeHint by viewModel.activeHint.collectAsState()
    val hintsUsed by viewModel.hintsUsed.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isGameWon by viewModel.isGameWon.collectAsState()
    val isGameGivenUp by viewModel.isGameGivenUp.collectAsState()
    val difficulty by viewModel.currentDifficulty.collectAsState()
    val isDaily by viewModel.isDailyGame.collectAsState()
    val dailyDate by viewModel.dailyDate.collectAsState()
    val isDarkMode by viewModel.isDarkTheme.collectAsState()

    var showNewGameSheet by remember { mutableStateOf(false) }
    var showGiveUpConfirmDialog by remember { mutableStateOf(false) }
    var showGiveUpResultDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        if (board.isEmpty()) {
            NoGameActiveView(
                onStartGame = { viewModel.startNewGame(it) },
                isDarkMode = isDarkMode,
                onToggleTheme = { viewModel.toggleTheme() }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Difficulty, Timer & Control buttons
                GameHeaderView(
                    difficulty = difficulty,
                    isDaily = isDaily,
                    dailyDate = dailyDate,
                    elapsedSeconds = elapsedSeconds,
                    isPaused = isPaused,
                    onTogglePause = { viewModel.togglePause() },
                    onRestart = { viewModel.restartCurrentGame() },
                    onNewGameClick = { showNewGameSheet = true },
                    onGiveUp = { showGiveUpConfirmDialog = true },
                    isDarkMode = isDarkMode,
                    onToggleTheme = { viewModel.toggleTheme() }
                )

            // Solution Revealed Banner if user gave up
            if (isGameGivenUp) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("solution_revealed_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Solution Revealed",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Game forfeited. Solved cells highlighted.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Button(
                            onClick = { showNewGameSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("banner_new_game_button")
                        ) {
                            Text(
                                text = "New Game",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Hint Banner if active
            activeHint?.let { hint ->
                HintBanner(
                    hintInfo = hint,
                    onApplyHint = { viewModel.applyActiveHint() },
                    onDismiss = { viewModel.dismissHint() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Sudoku Grid with Pause Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                SudokuBoardView(
                    board = board,
                    selectedCell = selectedCell,
                    onCellClick = { r, c -> viewModel.selectCell(r, c) }
                )

                if (isPaused) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                            .clickable { viewModel.togglePause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Game Paused",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Tap anywhere to resume",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            FilledTonalButton(
                                onClick = {
                                    showGiveUpConfirmDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("pause_give_up_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Give Up & Reveal Solution")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Number Pad & Action Controls (Undo, Redo, Erase, Notes, Hint, 1-9)
            NumberPadView(
                canUndo = canUndo,
                canRedo = canRedo,
                isNotesMode = isNotesMode,
                hintsUsed = hintsUsed,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onErase = { viewModel.eraseCell() },
                onToggleNotes = { viewModel.toggleNotesMode() },
                onHint = { viewModel.requestHint() },
                onNumberClick = { num -> viewModel.onNumberInput(num) },
                getRemainingCount = { num -> viewModel.getRemainingCount(num) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

        // Game Won Dialog
        if (isGameWon) {
            GameWonDialog(
                difficulty = difficulty,
                isDaily = isDaily,
                elapsedSeconds = elapsedSeconds,
                hintsUsed = hintsUsed,
                onPlayAgain = {
                    viewModel.startNewGame(difficulty)
                },
                onViewStats = {
                    onNavigateToStats()
                }
            )
        }

        // New Game Difficulty Selector Sheet
        if (showNewGameSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showNewGameSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Start New Game",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose a difficulty level:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Difficulty.entries.forEach { diff ->
                        Card(
                            onClick = {
                                viewModel.startNewGame(diff)
                                showNewGameSheet = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (difficulty == diff) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .testTag("select_difficulty_${diff.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = diff.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (difficulty == diff) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "~${diff.cluesCount} clues",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Give Up Confirmation Dialog
        if (showGiveUpConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showGiveUpConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Give Up This Puzzle?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Would you like to forfeit and reveal the full solution to study the board, or restart this puzzle from the beginning?",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showGiveUpConfirmDialog = false
                            viewModel.giveUpAndRevealSolution()
                            showGiveUpResultDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_give_up_button")
                    ) {
                        Text("Reveal Solution")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                showGiveUpConfirmDialog = false
                                viewModel.restartCurrentGame()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("give_up_restart_button")
                        ) {
                            Text("Restart")
                        }
                        TextButton(
                            onClick = { showGiveUpConfirmDialog = false },
                            modifier = Modifier.testTag("cancel_give_up_button")
                        ) {
                            Text("Keep Playing")
                        }
                    }
                }
            )
        }

        // Solution Revealed / Forfeited Summary Dialog
        if (showGiveUpResultDialog && isGameGivenUp) {
            AlertDialog(
                onDismissRequest = { showGiveUpResultDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Solution Revealed",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "The complete solved board has been revealed with newly filled numbers highlighted. You can inspect the grid or jump into a new game anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showGiveUpResultDialog = false
                            showNewGameSheet = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("result_new_game_button")
                    ) {
                        Text("Start New Game")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showGiveUpResultDialog = false },
                        modifier = Modifier.testTag("review_board_button")
                    ) {
                        Text("Review Board")
                    }
                }
            )
        }
    }
}

@Composable
fun NoGameActiveView(
    onStartGame: (Difficulty) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Theme toggle in no-game state too
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = if (isDarkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ready for a Challenge?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Select a difficulty level to start playing Sudoku",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Difficulty.entries.forEach { diff ->
                Card(
                    onClick = { onStartGame(diff) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_game_${diff.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = diff.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${diff.cluesCount} clues",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
