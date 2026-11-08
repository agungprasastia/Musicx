package com.example.musicx.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicx.catalog.youtube.CatalogRepository
import com.example.musicx.core.model.CatalogError
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CatalogUiState {
    data object Idle : CatalogUiState
    data object Loading : CatalogUiState
    data class Results(val title: String?, val items: List<CatalogItem>) : CatalogUiState
    data class Empty(val title: String?) : CatalogUiState
    data class Error(val message: String) : CatalogUiState
}

class MusicViewModel(
    private val repository: CatalogRepository,
    scope: CoroutineScope? = null,
) : ViewModel() {
    private val workScope: CoroutineScope = scope ?: viewModelScope

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Idle)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null
    private var lastRequest: Request? = null

    fun search(query: String, filter: CatalogFilter = CatalogFilter.ALL) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            currentJob?.cancel()
            lastRequest = null
            _uiState.value = CatalogUiState.Idle
            return
        }
        execute(Request.Search(trimmed, filter))
    }

    fun browse(id: String) {
        val browseId = id.trim()
        if (browseId.isEmpty()) {
            currentJob?.cancel()
            lastRequest = null
            _uiState.value = CatalogUiState.Idle
            return
        }
        execute(Request.Browse(browseId))
    }

    fun retry() {
        lastRequest?.let(::execute)
    }

    fun clear() {
        currentJob?.cancel()
        lastRequest = null
        _uiState.value = CatalogUiState.Idle
    }

    private fun execute(request: Request) {
        lastRequest = request
        currentJob?.cancel()
        currentJob = workScope.launch {
            _uiState.value = CatalogUiState.Loading
            when (request) {
                is Request.Search -> repository.search(request.query, request.filter).fold(
                    onSuccess = { items ->
                        ensureActive()
                        _uiState.value = items.toResults(title = null)
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        ensureActive()
                        _uiState.value = CatalogUiState.Error(error.userMessage())
                    },
                )

                is Request.Browse -> repository.browse(request.id).fold(
                    onSuccess = { page ->
                        ensureActive()
                        _uiState.value = page.items.toResults(title = page.title)
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        ensureActive()
                        _uiState.value = CatalogUiState.Error(error.userMessage())
                    },
                )
            }
        }
    }

    private fun List<CatalogItem>.toResults(title: String?): CatalogUiState =
        if (isEmpty()) CatalogUiState.Empty(title) else CatalogUiState.Results(title, this)

    private fun Throwable.userMessage(): String = when (this) {
        is CatalogError.EmptyQuery -> "Enter a search term."
        is CatalogError.EmptyBrowseId -> "This page is unavailable."
        is CatalogError.InvalidResponse -> "YouTube Music returned an unexpected response."
        is CatalogError.ResponseTooLarge -> "YouTube Music returned too much data."
        is CatalogError.Http -> "YouTube Music rejected the request (HTTP $statusCode)."
        is CatalogError.Network -> "Could not reach YouTube Music. Check your connection and try again."
        else -> "Something went wrong while loading the catalog."
    }

    private sealed interface Request {
        data class Search(val query: String, val filter: CatalogFilter) : Request
        data class Browse(val id: String) : Request
    }
}
