package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SudokuCell
import com.example.engine.SudokuEngine
import com.example.ui.theme.CellErrorDark
import com.example.ui.theme.CellErrorLight
import com.example.ui.theme.CellHintDark
import com.example.ui.theme.CellHintLight
import com.example.ui.theme.CellMatchingNumberDark
import com.example.ui.theme.CellMatchingNumberLight
import com.example.ui.theme.CellRelatedDark
import com.example.ui.theme.CellRelatedLight
import com.example.ui.theme.CellSelectedDark
import com.example.ui.theme.CellSelectedLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GridLineMajor
import com.example.ui.theme.GridLineMinorDark
import com.example.ui.theme.GridLineMinorLight

@Composable
fun SudokuBoardView(
    board: List<List<SudokuCell>>,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedValue = selectedCell?.let { (r, c) ->
        if (board.isNotEmpty() && r < board.size && c < board[r].size) board[r][c].value else 0
    } ?: 0

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .testTag("sudoku_board_container"),
        contentAlignment = Alignment.Center
    ) {
        val boardWidth = maxWidth
        val cellSize = boardWidth / 9f

        // Board Grid Layer
        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0 until SudokuEngine.GRID_SIZE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (c in 0 until SudokuEngine.GRID_SIZE) {
                        val cell = if (board.isNotEmpty() && r < board.size && c < board[r].size) {
                            board[r][c]
                        } else {
                            SudokuCell(row = r, col = c)
                        }

                        val isSelected = selectedCell?.first == r && selectedCell?.second == c
                        val isRelated = selectedCell != null && (
                            selectedCell.first == r ||
                            selectedCell.second == c ||
                            ((selectedCell.first / 3 == r / 3) && (selectedCell.second / 3 == c / 3))
                        )
                        val isSameNumber = selectedValue != 0 && cell.value == selectedValue

                        CellView(
                            cell = cell,
                            isSelected = isSelected,
                            isRelated = isRelated,
                            isSameNumber = isSameNumber,
                            isDark = isDark,
                            onClick = { onCellClick(r, c) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("sudoku_cell_${r}_${c}")
                        )
                    }
                }
            }
        }

        // Overlay major 3x3 and outer grid lines
        val lineMajorColor = if (isDark) GridLineMajor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        val lineMinorColor = if (isDark) GridLineMinorDark.copy(alpha = 0.4f) else GridLineMinorLight

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cellW = width / 9f
            val cellH = height / 9f

            // Minor grid lines
            for (i in 1..8) {
                if (i % 3 != 0) {
                    // Vertical
                    drawLine(
                        color = lineMinorColor,
                        start = Offset(i * cellW, 0f),
                        end = Offset(i * cellW, height),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Horizontal
                    drawLine(
                        color = lineMinorColor,
                        start = Offset(0f, i * cellH),
                        end = Offset(width, i * cellH),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Major block lines (every 3 cells)
            for (i in 1..2) {
                // Vertical major
                drawLine(
                    color = lineMajorColor,
                    start = Offset(i * 3 * cellW, 0f),
                    end = Offset(i * 3 * cellW, height),
                    strokeWidth = 2.5.dp.toPx()
                )
                // Horizontal major
                drawLine(
                    color = lineMajorColor,
                    start = Offset(0f, i * 3 * cellH),
                    end = Offset(width, i * 3 * cellH),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun CellView(
    cell: SudokuCell,
    isSelected: Boolean,
    isRelated: Boolean,
    isSameNumber: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBgColor = when {
        cell.isError -> if (isDark) CellErrorDark else CellErrorLight
        cell.isRevealed -> if (isDark) Color(0xFF4F46E5).copy(alpha = 0.25f) else Color(0xFFE0E7FF)
        cell.isHinted -> if (isDark) CellHintDark else CellHintLight
        isSelected -> if (isDark) CellSelectedDark else CellSelectedLight
        isSameNumber -> if (isDark) CellMatchingNumberDark else CellMatchingNumberLight
        isRelated -> if (isDark) CellRelatedDark else CellRelatedLight
        else -> Color.Transparent
    }

    val animatedBg by animateColorAsState(targetValue = targetBgColor, label = "cell_bg")

    Box(
        modifier = modifier
            .background(animatedBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cell.value != 0) {
            val textColor = when {
                cell.isError -> ErrorRed
                cell.isRevealed -> if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                cell.isFixed -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.primary
            }
            Text(
                text = cell.value.toString(),
                fontSize = 24.sp,
                fontWeight = if (cell.isFixed) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        } else if (cell.notes.isNotEmpty()) {
            // Notes 3x3 layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (r in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (c in 0..2) {
                            val num = r * 3 + c + 1
                            val hasNote = cell.notes.contains(num)
                            Text(
                                text = if (hasNote) num.toString() else " ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
