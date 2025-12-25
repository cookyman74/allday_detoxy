package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.allday.detoxy.domain.model.ScheduleType

/**
 * 집중 세션 할일 결과 엔티티
 *
 * 세션 종료 시 할일/목표의 완료 상태를 저장합니다.
 * 스케줄 삭제 시에도 히스토리를 유지하기 위해 FK 없이 스냅샷으로 저장합니다.
 *
 * @property sessionId 세션 ID (PK)
 * @property scheduleId 스케줄 ID (FK 없음 - 스케줄 삭제 허용)
 * @property scheduleType 스케줄 타입 (TIME_BASED / LOCATION_BASED)
 * @property scheduleTitleSnapshot 스케줄 제목 스냅샷 (삭제된 스케줄 표시용)
 * @property todoResultsJson 할일 결과 JSON (List<TodoStatus> 직렬화)
 * @property completedAt 완료 시각 (timestamp)
 * @property lastModifiedAt 마지막 수정 시각 (할일 관리 페이지에서 수정 시)
 */
@Entity(tableName = "focus_session_todo_result")
data class FocusSessionTodoResultEntity(
    @PrimaryKey
    val sessionId: String,
    
    val scheduleId: String,
    
    val scheduleType: ScheduleType,
    
    val scheduleTitleSnapshot: String,
    
    val todoResultsJson: String,
    
    val completedAt: Long,
    
    val lastModifiedAt: Long? = null
)
