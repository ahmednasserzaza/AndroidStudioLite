package com.worldcup.androidstudiolite.domain.exception

sealed class DomainException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class Network(message: String = "Network error", cause: Throwable? = null) :
        DomainException(message, cause)

    class Auth(message: String) : DomainException(message)

    class GitHub(message: String) : DomainException(message)

    class Ai(message: String, cause: Throwable? = null) : DomainException(message, cause)

    class Validation(message: String) : DomainException(message)
}
