package org.flaxo.model

import org.flaxo.common.CodeStyleGrade
import org.flaxo.common.ExternalService
import org.flaxo.model.dao.BuildReportRepository
import org.flaxo.model.dao.CodeStyleReportRepository
import org.flaxo.model.dao.CommitRepository
import org.flaxo.model.dao.CourseRepository
import org.flaxo.model.dao.CredentialsRepository
import org.flaxo.model.dao.PlagiarismReportRepository
import org.flaxo.model.dao.StudentRepository
import org.flaxo.model.dao.SolutionRepository
import org.flaxo.model.dao.TaskRepository
import org.flaxo.model.dao.UserRepository
import org.flaxo.model.data.BuildReport
import org.flaxo.model.data.CodeStyleReport
import org.flaxo.model.data.Commit
import org.flaxo.model.data.Course
import org.flaxo.model.data.CourseState
import org.flaxo.model.data.Credentials
import org.flaxo.model.data.PlagiarismMatch
import org.flaxo.model.data.PlagiarismReport
import org.flaxo.model.data.Student
import org.flaxo.model.data.Solution
import org.flaxo.model.data.Task
import org.flaxo.model.data.User
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Data service implementation based on jpa repositories.
 */
open class BasicDataService(private val userRepository: UserRepository,
                            private val credentialsRepository: CredentialsRepository,
                            private val courseRepository: CourseRepository,
                            private val taskRepository: TaskRepository,
                            private val studentRepository: StudentRepository,
                            private val solutionRepository: SolutionRepository,
                            private val buildReportRepository: BuildReportRepository,
                            private val codeStyleReportRepository: CodeStyleReportRepository,
                            private val plagiarismReportRepository: PlagiarismReportRepository,
                            private val commitRepository: CommitRepository
) : DataService {

    @Transactional
    override fun addUser(nickname: String,
                         password: String
    ): User = userRepository.findByNickname(nickname)
            ?.also { throw EntityAlreadyExistsException("User $nickname") }
            ?: userRepository.save(User(
                    nickname = nickname,
                    credentials = Credentials(password = password)
            ))

    @Transactional(readOnly = true)
    override fun getUser(nickname: String): User? =
            userRepository.findByNickname(nickname)

    @Transactional(readOnly = true)
    override fun getUserByGithubId(githubId: String): User? =
            userRepository.findByGithubId(githubId)

    @Transactional
    override fun deleteUser(username: String) {
        getUser(username)
                ?.also { userRepository.delete(it) }
                ?: throw ModelException("User $username not found")
    }

    @Transactional
    override fun createCourse(courseName: String,
                              description: String?,
                              language: String,
                              testingLanguage: String,
                              testingFramework: String,
                              tasksPrefix: String,
                              numberOfTasks: Int,
                              owner: User
    ): Course = createCourse(
            courseName = courseName,
            description = description,
            language = language,
            testingLanguage = testingLanguage,
            testingFramework = testingFramework,
            tasksNames = (1..numberOfTasks).map { taskNumber -> "$tasksPrefix$taskNumber" },
            owner = owner
    )

    override fun createCourse(courseName: String,
                              description: String?,
                              language: String,
                              testingLanguage: String,
                              testingFramework: String,
                              tasksNames: List<String>,
                              owner: User
    ): Course {
        getCourse(courseName, owner)
                ?.also { throw EntityAlreadyExistsException("Course $owner/$courseName") }

        val course = courseRepository
                .save(Course(
                        name = courseName,
                        description = description,
                        url = "https://github.com/${owner.githubId}/$courseName",
                        createdDate = LocalDateTime.now(),
                        language = language,
                        testingLanguage = testingLanguage,
                        testingFramework = testingFramework,
                        state = CourseState(),
                        user = owner
                ))

        val tasks = tasksNames
                .map { branchName ->
                    taskRepository
                            .save(Task(
                                    branch = branchName,
                                    url = "${course.url}/tree/$branchName",
                                    course = course
                            ))
                }
        return updateCourse(course.copy(tasks = course.tasks.plus(tasks)))
    }

    @Transactional
    override fun deleteCourse(courseName: String,
                              owner: User
    ) {
        getCourse(courseName, owner)
                ?.also { courseRepository.delete(it) }
                ?: throw EntityNotFound("Repository $courseName")
    }

    @Transactional
    override fun updateCourse(updatedCourse: Course): Course = courseRepository.save(updatedCourse)

    @Transactional(readOnly = true)
    override fun getCourse(name: String, owner: User): Course? = courseRepository.findByNameAndUser(name, owner)

    @Transactional(readOnly = true)
    override fun getCourses(userNickname: String): Set<Course> =
            getUser(userNickname)
                    ?.let { courseRepository.findByUser(it) }
                    ?: userNotFound(userNickname)

    @Transactional
    override fun addStudent(nickname: String,
                            course: Course
    ): Student {
        val student = studentRepository.save(Student(nickname = nickname, course = course))

        return taskRepository
                .findAllByCourse(course)
                .map { task -> solutionRepository.save(Solution(task = task, student = student)) }
                .let { solutions ->
                    studentRepository.save(student.copy(
                            solutions = student.solutions.plus(solutions)
                    ))
                }
                .also { updateCourse(course.copy(students = course.students.plus(it))) }
    }

    @Transactional(readOnly = true)
    override fun getStudents(course: Course): Set<Student> = studentRepository.findByCourse(course)

    @Transactional(readOnly = true)
    override fun getSolutions(student: Student): Set<Solution> = solutionRepository.findByStudent(student)

    @Transactional(readOnly = true)
    override fun getSolutions(task: Task): Set<Solution> = solutionRepository.findByTask(task)

    @Transactional(readOnly = true)
    override fun getTasks(course: Course): Set<Task> = taskRepository.findAllByCourse(course)

    @Transactional
    override fun addToken(userNickname: String,
                          service: ExternalService,
                          accessToken: String
    ): User = getUser(userNickname)
            ?.also { user -> credentialsRepository.save(user.credentials.withServiceToken(service, accessToken)) }
            ?: userNotFound(userNickname)

    private fun Credentials.withServiceToken(service: ExternalService,
                                             accessToken: String
    ): Credentials = when (service) {
        ExternalService.GITHUB -> copy(githubToken = accessToken)
        ExternalService.TRAVIS -> copy(travisToken = accessToken)
        ExternalService.CODACY -> copy(codacyToken = accessToken)
    }

    @Transactional
    override fun addGithubId(userNickname: String,
                             githubId: String
    ): User {
        userRepository.findByGithubId(githubId)
                ?.also { throw ModelException("User with $githubId github id already exists") }

        val user: User = getUser(userNickname)
                ?: userNotFound(userNickname)

        return userRepository.save(user.copy(githubId = githubId))
    }

    private fun userNotFound(userNickname: String): Nothing {
        throw ModelException("Could not find user with $userNickname nickname")
    }

    @Transactional
    override fun updateSolution(updatedSolution: Solution): Solution = solutionRepository.save(updatedSolution)

    @Transactional
    override fun updateTask(updatedTask: Task): Task = taskRepository.save(updatedTask)

    @Transactional
    override fun addBuildReport(solution: Solution,
                                succeed: Boolean,
                                date: LocalDateTime
    ): BuildReport =
            buildReportRepository
                    .save(BuildReport(
                            solution = solution,
                            date = date,
                            succeed = succeed
                    ))
                    .also {
                        updateSolution(solution.copy(
                                buildReports = solution.buildReports + it
                        ))
                    }

    @Transactional
    override fun addCodeStyleReport(solution: Solution,
                                    codeStyleGrade: CodeStyleGrade,
                                    date: LocalDateTime
    ): CodeStyleReport =
            codeStyleReportRepository
                    .save(CodeStyleReport(
                            solution = solution,
                            date = date,
                            grade = codeStyleGrade
                    ))
                    .also {
                        updateSolution(solution.copy(
                                codeStyleReports = solution.codeStyleReports + it
                        ))
                    }

    @Transactional
    override fun addPlagiarismReport(task: Task,
                                     url: String,
                                     matches: List<PlagiarismMatch>,
                                     date: LocalDateTime
    ): PlagiarismReport =
            plagiarismReportRepository
                    .save(PlagiarismReport(
                            task = task,
                            date = date,
                            url = url,
                            matches = matches
                    ))
                    .also {
                        updateTask(task.copy(
                                plagiarismReports = task.plagiarismReports + it
                        ))
                    }

    @Transactional
    override fun addCommit(solution: Solution,
                           pullRequestId: Int,
                           commitSha: String
    ): Commit =
            commitRepository
                    .save(Commit(
                            solution = solution,
                            date = LocalDateTime.now(),
                            pullRequestId = pullRequestId,
                            sha = commitSha
                    ))
                    .also {
                        updateSolution(solution.copy(
                                commits = solution.commits + it
                        ))
                    }
}