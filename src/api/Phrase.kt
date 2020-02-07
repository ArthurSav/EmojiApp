package com.emojiapp.api

import com.emojiapp.API_VERSION
import com.emojiapp.model.*
import com.emojiapp.repository.*
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

const val PHRASE_ENDPOINT = "$API_VERSION/phrase"

fun Route.phrase(db: Repository) {

    post(PHRASE_ENDPOINT) {
        val request = call.receive<Request>()
        val phrase = db.add("", request.emoji, request.phrase)
        call.respond(phrase)
    }
}