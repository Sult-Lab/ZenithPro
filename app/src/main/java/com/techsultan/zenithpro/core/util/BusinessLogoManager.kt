package com.techsultan.zenithpro.core.util

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class BusinessLogoManager(
    private val context: Context,
    private val imageLoader: ImageLoader
) {
    private val _logo = MutableStateFlow<Bitmap?>(null)
    val logo = _logo.asStateFlow()

    suspend fun loadLogo(url: String?): Bitmap? {
        if (url.isNullOrBlank()) {
            _logo.value = null
            return null
        }

        // Return cached logo if already loaded
        _logo.value?.let { return it }

        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .memoryCacheKey(url)
                .diskCacheKey(url)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)

            val bitmap = (result as? SuccessResult)
                ?.drawable
                ?.toBitmap()
            
            _logo.value = bitmap
            bitmap
        }.getOrNull()
    }
    
    fun getCachedLogo(): Bitmap? = _logo.value

    fun clear() {
        _logo.value = null
    }
}
