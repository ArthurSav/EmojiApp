package com.emojiapp

import com.emojiapp.api.phrase
import com.emojiapp.app.*
import com.emojiapp.model.EPSession
import com.emojiapp.model.User
import com.emojiapp.repository.DatabaseFactory
import com.emojiapp.repository.EmojiPhrasesRepository
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import com.squareup.moshi.kotlin.reflect.*
import freemarker.cache.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import org.slf4j.event.*
import java.net.*
import java.util.concurrent.*
import com.ryanharter.ktor.moshi.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(DefaultHeaders)
    install(StatusPages) {
        exception<Throwable> { e ->
            call.respondText(
                e.localizedMessage,
                ContentType.Text.Plain,
                HttpStatusCode.InternalServerError
            )
        }
    }
    install(ContentNegotiation) {
        moshi() {
            this.add(KotlinJsonAdapterFactory())
        }
    }
    install(CallLogging) {
        level = Level.INFO
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    install(Locations)
    install(Sessions) {
        cookie<EPSession>("SESSION") {
            transform(SessionTransportTransformerMessageAuthentication(hashKey))
        }
    }

    val hashFunction = { s: String -> hash(s) }
    DatabaseFactory.init()

    val db = EmojiPhrasesRepository()

    routing {

        static("/static") {
            resources("images")
        }

        home(db)
        about(db)
        phrases(db, hashFunction)
        signin(db, hashFunction)
        signout()
        signup(db, hashFunction)

        //api
        phrase(db)
    }
}

const val API_VERSION = "/api/v1"

suspend fun ApplicationCall.redirect(location: Any) {
    respondRedirect(application.locations.href(location))
}

fun ApplicationCall.refererHost() = request.header(HttpHeaders.Referrer)?.let { URI.create(it).host }

fun ApplicationCall.securityCode(date: Long, user: User, hashFunction: (String) -> String) =
    hashFunction("$date:${user.userId}:${request.host()}:${refererHost()}")

fun ApplicationCall.verifyCode(date: Long, user: User, code: String, hashFunction: (String) -> String) =
    (System.currentTimeMillis() - date).let { it > 0 && it < TimeUnit.MICROSECONDS.convert(2, TimeUnit.HOURS) }

