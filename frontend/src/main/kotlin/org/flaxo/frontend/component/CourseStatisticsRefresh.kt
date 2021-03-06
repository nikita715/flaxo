package org.flaxo.frontend.component

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import org.flaxo.common.data.Course
import org.flaxo.common.data.CourseLifecycle
import org.flaxo.frontend.Container
import org.flaxo.frontend.Notifications
import org.flaxo.frontend.OnCourseStatisticsChange
import org.flaxo.frontend.client.FlaxoHttpException
import org.flaxo.frontend.credentials
import react.RBuilder
import react.dom.button
import react.dom.i

/**
 * Adds course statistics refresh button.
 */
fun RBuilder.courseStatisticsRefresh(course: Course, onUpdate: OnCourseStatisticsChange) {
    button(classes = "btn btn-outline-info icon-btn") {
        attrs {
            onClickFunction = { GlobalScope.launch { synchronizeCourseStatistics(course, onUpdate) } }
            disabled = course.state.lifecycle == CourseLifecycle.INIT
        }
        i(classes = "material-icons") { +"refresh" }
    }
}

private suspend fun synchronizeCourseStatistics(course: Course, onUpdate: OnCourseStatisticsChange) {
    credentials?.also {
        try {
            Notifications.info("Course statistics refreshing was initiated.")
            val updatedStatistics = Container.flaxoClient.syncCourse(it, course.name)
            onUpdate(updatedStatistics)
            Notifications.success("Course statistics synchronization has been finished.")
        } catch (e: FlaxoHttpException) {
            console.log(e)
            Notifications.error("Error occurred during ${course.name} course statistics synchronization.", e)
        }
    }
}
