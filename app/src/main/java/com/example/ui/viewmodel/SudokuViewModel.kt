package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DailyChallengeEntity
import com.example.data.local.entity.SavedGameEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.Difficulty
import com.example.data.model.HintInfo
import com.example.data.model.OverallStats
import com.example.data.model.SudokuCell
import com.example.data.model.SudokuMove
import com.example.data.repository.SudokuRepository
import com.example.engine.SudokuEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class SudokuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SudokuRepository = SudokuRepository(
        AppDatabase.getDatabase(application).sudokuDao()
    )

    // Current navigation section: HOME is default landing page
    enum class AppSection { HOME, PLAY, DAILY, STATS, LOGIN }
    private val _currentSection = MutableStateFlow(AppSection.HOME)
    val currentSection: StateFlow<AppSection> = _currentSection.asStateFlow()

    // Current User & Authentication (1 device user per device)
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    val deviceUser: StateFlow<UserEntity?> = repository.deviceUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Light / Dark Theme mode
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Game Board State
    private val _board = MutableStateFlow<List<List<SudokuCell>>>(emptyList())
    val board: StateFlow<List<List<SudokuCell>>> = _board.asStateFlow()

    private var solution: List<List<Int>> = emptyList()
    private var initialBoard: List<List<Int>> = emptyList()

    private val _selectedCell = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedCell: StateFlow<Pair<Int, Int>?> = _selectedCell.asStateFlow()

    private val _isNotesMode = MutableStateFlow(false)
    val isNotesMode: StateFlow<Boolean> = _isNotesMode.asStateFlow()

    // Undo & Redo Stacks
    private val _undoStack = MutableStateFlow<List<SudokuMove>>(emptyList())
    val canUndo: StateFlow<Boolean> = MutableStateFlow(false)

    private val _redoStack = MutableStateFlow<List<SudokuMove>>(emptyList())
    val canRedo: StateFlow<Boolean> = MutableStateFlow(false)

    // Hint State
    private val _activeHint = MutableStateFlow<HintInfo?>(null)
    val activeHint: StateFlow<HintInfo?> = _activeHint.asStateFlow()

    private val _hintsUsed = MutableStateFlow(0)
    val hintsUsed: StateFlow<Int> = _hintsUsed.asStateFlow()

    // Timer & Game State
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isGameWon = MutableStateFlow(false)
    val isGameWon: StateFlow<Boolean> = _isGameWon.asStateFlow()

    private val _isGameGivenUp = MutableStateFlow(false)
    val isGameGivenUp: StateFlow<Boolean> = _isGameGivenUp.asStateFlow()

    private val _currentDifficulty = MutableStateFlow(Difficulty.MEDIUM)
    val currentDifficulty: StateFlow<Difficulty> = _currentDifficulty.asStateFlow()

    private val _isDailyGame = MutableStateFlow(false)
    val isDailyGame: StateFlow<Boolean> = _isDailyGame.asStateFlow()

    private val _dailyDate = MutableStateFlow<String?>(null)
    val dailyDate: StateFlow<String?> = _dailyDate.asStateFlow()

    private var timerJob: Job? = null

    // Room Persistent Stats & Dailies (Reactive & scoped to current user)
    val overallStats: StateFlow<OverallStats> = _currentUser
        .flatMapLatest { user ->
            val userId = user?.username ?: "guest"
            repository.getUserOverallStats(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OverallStats()
        )

    val allDailyChallenges: StateFlow<List<DailyChallengeEntity>> = _currentUser
        .flatMapLatest { user ->
            val userId = user?.username ?: "guest"
            repository.getUserDailyChallenges(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedGame: StateFlow<SavedGameEntity?> = _currentUser
        .flatMapLatest { user ->
            val userId = user?.username ?: "guest"
            repository.getSavedGame(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            // For the preview, start in guest mode (unauthenticated)
            _currentUser.value = null
        }
    }

    // --- NAVIGATION & THEME CONTROLS ---
    fun setSection(section: AppSection) {
        _currentSection.value = section
    }

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        _isDarkTheme.value = newTheme
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.updateUserTheme(user.username, if (newTheme) "DARK" else "LIGHT")
            }
        }
    }

    // --- USER AUTHENTICATION & MANAGEMENT (1 User per Device) ---
    fun clearAuthError() {
        _authError.value = null
    }

    fun login(username: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val res = repository.loginUser(username, password)
            if (res.isSuccess) {
                val user = res.getOrThrow()
                _currentUser.value = user
                if (user.themePreference == "DARK") {
                    _isDarkTheme.value = true
                } else if (user.themePreference == "LIGHT") {
                    _isDarkTheme.value = false
                }
                _currentSection.value = AppSection.HOME
                onComplete(true)
            } else {
                _authError.value = res.exceptionOrNull()?.message ?: "Login failed"
                onComplete(false)
            }
        }
    }

    fun register(
        username: String,
        displayName: String,
        password: String,
        avatarEmoji: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _authError.value = null
            val res = repository.registerUser(username, displayName, password, avatarEmoji)
            if (res.isSuccess) {
                val user = res.getOrThrow()
                _currentUser.value = user
                _currentSection.value = AppSection.HOME
                onComplete(true)
            } else {
                _authError.value = res.exceptionOrNull()?.message ?: "Registration failed"
                onComplete(false)
            }
        }
    }

    fun continueAsGuest() {
        _currentUser.value = null
        _authError.value = null
        _currentSection.value = AppSection.HOME
    }

    fun logout() {
        _currentUser.value = null
        _authError.value = null
        _currentSection.value = AppSection.LOGIN
    }

    fun startNewGame(difficulty: Difficulty) {
        _currentDifficulty.value = difficulty
        _isDailyGame.value = false
        _dailyDate.value = null
        _hintsUsed.value = 0
        _activeHint.value = null
        _undoStack.value = emptyList()
        _redoStack.value = emptyList()
        _isGameGivenUp.value = false
        updateUndoRedoStates()

        viewModelScope.launch {
            val userId = _currentUser.value?.username ?: "guest"
            repository.recordGameStarted(userId, difficulty)
            val generated = SudokuEngine.generatePuzzle(difficulty)
            solution = generated.solution
            initialBoard = generated.initialBoard

            val newCells = List(SudokuEngine.GRID_SIZE) { r ->
                List(SudokuEngine.GRID_SIZE) { c ->
                    val value = generated.initialBoard[r][c]
                    SudokuCell(
                        row = r,
                        col = c,
                        value = value,
                        initialValue = value
                    )
                }
            }
            _board.value = newCells
            _selectedCell.value = null
            _isGameWon.value = false
            _elapsedSeconds.value = 0L
            startTimer()
        }
    }

    fun startDailyChallenge(dateString: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) {
        _isDailyGame.value = true
        _dailyDate.value = dateString
        _hintsUsed.value = 0
        _activeHint.value = null
        _undoStack.value = emptyList()
        _redoStack.value = emptyList()
        _isGameGivenUp.value = false
        updateUndoRedoStates()

        viewModelScope.launch {
            val generated = SudokuEngine.generateDailyPuzzle(dateString)
            _currentDifficulty.value = generated.difficulty
            solution = generated.solution
            initialBoard = generated.initialBoard

            val newCells = List(SudokuEngine.GRID_SIZE) { r ->
                List(SudokuEngine.GRID_SIZE) { c ->
                    val value = generated.initialBoard[r][c]
                    SudokuCell(
                        row = r,
                        col = c,
                        value = value,
                        initialValue = value
                    )
                }
            }
            _board.value = newCells
            _selectedCell.value = null
            _isGameWon.value = false
            _elapsedSeconds.value = 0L
            _currentSection.value = AppSection.PLAY
            startTimer()
        }
    }

    fun restartCurrentGame() {
        if (initialBoard.isEmpty()) return
        _hintsUsed.value = 0
        _activeHint.value = null
        _undoStack.value = emptyList()
        _redoStack.value = emptyList()
        _isGameGivenUp.value = false
        updateUndoRedoStates()

        val resetCells = List(SudokuEngine.GRID_SIZE) { r ->
            List(SudokuEngine.GRID_SIZE) { c ->
                val initVal = initialBoard[r][c]
                SudokuCell(
                    row = r,
                    col = c,
                    value = initVal,
                    initialValue = initVal
                )
            }
        }
        _board.value = resetCells
        _selectedCell.value = null
        _isGameWon.value = false
        _elapsedSeconds.value = 0L
        startTimer()
    }

    fun giveUpAndRevealSolution() {
        if (_isGameWon.value || _isGameGivenUp.value) return
        _isGameGivenUp.value = true
        _isPaused.value = false
        pauseTimer()
        _activeHint.value = null
        _selectedCell.value = null

        val currentCells = _board.value
        if (solution.isNotEmpty() && currentCells.isNotEmpty()) {
            val revealedBoard = currentCells.mapIndexed { r, rowCells ->
                rowCells.mapIndexed { c, cell ->
                    val solVal = solution[r][c]
                    if (cell.value == solVal) {
                        cell.copy(isError = false, notes = emptySet())
                    } else {
                        cell.copy(
                            value = solVal,
                            notes = emptySet(),
                            isError = false,
                            isRevealed = true
                        )
                    }
                }
            }
            _board.value = revealedBoard
        }

        viewModelScope.launch {
            val userId = _currentUser.value?.username ?: "guest"
            repository.clearSavedGame(userId)
        }
    }

    fun selectCell(row: Int, col: Int) {
        if (_isPaused.value || _isGameWon.value || _isGameGivenUp.value) return
        _selectedCell.value = Pair(row, col)
        // Clear active hint highlight if user moves to another cell
        if (_activeHint.value != null && (_activeHint.value?.row != row || _activeHint.value?.col != col)) {
            _activeHint.value = null
            clearHintHighlight()
        }
    }

    fun toggleNotesMode() {
        if (_isGameGivenUp.value) return
        _isNotesMode.value = !_isNotesMode.value
    }

    fun onNumberInput(number: Int) {
        if (_isPaused.value || _isGameWon.value || _isGameGivenUp.value) return
        val currentSelected = _selectedCell.value ?: return
        val (r, c) = currentSelected
        val currentCells = _board.value
        val cell = currentCells[r][c]
        if (cell.isFixed) return

        if (_isNotesMode.value) {
            // Pencil notes toggling
            val newNotes = if (cell.notes.contains(number)) {
                cell.notes - number
            } else {
                cell.notes + number
            }
            val move = SudokuMove(
                row = r,
                col = c,
                prevValue = cell.value,
                newValue = cell.value,
                prevNotes = cell.notes,
                newNotes = newNotes
            )
            pushMove(move)
            updateCell(r, c) { it.copy(notes = newNotes) }
        } else {
            // Normal number placement
            val newValue = if (cell.value == number) 0 else number
            val move = SudokuMove(
                row = r,
                col = c,
                prevValue = cell.value,
                newValue = newValue,
                prevNotes = cell.notes,
                newNotes = emptySet()
            )
            pushMove(move)
            updateCell(r, c) { it.copy(value = newValue, notes = emptySet(), isHinted = false) }

            // Auto clean notes from row, col, and box if a number is placed
            if (newValue != 0) {
                removeNotesForPlacedNumber(r, c, newValue)
            }

            recheckBoardErrorsAndCompletion()
        }
    }

    fun eraseCell() {
        if (_isPaused.value || _isGameWon.value || _isGameGivenUp.value) return
        val currentSelected = _selectedCell.value ?: return
        val (r, c) = currentSelected
        val cell = _board.value[r][c]
        if (cell.isFixed) return

        if (cell.value != 0 || cell.notes.isNotEmpty()) {
            val move = SudokuMove(
                row = r,
                col = c,
                prevValue = cell.value,
                newValue = 0,
                prevNotes = cell.notes,
                newNotes = emptySet()
            )
            pushMove(move)
            updateCell(r, c) { it.copy(value = 0, notes = emptySet(), isHinted = false) }
            recheckBoardErrorsAndCompletion()
        }
    }

    // --- UNDO AND REDO FUNCTIONALITY ---
    fun undo() {
        val stack = _undoStack.value
        if (stack.isEmpty() || _isPaused.value || _isGameWon.value || _isGameGivenUp.value) return

        val lastMove = stack.last()
        _undoStack.value = stack.dropLast(1)
        _redoStack.value = _redoStack.value + lastMove
        updateUndoRedoStates()

        updateCell(lastMove.row, lastMove.col) {
            it.copy(value = lastMove.prevValue, notes = lastMove.prevNotes, isHinted = false)
        }
        _selectedCell.value = Pair(lastMove.row, lastMove.col)
        recheckBoardErrorsAndCompletion()
    }

    fun redo() {
        val stack = _redoStack.value
        if (stack.isEmpty() || _isPaused.value || _isGameWon.value || _isGameGivenUp.value) return

        val move = stack.last()
        _redoStack.value = stack.dropLast(1)
        _undoStack.value = _undoStack.value + move
        updateUndoRedoStates()

        updateCell(move.row, move.col) {
            it.copy(value = move.newValue, notes = move.newNotes, isHinted = false)
        }
        _selectedCell.value = Pair(move.row, move.col)
        recheckBoardErrorsAndCompletion()
    }

    private fun pushMove(move: SudokuMove) {
        _undoStack.value = _undoStack.value + move
        _redoStack.value = emptyList() // Redo stack clears on new action
        updateUndoRedoStates()
    }

    private fun updateUndoRedoStates() {
        (canUndo as MutableStateFlow).value = _undoStack.value.isNotEmpty()
        (canRedo as MutableStateFlow).value = _redoStack.value.isNotEmpty()
    }

    // --- HINT SYSTEM ---
    fun requestHint() {
        if (_isPaused.value || _isGameWon.value || _isGameGivenUp.value) return
        val currentBoardValues = _board.value.map { row -> row.map { it.value } }
        val hint = SudokuEngine.findHint(currentBoardValues, solution) ?: return

        _hintsUsed.value = _hintsUsed.value + 1
        _activeHint.value = hint

        // Highlight hinted cell and select it
        _selectedCell.value = Pair(hint.row, hint.col)
        updateCell(hint.row, hint.col) { it.copy(isHinted = true) }
    }

    fun applyActiveHint() {
        val hint = _activeHint.value ?: return
        val (r, c) = Pair(hint.row, hint.col)
        val currentCell = _board.value[r][c]

        val move = SudokuMove(
            row = r,
            col = c,
            prevValue = currentCell.value,
            newValue = hint.solutionValue,
            prevNotes = currentCell.notes,
            newNotes = emptySet()
        )
        pushMove(move)
        updateCell(r, c) { it.copy(value = hint.solutionValue, notes = emptySet(), isHinted = false) }
        removeNotesForPlacedNumber(r, c, hint.solutionValue)

        _activeHint.value = null
        recheckBoardErrorsAndCompletion()
    }

    fun dismissHint() {
        _activeHint.value = null
        clearHintHighlight()
    }

    private fun clearHintHighlight() {
        val current = _board.value
        _board.value = current.map { row ->
            row.map { cell ->
                if (cell.isHinted) cell.copy(isHinted = false) else cell
            }
        }
    }

    private fun removeNotesForPlacedNumber(row: Int, col: Int, number: Int) {
        val current = _board.value
        val startRow = (row / SudokuEngine.BOX_SIZE) * SudokuEngine.BOX_SIZE
        val startCol = (col / SudokuEngine.BOX_SIZE) * SudokuEngine.BOX_SIZE

        _board.value = current.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                val isInRow = r == row
                val isInCol = c == col
                val isInBox = r in startRow until startRow + SudokuEngine.BOX_SIZE &&
                        c in startCol until startCol + SudokuEngine.BOX_SIZE
                if ((isInRow || isInCol || isInBox) && cell.notes.contains(number)) {
                    cell.copy(notes = cell.notes - number)
                } else {
                    cell
                }
            }
        }
    }

    private fun updateCell(row: Int, col: Int, transform: (SudokuCell) -> SudokuCell) {
        val current = _board.value
        _board.value = current.mapIndexed { r, rowCells ->
            if (r == row) {
                rowCells.mapIndexed { c, cell ->
                    if (c == col) transform(cell) else cell
                }
            } else {
                rowCells
            }
        }
    }

    private fun recheckBoardErrorsAndCompletion() {
        val currentBoardValues = _board.value.map { row -> row.map { it.value } }
        val errors = SudokuEngine.checkErrors(currentBoardValues).toSet()

        _board.value = _board.value.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                cell.copy(isError = errors.contains(Pair(r, c)))
            }
        }

        // Check if full and completely matches solution
        if (errors.isEmpty() && SudokuEngine.isBoardFull(currentBoardValues)) {
            var allMatch = true
            for (r in 0 until SudokuEngine.GRID_SIZE) {
                for (c in 0 until SudokuEngine.GRID_SIZE) {
                    if (currentBoardValues[r][c] != solution[r][c]) {
                        allMatch = false
                        break
                    }
                }
            }
            if (allMatch) {
                handleVictory()
            }
        }
    }

    private fun handleVictory() {
        _isGameWon.value = true
        pauseTimer()
        val timeSeconds = _elapsedSeconds.value
        val userId = _currentUser.value?.username ?: "guest"

        viewModelScope.launch {
            if (_isDailyGame.value) {
                val date = _dailyDate.value ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                repository.recordDailyCompleted(
                    userId = userId,
                    date = date,
                    timeSeconds = timeSeconds,
                    hintsUsed = _hintsUsed.value,
                    difficulty = _currentDifficulty.value.name
                )
            } else {
                repository.recordGameWon(userId, _currentDifficulty.value, timeSeconds)
            }
            repository.clearSavedGame(userId)
        }
    }

    // --- TIMER CONTROLS ---
    fun togglePause() {
        if (_isGameWon.value) return
        if (_isPaused.value) {
            _isPaused.value = false
            startTimer()
        } else {
            _isPaused.value = true
            pauseTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (!_isPaused.value && !_isGameWon.value) {
                delay(1000L)
                _elapsedSeconds.value = _elapsedSeconds.value + 1L
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun getRemainingCount(number: Int): Int {
        var count = 0
        val current = _board.value
        for (r in current) {
            for (c in r) {
                if (c.value == number) count++
            }
        }
        return SudokuEngine.GRID_SIZE - count
    }

    override fun onCleared() {
        super.onCleared()
        pauseTimer()
    }
}
