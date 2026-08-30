package com.shjamolov.mediastreamplayer.core.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val mutableLanguage = MutableStateFlow(
        CatalogLanguage.fromApiCode(preferences.getString(KEY_LANGUAGE, null)),
    )
    val language: StateFlow<CatalogLanguage> = mutableLanguage.asStateFlow()

    fun setLanguage(language: CatalogLanguage) {
        preferences.edit { putString(KEY_LANGUAGE, language.apiCode) }
        mutableLanguage.value = language
    }

    private companion object { const val KEY_LANGUAGE = "catalog_language" }
}

enum class CatalogLanguage(val apiCode: String) {
    RUSSIAN("ru-RU"),
    ENGLISH("en-US"),
    UZBEK("uz-UZ");

    companion object {
        fun fromApiCode(value: String?): CatalogLanguage = entries.firstOrNull { it.apiCode == value } ?: RUSSIAN
    }
}
