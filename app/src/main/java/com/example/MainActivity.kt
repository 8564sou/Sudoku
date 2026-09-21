package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DailyChallengeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.SudokuGameScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SudokuViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val sudokuViewModel: SudokuViewModel = viewModel()
      val isDarkMode by sudokuViewModel.isDarkTheme.collectAsState()

      MyApplicationTheme(darkTheme = isDarkMode) {
        SudokuApp(viewModel = sudokuViewModel)
      }
    }
  }
}

@Composable
fun SudokuApp(viewModel: SudokuViewModel = viewModel()) {
  val currentSection by viewModel.currentSection.collectAsState()

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing),
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.navigationBars)
          .testTag("bottom_nav_bar")
      ) {
        NavigationBarItem(
          selected = currentSection == SudokuViewModel.AppSection.HOME,
          onClick = { viewModel.setSection(SudokuViewModel.AppSection.HOME) },
          icon = {
            Icon(
              imageVector = if (currentSection == SudokuViewModel.AppSection.HOME) Icons.Filled.Home else Icons.Outlined.Home,
              contentDescription = "Home"
            )
          },
          label = { Text("Home") },
          modifier = Modifier.testTag("nav_item_home")
        )

        NavigationBarItem(
          selected = currentSection == SudokuViewModel.AppSection.PLAY,
          onClick = { viewModel.setSection(SudokuViewModel.AppSection.PLAY) },
          icon = {
            Icon(
              imageVector = if (currentSection == SudokuViewModel.AppSection.PLAY) Icons.Filled.GridOn else Icons.Outlined.GridOn,
              contentDescription = "Play"
            )
          },
          label = { Text("Play") },
          modifier = Modifier.testTag("nav_item_play")
        )

        NavigationBarItem(
          selected = currentSection == SudokuViewModel.AppSection.DAILY,
          onClick = { viewModel.setSection(SudokuViewModel.AppSection.DAILY) },
          icon = {
            Icon(
              imageVector = if (currentSection == SudokuViewModel.AppSection.DAILY) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday,
              contentDescription = "Daily"
            )
          },
          label = { Text("Daily") },
          modifier = Modifier.testTag("nav_item_daily")
        )

        NavigationBarItem(
          selected = currentSection == SudokuViewModel.AppSection.STATS,
          onClick = { viewModel.setSection(SudokuViewModel.AppSection.STATS) },
          icon = {
            Icon(
              imageVector = if (currentSection == SudokuViewModel.AppSection.STATS) Icons.Filled.BarChart else Icons.Outlined.BarChart,
              contentDescription = "Statistics"
            )
          },
          label = { Text("Statistics") },
          modifier = Modifier.testTag("nav_item_stats")
        )
      }
    }
  ) { innerPadding ->
    Crossfade(
      targetState = currentSection,
      label = "screen_crossfade",
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) { section ->
      when (section) {
        SudokuViewModel.AppSection.HOME -> {
          HomeScreen(viewModel = viewModel)
        }
        SudokuViewModel.AppSection.PLAY -> {
          SudokuGameScreen(
            viewModel = viewModel,
            onNavigateToStats = { viewModel.setSection(SudokuViewModel.AppSection.STATS) }
          )
        }
        SudokuViewModel.AppSection.DAILY -> {
          DailyChallengeScreen(
            viewModel = viewModel
          )
        }
        SudokuViewModel.AppSection.STATS -> {
          StatisticsScreen(
            viewModel = viewModel
          )
        }
        SudokuViewModel.AppSection.LOGIN -> {
          LoginScreen(
            viewModel = viewModel
          )
        }
      }
    }
  }
}

