package org.flaxo.rest.manager.gitplag

import io.gitplag.gitplagapi.model.output.analysis.AnalysisResult
import org.flaxo.model.DataManager
import org.flaxo.model.data.PlagiarismMatch
import org.flaxo.moss.MossMatch
import org.flaxo.moss.MossResult
import org.flaxo.rest.manager.CourseNotFoundException
import org.flaxo.rest.manager.TaskNotFoundException
import org.flaxo.rest.manager.UserNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@RestController
class GitplagAnalysisResponseController(
        private val dataManager: DataManager,
        @Value("\${flaxo.gitplag.ui.url}") private val gitplagUiUrl: String
) {

    @PostMapping("/rest/gitplag-result")
    fun saveAnalysisResult(@RequestBody body: AnalysisResult) {

        val matches = body.analysisPairs.map {
            MossMatch(
                    students = it.student1 to it.student2,
                    link = "$gitplagUiUrl/analyzes/${body.id}/pairs/${it.id}",
                    percentage = it.percentage,
                    lines = 0)
        }.toSet()

        val (repoOwner, courseName) = body.repoName.split("/")

        val user = dataManager.getUserByGithubId(repoOwner) ?: throw UserNotFoundException(repoOwner)

        val course = dataManager.getCourse(courseName, user) ?: throw CourseNotFoundException(repoOwner, courseName)

        val task = (course.tasks.find { it.branch == body.branch }
                ?: throw TaskNotFoundException(repoOwner, courseName, body.branch))

        val mossResult = MossResult(
                url = URL("$gitplagUiUrl/analyzes/${body.id}"),
                matches = matches,
                students = course.students.map { it.name }
        )

        val plagiarismReport = dataManager.addPlagiarismReport(
                task = task,
                url = mossResult.url.toString(),
                matches = mossResult.matches.map {
                    PlagiarismMatch(
                            student1 = it.students.first,
                            student2 = it.students.second,
                            lines = it.lines,
                            url = it.link,
                            percentage = it.percentage
                    )
                }
        )

        dataManager.updateTask(task.copy(
                plagiarismReports = task.plagiarismReports.plus(plagiarismReport)
        ))
    }

}