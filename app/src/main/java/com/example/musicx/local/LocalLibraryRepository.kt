package com.example.musicx.local

import android.content.Context
import com.example.musicx.core.model.CatalogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalLibraryRepository(private val context: Context) {

    suspend fun loadAll(): List<CatalogItem.Track> = withContext(Dispatchers.IO) {
        LocalAudioScanner.scan(context)
    }
}
