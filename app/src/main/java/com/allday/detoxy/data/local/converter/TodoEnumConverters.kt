package com.allday.detoxy.data.local.converter

import androidx.room.TypeConverter
import com.allday.detoxy.domain.model.ScheduleType
import com.allday.detoxy.domain.model.TodoCompletionStatus

/**
 * ScheduleType enum TypeConverter
 * 
 * 잘못된 값이 들어올 경우 TIME_BASED로 fallback 처리
 */
class ScheduleTypeConverter {
    @TypeConverter
    fun fromScheduleType(type: ScheduleType): String = type.name

    @TypeConverter
    fun toScheduleType(value: String): ScheduleType =
        try {
            ScheduleType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ScheduleType.TIME_BASED  // fallback
        }
}

/**
 * TodoCompletionStatus enum TypeConverter
 * 
 * 잘못된 값이 들어올 경우 NO_RESPONSE로 fallback 처리
 */
class TodoCompletionStatusConverter {
    @TypeConverter
    fun fromStatus(status: TodoCompletionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TodoCompletionStatus =
        try {
            TodoCompletionStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TodoCompletionStatus.NO_RESPONSE  // fallback
        }
}
