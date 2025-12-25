package com.allday.detoxy.data.local.converter

import androidx.room.TypeConverter
import com.allday.detoxy.domain.model.ScheduleInfo
import com.allday.detoxy.domain.model.ScheduleTodo
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * ScheduleInfo TypeConverter
 *
 * Room에서 ScheduleInfo 객체를 JSON 문자열로 변환하여 저장합니다.
 * 파싱 실패 시 빈 ScheduleInfo 객체로 fallback 처리합니다.
 * 
 * Android 기본 org.json 패키지를 사용합니다.
 */
class ScheduleInfoConverter {

    /**
     * ScheduleInfo 객체를 JSON 문자열로 변환
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
            null
        }
    }

    /**
     * JSON 문자열을 ScheduleInfo 객체로 변환
     * 파싱 실패 시 빈 ScheduleInfo 객체 반환
     */
    @TypeConverter
    fun toScheduleInfo(json: String?): ScheduleInfo? {
        if (json.isNullOrBlank()) return null
        
        return try {
            val jsonObject = JSONObject(json)
            val todos = mutableListOf<ScheduleTodo>()
            
            val todosArray = jsonObject.optJSONArray("todos")
            if (todosArray != null) {
                for (i in 0 until todosArray.length()) {
                    val todoObj = todosArray.getJSONObject(i)
                    todos.add(
                        ScheduleTodo(
                            id = todoObj.optString("id", ""),
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
            ScheduleInfo.EMPTY  // fallback
        } catch (e: Exception) {
            ScheduleInfo.EMPTY  // fallback
        }
    }
}
