package com.shjamolov.mediastreamplayer.domain.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppResultTest {
    @Test
    fun map_transformsSuccessValue() {
        val result = AppResult.Success(21).map { it * 2 }

        assertEquals(AppResult.Success(42), result)
    }

    @Test
    fun map_preservesFailure() {
        val error = AppError.Configuration("Missing API key")
        val failure = AppResult.Failure(error)

        val result = failure.map { "unreachable" }

        assertSame(failure, result)
    }

    @Test
    fun fold_selectsFailureBranch() {
        val result = AppResult.Failure(AppError.Network()).fold(
            onSuccess = { "success" },
            onFailure = { "failure" },
        )

        assertEquals("failure", result)
    }
}

