package com.shjamolov.mediastreamplayer.domain.common

sealed interface AppError {
    val cause: Throwable?

    data class Network(
        override val cause: Throwable? = null,
    ) : AppError

    data class Storage(
        override val cause: Throwable? = null,
    ) : AppError

    data class Configuration(
        val message: String,
        override val cause: Throwable? = null,
    ) : AppError

    data class Unexpected(
        override val cause: Throwable,
    ) : AppError
}

