package com.allday.detoxy.data.local.converter

import androidx.room.TypeConverter
import com.allday.detoxy.domain.model.ScheduleType
import com.allday.detoxy.domain.model.TodoCompletionStatus

/**
 * ScheduleType enum TypeConverter
 */
class ScheduleTypeConverter {
    @TypeConverter
    fun fromScheduleType(type: ScheduleType): String = type.name

    @TypeConverter
    fun toScheduleType(value: String): ScheduleType = 
        ScheduleType.valueOf(value)
}

/**
 * TodoCompletionStatus enum TypeConverter
 */
class TodoCompletionStatusConverter {
    @TypeConverter
    fun fromStatus(status: TodoCompletionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TodoCompletionStatus = 
        TodoCompletionStatus.valueOf(value)
}
