package com.allday.detoxy.core.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * UI에서 사용할 텍스트를 캡슐화하는 Sealed Class.
 * Domain Layer에서 Context 없이 문자열 리소스를 참조하기 위해 사용됩니다.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(@StringRes val resId: Int, vararg val args: Any) : UiText()

    /**
     * Context를 사용하여 실제 문자열로 변환 (일반 코드용)
     */
    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }

    /**
     * Composable 컨텍스트에서 실제 문자열로 변환
     */
    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }
}
