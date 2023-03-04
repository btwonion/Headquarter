package dev.nyon.headquarters.server.routings

import dev.nyon.headquarters.api.user.User
import dev.nyon.headquarters.server.database.users
import dev.nyon.headquarters.server.httpClient
import dev.nyon.headquarters.server.json
import dev.nyon.headquarters.server.session.UserSession
import dev.nyon.headquarters.server.util.generateAndCheckID
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.title
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.litote.kmongo.eq

fun Route.configureUserLoginRoot() {
    authenticate("auth-oauth-github") {
        get("/login") {}

        get("/callback") {
            val principal: OAuthAccessTokenResponse.OAuth2 = call.principal() ?: error("No principal")
            call.sessions.set(UserSession(principal.accessToken))
            val githubUserResponse = httpClient.get {
                url("https://api.github.com/user")
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header(HttpHeaders.Authorization, "Bearer ${principal.accessToken}")
                header("X-GitHub-Api-Version", "2022-11-28")
            }.bodyAsText()

            val githubUser = json.parseToJsonElement(githubUserResponse).jsonObject

            val id = githubUser["id"]!!.jsonPrimitive.content
            var user = users.findOne(User::githubID eq id)
            if (user == null) {
                user = User(generateAndCheckID(8, users), id, listOf())
                users.insertOne(user)
            }
            call.sessions.set(user)

            call.respondHtml {
                head {
                    title {
                        +"GitHub OAuth Success"
                    }
                }

                body {
                    h1 {
                        +"Success!"
                    }
                }
            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.sessions.clear<User>()
        }
    }
}