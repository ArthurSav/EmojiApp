package com.emojiapp.app

import com.emojiapp.*
import com.emojiapp.model.*
import com.emojiapp.repository.*
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*

const val SIGNUP = "/signup"

@Location(SIGNUP)
data class Signup(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val error: String = ""
)

fun Route.signup(db: Repository, hashFunction: (String) -> String) {

    post<Signup> {
        val user = call.sessions.get<EPSession>()?.let { db.user(it.userId) }
        if (user != null) return@post call.redirect(Phrases())

        val signupParameters = call.receive<Parameters>()
        val userId = signupParameters["userId"] ?: return@post call.redirect(it)
        val password = signupParameters["password"] ?: return@post call.redirect(it)
        val displayName = signupParameters["displayName"] ?: return@post call.redirect(it)
        val email = signupParameters["email"] ?: return@post call.redirect(it)

        val signUpError = Signup(userId, displayName, email)

        when {
            password.length < MIN_PASSWORD_LENGTH -> call.redirect(signUpError.copy(error = "Password should be at least $MIN_PASSWORD_LENGTH characters long"))
            userId.length < MIN_USER_ID_LENGTH -> call.redirect(signUpError.copy(error = "Username should be at least $MIN_USER_ID_LENGTH characters long"))
            !userNameValid(userId) -> call.redirect(signUpError.copy(error = "Username should be consists of digits, letters, dots or underscores"))
            db.user(userId) != null -> call.redirect(signUpError.copy(error = "User with the following username is already registered"))
            else -> {
                val hash = hashFunction(password)
                val newUser = User(userId, email, displayName, hash)

                try {
                    db.createUser(newUser)
                } catch (e: Throwable) {
                    when {
                        db.user(userId) != null -> call.redirect(signUpError.copy(error = "User with the following username is already registered"))
                        db.userByEmail(email) != null -> call.redirect(signUpError.copy(error = "User with the following email $email is already registered"))
                        else -> {
                            application.log.error("Failed to register user", e)
                            call.redirect(signUpError.copy(error = "Failed to register"))
                        }
                    }
                }

                call.sessions.set(EPSession(newUser.userId))
                call.redirect(Phrases())
            }
        }
    }

    get<Signup> {
        val user = call.sessions.get<EPSession>()?.let { db.user(it.userId) }
        if (user != null) {
            call.redirect(Phrases())
        } else {
            call.respond(FreeMarkerContent("signup.ftl", mapOf("error" to it.error)))
        }
    }

}