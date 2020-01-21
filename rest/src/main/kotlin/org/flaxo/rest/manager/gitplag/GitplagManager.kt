package org.flaxo.rest.manager.gitplag

import arrow.core.Either
import io.gitplag.gitplagapi.model.enums.AnalysisMode
import io.gitplag.gitplagapi.model.enums.AnalyzerProperty
import io.gitplag.gitplagapi.model.enums.GitProperty
import io.gitplag.gitplagapi.model.input.RepositoryInput
import okhttp3.ResponseBody
import org.flaxo.common.Language
import org.flaxo.model.ModelException
import org.flaxo.model.data.Course
import org.flaxo.rest.manager.UnsupportedLanguageException
import org.flaxo.rest.manager.ValidationManager
import retrofit2.Call

/**
 * Gitplag manager.
 */
class GitplagManager(private val gitplagClient: GitplagClient) : ValidationManager {

    override fun activate(course: Course) {
        val language = Language.from(course.settings.language)
                ?: throw UnsupportedLanguageException("Course ${course.name} does not have language property")
        val userGithubId = course.user.githubId
                ?: throw ModelException("Github id for ${course.user.name} user was not found")
        gitplagClient.addRepository(RepositoryInput(
                git = GitProperty.GITHUB,
                language = toGitplagLanguage(language),
                name = "$userGithubId/${course.name}",
                analyzer = AnalyzerProperty.MOSS,
                filePatterns = listOf(language.extensionsRegexp),
                analysisMode = AnalysisMode.FULL
        )).callUnit()
        gitplagClient.updateRepositoryFiles(
                vcsService = "github",
                username = userGithubId,
                projectName = course.name
        ).callUnit()
    }

    override fun deactivate(course: Course) = Unit

    override fun refresh(course: Course) {
        val language = Language.from(course.settings.language)
                ?: throw UnsupportedLanguageException("Course ${course.name} does not have language property")
        val userGithubId = course.user.githubId
                ?: throw ModelException("Github id for ${course.user.name} user was not found")
        gitplagClient.updateRepository(
                "github",
                userGithubId,
                course.name,
                RepositoryInput(
                git = GitProperty.GITHUB,
                language = toGitplagLanguage(language),
                name = "$userGithubId/${course.name}",
                analyzer = AnalyzerProperty.MOSS,
                filePatterns = listOf(course.settings.filePatterns ?: language.extensionsRegexp),
                analysisMode = AnalysisMode.FULL
        )).callUnit()
    }

    private fun <T> Call<T>.call(): Either<ResponseBody, T> =
            execute().run {
                if (isSuccessful) Either.right(body()!!)
                else Either.left(errorBody()!!)
            }

    private fun <T> Call<T>.callUnit(): ResponseBody? =
            execute().run { if (isSuccessful) null else errorBody() }
}