package org.flaxo.rest.manager.environment

import org.amshove.kluent.shouldThrow
import org.flaxo.common.framework.JUnitTestingFramework
import org.flaxo.common.framework.SpekTestingFramework
import org.flaxo.common.framework.TestingFramework
import org.flaxo.common.lang.JavaLang
import org.flaxo.common.lang.KotlinLang
import org.flaxo.common.lang.Language
import org.flaxo.gradle.GradleBuildTool
import org.flaxo.rest.manager.IncompatibleLanguageException
import org.flaxo.rest.manager.IncompatibleTestingFrameworkException
import org.flaxo.travis.env.SimpleTravisEnvironmentSupplier
import org.flaxo.travis.env.TravisEnvironmentSupplier
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object RepositoryEnvironmentServiceSpec : SubjectSpek<EnvironmentManager>({
    val firstLanguage = "java"
    val secondLanguage = "kotlin"
    val firstTestingFramework = "junit"
    val secondTestingFramework = "spek"

    val languages: List<Language> = listOf(JavaLang, KotlinLang)
    val testingFrameworks: List<TestingFramework> = listOf(JUnitTestingFramework, SpekTestingFramework)
    val travisEnvironmentSupplier: TravisEnvironmentSupplier =
            SimpleTravisEnvironmentSupplier(travisWebHookUrl = "travisWebHookUrl")
    val defaultBuildTools: Map<Language, GradleBuildTool> = mapOf(
            JavaLang to GradleBuildTool(travisEnvironmentSupplier),
            KotlinLang to GradleBuildTool(travisEnvironmentSupplier)
    )

    subject { SimpleEnvironmentManager(languages, testingFrameworks, defaultBuildTools) }

    describe("repository environment service") {

        on("creating an environment with incompatible languages") {
            it("should throw an IncompatibleLanguage exception") {
                {
                    subject.produceEnvironment(secondLanguage, firstLanguage, firstTestingFramework)
                } shouldThrow IncompatibleLanguageException::class
            }
        }

        on("creating an environment with incompatible testing language and testing framework") {
            it("should throw an IncompatibleTestingFramework exception") {
                {
                    subject.produceEnvironment(firstLanguage, firstLanguage, secondTestingFramework)
                } shouldThrow IncompatibleTestingFrameworkException::class
            }
        }

        on("creating an environment with compatible languages and framework") {
            it("should successfully create an environment") {
                subject.produceEnvironment(firstLanguage, firstLanguage, firstTestingFramework)
            }
        }
    }

})