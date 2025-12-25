package com.allday.detoxy.data.local.converter

import androidx.room.TypeConverter
import com.allday.detoxy.domain.model.ScheduleInfo
import com.allday.detoxy.domain.model.ScheduleTodo
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/**
 * ScheduleInfo TypeConverter
 *
 * Room에서 ScheduleInfo 객체를 JSON 문자열로 변환하여 저장합니다.
 * 
 * ## 시맨틱 규칙
 * - **null**: 스케줄 정보가 설정되지 않음 (기존 스케줄 호환)
 * - **ScheduleInfo.EMPTY**: 빈/공백 JSON 또는 파싱 실패 (잘못된 데이터)
 * - **유효한 ScheduleInfo**: 정상 파싱된 스케줄 정보
 * 
 * ## 변환 규칙
 * - null 입력 → null 반환 (스케줄 정보 없음)
 * - 빈/공백 JSON → ScheduleInfo.EMPTY 반환 (잘못된 입력)
 * - 파싱 실패 → ScheduleInfo.EMPTY 반환
 * - 빈 ID → 새 UUID 생성하여 복구
 * 
 * ## 사용 방법 (Entity 필드가 String?이므로 수동 변환 필요)
 * ```kotlin
 * // 저장 시
 * val json = ScheduleInfoConverter().fromScheduleInfo(scheduleInfo)
 * // 조회 시
 * val info = ScheduleInfoConverter().toScheduleInfo(json) // null 또는 ScheduleInfo
 * // null 체크
 * if (info == null) { /* 스케줄 정보 없음 */ }
 * else if (!info.hasContent()) { /* 빈 스케줄 정보 */ }
 * ```
 * 
 * Android 기본 org.json 패키지를 사용합니다.
 */
class ScheduleInfoConverter {

    /**
     * ScheduleInfo 객체를 JSON 문자열로 변환
     * 
     * @param info 변환할 ScheduleInfo 객체
     * @return JSON 문자열, info가 null이면 null, 직렬화 실패 시 빈 객체 JSON 반환
     */
    @TypeConverter
    fun fromScheduleInfo(info: ScheduleInfo?): String? {
        if (info == null) return null
        
        return try {
            val jsonObject = JSONObject().apply {
                put("title", info.title)
                put("description", info.description)
                put("memo", info.memo)
                put("todos", JSONArray().apply {
                    info.todos.forEach { todo ->
                        put(JSONObject().apply {
                            put("id", todo.id)
                            put("content", todo.content)
                            put("isRequired", todo.isRequired)
                            put("orderIndex", todo.orderIndex)
                        })
                    }
                })
            }
            jsonObject.toString()
        } catch (e: JSONException) {
            // 직렬화 실패 시 빈 객체 JSON 반환 (null 대신)
            // 이렇게 하면 데이터 손실 방지
            "{\"title\":\"\",\"description\":\"\",\"memo\":\"\",\"todos\":[]}"
        }
    }

    /**
     * JSON 문자열을 ScheduleInfo 객체로 변환
     * 
     * ## 반환값 시맨틱
     * - null → null 반환 (스케줄 정보 없음)
     * - 빈/공백 문자열 → ScheduleInfo.EMPTY 반환 (잘못된 입력)
     * - 유효한 JSON → 파싱된 ScheduleInfo 반환
     * - 파싱 실패 → ScheduleInfo.EMPTY 반환
     * 
     * @param json 변환할 JSON 문자열
     * @return ScheduleInfo 객체 또는 null (스케줄 정보 없음)
     */
    @TypeConverter
    fun toScheduleInfo(json: String?): ScheduleInfo? {
        // null인 경우 null 반환 (스케줄 정보 없음 시맨틱 유지)
        if (json == null) return null
        
        // 빈/공백 문자열인 경우 EMPTY 반환 (잘못된 입력)
        if (json.isBlank()) return ScheduleInfo.EMPTY
        
        return try {
            val jsonObject = JSONObject(json)
            val todos = mutableListOf<ScheduleTodo>()
            
            val todosArray = jsonObject.optJSONArray("todos")
            if (todosArray != null) {
                for (i in 0 until todosArray.length()) {
                    val todoObj = todosArray.getJSONObject(i)
                    val parsedId = todoObj.optString("id", "")
                    
                    todos.add(
                        ScheduleTodo(
                            // 빈 ID인 경우 새 UUID 생성하여 복구
                            id = if (parsedId.isBlank()) UUID.randomUUID().toString() else parsedId,
                            content = todoObj.optString("content", ""),
                            isRequired = todoObj.optBoolean("isRequired", false),
                            orderIndex = todoObj.optInt("orderIndex", 0)
                        )
                    )
                }
            }
            
            ScheduleInfo(
                title = jsonObject.optString("title", ""),
                description = jsonObject.optString("description", ""),
                memo = jsonObject.optString("memo", ""),
                todos = todos
            )
        } catch (e: JSONException) {
            ScheduleInfo.EMPTY  // 파싱 실패 시 빈 객체 반환
        } catch (e: Exception) {
            ScheduleInfo.EMPTY  // 기타 예외 시 빈 객체 반환
        }
    }
}
