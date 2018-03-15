package com.tcibinan.flaxo.model.data

import com.tcibinan.flaxo.model.entity.TaskEntity
import com.tcibinan.flaxo.model.entity.toDtos

/**
 * Task data object.
 */
data class Task(private val entity: TaskEntity)
    : DataObject<TaskEntity> {

    val id: Long by lazy { entity.taskId ?: missing("id") }
    val name: String by lazy { entity.taskName ?: missing("name") }
    val course: Course by lazy { Course(entity.course ?: missing("course")) }
    val mossUrl: String? by lazy { entity.mossUrl }
    val studentTasks: Set<StudentTask> by lazy { entity.studentTasks.toDtos() }

    override fun toEntity() = entity

    fun with(id: Long? = null,
             name: String? = null,
             course: Course? = null,
             mossUrl: String? = null,
             studentTasks: Set<StudentTask> = emptySet()
    ): Task = TaskEntity()
            .apply {
                this.taskId = id ?: entity.taskId
                this.taskName = name ?: entity.taskName
                this.course = course?.toEntity() ?: entity.course
                this.mossUrl = mossUrl ?: entity.mossUrl
                this.studentTasks = studentTasks.takeIf { it.isNotEmpty() }?.toEntities() ?: entity.studentTasks
            }
            .toDto()

}