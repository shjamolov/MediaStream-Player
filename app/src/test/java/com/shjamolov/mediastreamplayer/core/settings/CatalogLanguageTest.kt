package com.shjamolov.mediastreamplayer.core.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogLanguageTest {
    @Test
    fun fromApiCode_restoresSavedLanguageAndDefaultsToRussian() {
        assertEquals(CatalogLanguage.UZBEK, CatalogLanguage.fromApiCode("uz-UZ"))
        assertEquals(CatalogLanguage.ENGLISH, CatalogLanguage.fromApiCode("en-US"))
        assertEquals(CatalogLanguage.RUSSIAN, CatalogLanguage.fromApiCode(null))
        assertEquals(CatalogLanguage.RUSSIAN, CatalogLanguage.fromApiCode("invalid"))
    }
}
