package com.allday.detoxy.data.local.converter

import com.allday.detoxy.domain.model.TodoCompletionStatus
import com.allday.detoxy.domain.model.TodoStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * TodoStatus 리스트 JSON 변환기
 * 
 * FocusSessionTodoResultEntity의 todoResultsJson 필드 변환용
 */
object TodoResultConverter {

    /**
     * TodoStatus 리스트를 JSON 문자열로 변환
     */
    fun toJson(todos: List<TodoStatus>): String {
        return try {
            val jsonArray = JSONArray()
            todos.forEach { todo ->
                jsonArray.put(JSONObject().apply {
                    put("id", todo.id)
                    put("content", todo.content)
                    put("isRequired", todo.isRequired)
                    put("status", todo.status.name)
                    put("isGoal", todo.isGoal)
                })
            }
            jsonArray.toString()
        } catch (e: JSONException) {
            "[]"
        }
    }

    /**
     * JSON 문자열을 TodoStatus 리스트로 변환
     */
    fun fromJson(json: String?): List<TodoStatus> {
        if (json.isNullOrBlank()) return emptyList()
        
        return try {
            val jsonArray = JSONArray(json)
            val result = mutableListOf<TodoStatus>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    TodoStatus(
                        id = obj.optString("id", ""),
                        content = obj.optString("content", ""),
                        isRequired = obj.optBoolean("isRequired", false),
                        status = try {
                            TodoCompletionStatus.valueOf(obj.optString("status", "NO_RESPONSE"))
                        } catch (e: Exception) {
                            TodoCompletionStatus.NO_RESPONSE
                        },
                        isGoal = obj.optBoolean("isGoal", false)
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
