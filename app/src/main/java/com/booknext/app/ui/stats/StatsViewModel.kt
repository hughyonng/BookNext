package com.booknext.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.local.db.BookDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsState(
    val totalSeconds: Long = 0L,
    val finishedCount: Int = 0,
    val totalBooks: Int = 0,
    val lastBookTitle: String = "",
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val bookDao: BookDao,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(bookDao.observeAll(), bookDao.observeFinished()) { all, finished ->
                StatsState(
                    totalSeconds = all.sumOf { it.totalReadingSeconds },
                    finishedCount = finished.size,
                    totalBooks = all.size,
                    lastBookTitle = all.maxByOrNull { it.lastReadAt }?.title ?: "",
                )
            }.collect { _state.value = it }
        }
    }
}