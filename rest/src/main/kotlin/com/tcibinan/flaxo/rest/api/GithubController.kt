package com.tcibinan.flaxo.rest.api

import com.tcibinan.flaxo.git.GitPayload
import com.tcibinan.flaxo.git.PullRequest
import com.tcibinan.flaxo.github.GithubException
import com.tcibinan.flaxo.model.DataService
import com.tcibinan.flaxo.model.IntegratedService
import com.tcibinan.flaxo.rest.service.git.GitService
import com.tcibinan.flaxo.rest.service.response.ResponseService
import org.apache.commons.collections4.map.PassiveExpiringMap
import org.apache.http.client.fluent.Form
import org.apache.http.client.fluent.Request
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.Reader
import java.security.Principal
import java.util.*
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/rest/github")
class GithubController(
        private val responseService: ResponseService,
        private val dataService: DataService,
        private val gitService: GitService,
        @Value("\${GITHUB_ID}") private val clientId: String,
        @Value("\${GITHUB_SECRET}") private val clientSecret: String
) {

    private val githubAuthUrl = "https://github.com/login/oauth"
    private val states: MutableMap<String, String> = PassiveExpiringMap(
            TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES)
    )
    private val logger = LogManager.getLogger(GithubController::class.java)

    @GetMapping("/auth")
    @PreAuthorize("hasAuthority('USER')")
    fun githubAuth(principal: Principal): Any {
        val state = Random().nextInt().toString()

        synchronized(states) {
            states[principal.name] = state
        }

        return responseService.ok(object {
            val redirect = "$githubAuthUrl/authorize"
            val params = mapOf(
                    "client_id" to clientId,
                    "scope" to listOf("delete_repo", "repo").joinToString(separator = " "),
                    "state" to state
            )
        })
    }

    @GetMapping("/auth/code")
    @Transactional
    fun githubAuthToken(@RequestParam("code") code: String,
                        @RequestParam("state") state: String,
                        response: HttpServletResponse
    ) {
        val accessToken = Request.Post("$githubAuthUrl/access_token")
                .bodyForm(
                        Form.form().apply {
                            add("client_id", clientId)
                            add("client_secret", clientSecret)
                            add("code", code)
                            add("state", state)
                        }.build()
                )
                .execute()
                .returnContent()
                .asString()
                .split("&")
                .find { it.startsWith("access_token") }
                ?.split("=")
                ?.last()
                ?: throw GithubException("Access token was not received from github.")

        val nickname = synchronized(states) {
            val key = states.filterValues { it == state }
                    .apply {
                        if (size > 1)
                            throw GithubException("Two users have the same random state for github auth.")
                    }
                    .keys.first()

            states.remove(key)
            key
        }

        val githubId = gitService.with(accessToken).nickname()

        dataService.addGithubId(nickname, githubId)
        dataService.addToken(nickname, IntegratedService.GITHUB, accessToken)

        response.sendRedirect("/")
    }

    @PostMapping("/hook")
    @Transactional
    fun webHook(request: HttpServletRequest) {
        val payloadReader: Reader = request.getParameter("payload").reader()
        val headers: Map<String, List<String>> =
                request.headerNames
                        .toList()
                        .map { it.toLowerCase() to listOf(request.getHeader(it)) }
                        .toMap()
        val hook: GitPayload? = gitService.parsePayload(payloadReader, headers)

        when (hook) {
            is PullRequest -> {
                if (hook.isOpened) {
                    logger.info("Github opening pull request web hook received from ${hook.authorId} " +
                            "to ${hook.receiverId}/${hook.receiverRepositoryName}")

                    val user = dataService.getUserByGithubId(hook.receiverId)
                            ?: throw GithubException("User with githubId ${hook.receiverId} wasn't found in database.")

                    val course = dataService.getCourse(hook.receiverRepositoryName, user)
                            ?: throw GithubException("Course ${hook.receiverRepositoryName} wasn't found for user ${user.nickname}.")

                    val student = course.students.find { it.nickname == hook.authorId }
                            ?: dataService.addStudent(hook.authorId, course)

                    logger.info("Student ${student.nickname} was initialised for course ${user.nickname}/${course.name}.")
                } else {
                    logger.info("Github updating pull request web hook received from ${hook.authorId} " +
                            "to ${hook.receiverId}/${hook.receiverRepositoryName}.")

                    // do nothing
                }
            }
            else -> {
                logger.info("Github custom web hook received from request: $request.")

                //do nothing
            }
        }
    }

}

