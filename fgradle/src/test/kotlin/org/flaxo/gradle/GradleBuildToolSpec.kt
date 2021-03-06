package org.flaxo.gradle

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainSome
import org.amshove.kluent.shouldEqual
import org.flaxo.common.Framework
import org.flaxo.common.Language
import org.flaxo.common.env.EnvironmentSupplier
import org.flaxo.common.env.SimpleEnvironment
import org.flaxo.common.env.file.EnvironmentFile
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object GradleBuildToolSpec : SubjectSpek<GradleBuildTool>({

    val firstPlugin = GradlePlugin("java")
    val secondPlugin = GradlePlugin("application")
    val firstDependency = GradleDependency("a", "b", "c")
    val secondDependency = GradleDependency("1", "2", "3")
    val testingDependency = GradleDependency("t", "y", "u", type = GradleDependencyType.TEST_COMPILE)
    val compilingDependency = GradleDependency("t", "y", "u", type = GradleDependencyType.COMPILE)
    val travis: EnvironmentSupplier = mock {
        on { with(any(), any(), any()) }.thenReturn(it)
        on { environment() }.thenReturn(SimpleEnvironment(emptySet()))
    }

    subject {
        GradleBuildTool(travis).with(Language.Java, Language.Java, Framework.JUnit) as GradleBuildTool
    }

    describe("Gradle build tool") {
        on("plugins addition") {
            val buildTool =
                    subject.addPlugin(firstPlugin)
                            .addPlugin(secondPlugin)
                            .addPlugin(secondPlugin)
            val environment = buildTool.environment()
            val buildGradle = environment.files()
                    .find { it.fileName == "build.gradle" }
                    ?: throw GradleException("build.gradle wasn't found")

            it("should have build.gradle with all passed plugins") {
                buildGradle.content shouldContainSome listOf("apply plugin: ", "id ")
                buildGradle.shouldHaveName(firstPlugin, secondPlugin)
            }

            it("should have build.gradle with a single line for repeated plugins") {
                buildGradle.shouldHaveSingle(secondPlugin)
            }
        }

        on("dependency addition") {
            val buildTool =
                    subject.addPlugin(javaPlugin())
                            .addDependency(firstDependency)
                            .addDependency(secondDependency)
                            .addDependency(secondDependency)
            val environment = buildTool.environment()
            val buildGradle = environment.files()
                    .find { it.fileName == "build.gradle" }
                    ?: throw GradleException("build.gradle wasn't found")

            it("should have build.gradle with build.gradle containing single dependency") {
                buildGradle.shouldHaveName(firstDependency, secondDependency)
            }

            it("should have build.gradle with a single line for repeated dependencies") {
                buildGradle.shouldHaveSingleName(secondDependency)
            }
        }

        on("different types of dependencies addition") {
            val buildTool =
                    subject.addPlugin(javaPlugin())
                            .addDependency(testingDependency)
                            .addDependency(compilingDependency)
            val environment = buildTool.environment()
            val buildGradle = environment.files()
                    .find { it.fileName == "build.gradle" }
                    ?: throw GradleException("build.gradle wasn't found")

            it("should contain all dependencies") {
                buildGradle.shouldHaveDependency(testingDependency, compilingDependency)
            }

        }
    }
})

private fun EnvironmentFile.shouldHaveName(vararg plugins: GradlePlugin) {
    plugins.map { it.id }
            .forEach {
                content shouldContainSome listOf(
                        "apply plugin: \"$it\"",
                        "id \"$it\""
                )
            }
}

private fun EnvironmentFile.shouldHaveSingle(vararg plugins: GradlePlugin) {
    plugins.map { it.id }
            .forEach {
                content
                        .split(
                                "apply plugin: \"$it\"",
                                "id \"$it\""
                        )
                        .size - 1 shouldEqual 1
            }
}

private fun EnvironmentFile.shouldHaveName(vararg dependencies: GradleDependency) {
    dependencies.forEach {
        content shouldContain it.name
    }
}

private fun EnvironmentFile.shouldHaveDependency(vararg dependencies: GradleDependency) {
    dependencies.forEach {
        content shouldContain it.toString()
    }
}

private fun EnvironmentFile.shouldHaveSingleName(vararg dependencies: GradleDependency) {
    dependencies.forEach {
        content
                .split(it.name)
                .size - 1 shouldEqual 1
    }
}