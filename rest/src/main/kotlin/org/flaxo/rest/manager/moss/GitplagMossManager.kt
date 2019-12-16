package org.flaxo.rest.manager.moss

import io.gitplag.gitplagapi.model.input.AnalysisRequest
import okhttp3.ResponseBody
import org.flaxo.common.Language
import org.flaxo.model.ModelException
import org.flaxo.model.data.Task
import org.flaxo.rest.manager.gitplag.GitplagClient
import org.flaxo.rest.manager.gitplag.toGitplagLanguage
import retrofit2.Call

class GitplagMossManager(
        private val gitplagClient: GitplagClient,
        private val restUrl: String
) : MossManager {
    override fun analyse(task: Task): Task {
        val course = task.course

        gitplagClient.analyse(
                vcsService = "github",
                username = course.user.githubId
                        ?: throw ModelException("Github id for ${course.user.name} user was not found"),
                projectName = course.name,
                analysisRequest = AnalysisRequest(
                        branch = task.branch,
                        language = toGitplagLanguage(Language.from(task.course.settings.language)),
                        responseUrl = "$restUrl/rest/gitplag-result"
                )
        ).callUnit()
        return task
    }

    private fun <T> Call<T>.callUnit(): ResponseBody? =
            execute().run { if (isSuccessful) null else errorBody() }
}