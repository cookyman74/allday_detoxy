# Phase 4. 차단 오버레이 할일 표시 상세 작업 지시서

> **상위 문서**: [할 일 기능 작업 계획](../10_추가기능_할일관리기능_todolist.md)  
> **기능 설계**: [할 일 기능 설계 문서](../10_추가기능_할일관리기능.md)

---

> **🎯 핵심 MVP 기능**: 차단된 앱 실행 시 목표/할일 표시로 동기 상기

---

## 4.1 오버레이 UI 수정

### 4.1.1 OverlayDisplayRules

**파일**: `app/src/main/java/com/allday/detoxy/presentation/overlay/OverlayDisplayRules.kt`

```kotlin
object OverlayDisplayRules {
    const val MAX_TODO_DISPLAY_COUNT = 3      // 오버레이에 표시할 최대 할일 개수
    const val MAX_TODO_TEXT_LENGTH = 20       // 할일 텍스트 최대 길이 (초과 시 "...")
    const val MAX_GOAL_TEXT_LENGTH = 30       // 목표 텍스트 최대 길이
}

fun truncateForOverlay(text: String, maxLength: Int): String =
    if (text.length > maxLength) "${text.take(maxLength)}..." else text
```

### 4.1.2 LockOverlayService 수정

**파일**: `app/src/main/java/com/allday/detoxy/service/LockOverlayService.kt`

```kotlin
// 오버레이 뷰에 할일 섹션 추가
private fun createOverlayView(): View {
    // 기존 오버레이 뷰 생성 로직...
    
    // 🆕 할일 섹션 추가
    val todoSection = createTodoSection()
    overlayRootView.addView(todoSection)
    
    return overlayRootView
}

private fun createTodoSection(): View {
    val scheduleInfo = getCurrentScheduleInfo()  // 🔧 아래 구현 참조
    val prefs = preferenceManager
    
    // 프라이버시 설정 확인
    if (prefs.hideGoalOnOverlay) {
        return createEmojiOnlyView()  // 🔧 아래 구현 참조
    }
    if (!prefs.showTodoOnOverlay) {
        return View(this)  // 빈 뷰
    }
    
    return if (prefs.showDetailedTodoOnOverlay && scheduleInfo.todos.isNotEmpty()) {
        createTodoListView(scheduleInfo)
    } else {
        createGoalOnlyView(scheduleInfo)
    }
}

// 🔧 8차 리뷰: 미구현 함수 추가
private fun getCurrentScheduleInfo(): ScheduleInfo {
    // FocusSessionManager에서 현재 세션의 스케줄 정보 조회
    val currentSchedule = focusSessionManager.currentSchedule ?: return ScheduleInfo()
    return currentSchedule.scheduleInfo ?: ScheduleInfo()
}

private fun createEmojiOnlyView(): View {
    // 이모지만 표시하는 뷰 (프라이버시 보호)
    return TextView(this).apply {
        text = "🎯"
        textSize = 48f
        gravity = Gravity.CENTER
        setPadding(16, 16, 16, 16)
    }
}

private fun createGoalOnlyView(info: ScheduleInfo): View {
    // 메인 목표만 표시
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        padding = 16.dp
        
        // 헤더
        addView(TextView(context).apply {
            text = "🎯 오늘의 목표"
            textSize = 14f
        })
        
        // 목표 텍스트
        addView(TextView(context).apply {
            text = truncateForOverlay(info.title, OverlayDisplayRules.MAX_GOAL_TEXT_LENGTH)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        })
    }
}

private fun createTodoListView(info: ScheduleInfo): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        padding = 16.dp
        
        // 헤더
        addView(TextView(context).apply {
            text = "📋 지금 해야 할 일"
            textSize = 14f
        })
        
        // 할일 목록 (최대 3개)
        val displayTodos = info.todos.take(OverlayDisplayRules.MAX_TODO_DISPLAY_COUNT)
        displayTodos.forEach { todo ->
            addView(createTodoItemView(todo))
        }
        
        // 더 있으면 "+N개 더" 표시
        val remaining = info.todos.size - OverlayDisplayRules.MAX_TODO_DISPLAY_COUNT
        if (remaining > 0) {
            addView(TextView(context).apply {
                text = "...외 ${remaining}개"
                textSize = 12f
            })
        }
    }
}

private fun createTodoItemView(todo: ScheduleTodo): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        
        // 체크박스 아이콘
        addView(TextView(context).apply { text = "□" })
        
        // 할일 내용
        addView(TextView(context).apply {
            text = truncateForOverlay(todo.content, OverlayDisplayRules.MAX_TODO_TEXT_LENGTH)
        })
        
        // 필수 뱃지
        if (todo.isRequired) {
            addView(TextView(context).apply {
                text = "[필수]"
                setTextColor(Color.RED)
            })
        }
    }
}
```

---

## 4.2 프라이버시 옵션

### 4.2.1 PreferenceManager 확장

**파일**: `app/src/main/java/com/allday/detoxy/data/preference/PreferenceManager.kt`

```kotlin
// 🆕 오버레이 할일 표시 설정
var showTodoOnOverlay: Boolean
    get() = prefs.getBoolean(KEY_SHOW_TODO_OVERLAY, true)
    set(value) = prefs.edit().putBoolean(KEY_SHOW_TODO_OVERLAY, value).apply()

var showDetailedTodoOnOverlay: Boolean
    get() = prefs.getBoolean(KEY_SHOW_DETAILED_TODO_OVERLAY, false)
    set(value) = prefs.edit().putBoolean(KEY_SHOW_DETAILED_TODO_OVERLAY, value).apply()

var hideGoalOnOverlay: Boolean
    get() = prefs.getBoolean(KEY_HIDE_GOAL_OVERLAY, false)
    set(value) = prefs.edit().putBoolean(KEY_HIDE_GOAL_OVERLAY, value).apply()

companion object {
    private const val KEY_SHOW_TODO_OVERLAY = "show_todo_overlay"
    private const val KEY_SHOW_DETAILED_TODO_OVERLAY = "show_detailed_todo_overlay"
    private const val KEY_HIDE_GOAL_OVERLAY = "hide_goal_overlay"
}
```

### 4.2.2 설정 화면 UI

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/settings/PrivacySettingsSection.kt`

```kotlin
@Composable
fun PrivacySettingsSection(
    showTodoOnOverlay: Boolean,
    showDetailedTodo: Boolean,
    hideGoal: Boolean,
    onShowTodoChange: (Boolean) -> Unit,
    onShowDetailedChange: (Boolean) -> Unit,
    onHideGoalChange: (Boolean) -> Unit
) {
    SettingsSection(title = "🔒 오버레이 프라이버시") {
        SwitchPreference(
            title = "오버레이에 목표 표시",
            description = "차단 화면에 집중 목표를 표시합니다",
            checked = showTodoOnOverlay,
            onCheckedChange = onShowTodoChange
        )
        
        SwitchPreference(
            title = "세부 할일 표시",
            description = "개별 할일 항목까지 표시합니다",
            checked = showDetailedTodo,
            enabled = showTodoOnOverlay,
            onCheckedChange = onShowDetailedChange
        )
        
        SwitchPreference(
            title = "목표 숨기기 (이모지만)",
            description = "민감한 목표를 숨기고 이모지만 표시합니다",
            checked = hideGoal,
            onCheckedChange = onHideGoalChange
        )
    }
}
```

### 4.2.3 우선순위 규칙 구현

```kotlin
fun determineOverlayDisplayMode(prefs: PreferenceManager): OverlayDisplayMode {
    return when {
        prefs.hideGoalOnOverlay -> OverlayDisplayMode.EMOJI_ONLY
        !prefs.showTodoOnOverlay -> OverlayDisplayMode.HIDDEN
        !prefs.showDetailedTodoOnOverlay -> OverlayDisplayMode.GOAL_ONLY
        else -> OverlayDisplayMode.GOAL_AND_TODOS
    }
}

enum class OverlayDisplayMode {
    EMOJI_ONLY,      // 이모지만 표시
    HIDDEN,          // 모두 숨김
    GOAL_ONLY,       // 목표만 표시
    GOAL_AND_TODOS   // 목표 + 할일 표시
}
```

---

## 검증 체크리스트

```bash
# 빌드 확인
./gradlew assembleDebug
```

### 수동 QA

1. 할일 포함 스케줄 생성
2. 세션 시작
3. 차단된 앱 실행
4. 오버레이에 할일 표시 확인
5. 설정에서 프라이버시 옵션 변경
6. 각 설정별 표시 확인:
   - 목표만 표시
   - 목표 + 할일 표시
   - 이모지만 표시
   - 모두 숨김

---

## 완료 기준

- [ ] OverlayDisplayRules object 구현
- [ ] truncateForOverlay() 함수 구현
- [ ] LockOverlayService에 할일 표시 UI 추가
- [ ] 최대 3개 할일 + "+N개 더" 표시
- [ ] PreferenceManager에 프라이버시 설정 추가
- [ ] 설정 화면에 프라이버시 옵션 UI 추가
- [ ] 우선순위 규칙 동작 확인

---

*작성일: 2025-12-25 · Phase 4 상세 작업 지시서*
