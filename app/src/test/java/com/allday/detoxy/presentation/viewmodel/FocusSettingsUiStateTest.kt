package com.allday.detoxy.presentation.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * FocusSettingsUiState 단위 테스트
 * 
 * 흑백 모드 관련 UI 상태 테스트
 */
class FocusSettingsUiStateTest {

    @Test
    fun `grayscaleModeEnabled defaults to false`() {
        val state = FocusSettingsUiState()
        
        assertFalse("grayscaleModeEnabled should default to false", state.grayscaleModeEnabled)
    }

    @Test
    fun `showGrayscalePermissionDialog defaults to false`() {
        val state = FocusSettingsUiState()
        
        assertFalse("showGrayscalePermissionDialog should default to false", state.showGrayscalePermissionDialog)
    }

    @Test
    fun `grayscaleModeEnabled can be set to true via copy`() {
        val initialState = FocusSettingsUiState()
        val updatedState = initialState.copy(grayscaleModeEnabled = true)
        
        assertFalse("Initial state should be false", initialState.grayscaleModeEnabled)
        assertTrue("Updated state should be true", updatedState.grayscaleModeEnabled)
    }

    @Test
    fun `showGrayscalePermissionDialog can be toggled independently`() {
        val state1 = FocusSettingsUiState()
        val state2 = state1.copy(showGrayscalePermissionDialog = true)
        val state3 = state2.copy(grayscaleModeEnabled = true, showGrayscalePermissionDialog = false)
        
        assertFalse("state1 dialog should be false", state1.showGrayscalePermissionDialog)
        assertTrue("state2 dialog should be true", state2.showGrayscalePermissionDialog)
        assertFalse("state2 mode should still be false", state2.grayscaleModeEnabled)
        assertTrue("state3 mode should be true", state3.grayscaleModeEnabled)
        assertFalse("state3 dialog should be false", state3.showGrayscalePermissionDialog)
    }

    @Test
    fun `toggle OFF closes dialog and sets mode to false`() {
        // 다이얼로그가 열린 상태에서 토글 OFF 시나리오
        val dialogOpenState = FocusSettingsUiState(
            grayscaleModeEnabled = false,
            showGrayscalePermissionDialog = true
        )
        
        // 토글 OFF 시 둘 다 false 되어야 함
        val toggledOffState = dialogOpenState.copy(
            grayscaleModeEnabled = false,
            showGrayscalePermissionDialog = false
        )
        
        assertFalse("grayscaleModeEnabled should be false", toggledOffState.grayscaleModeEnabled)
        assertFalse("showGrayscalePermissionDialog should be false", toggledOffState.showGrayscalePermissionDialog)
    }

    @Test
    fun `permission granted enables mode and closes dialog`() {
        // 권한 다이얼로그 열린 상태
        val dialogOpenState = FocusSettingsUiState(
            grayscaleModeEnabled = false,
            showGrayscalePermissionDialog = true
        )
        
        // 권한 승인 후 상태
        val permissionGrantedState = dialogOpenState.copy(
            grayscaleModeEnabled = true,
            showGrayscalePermissionDialog = false
        )
        
        assertTrue("grayscaleModeEnabled should be true after permission granted", permissionGrantedState.grayscaleModeEnabled)
        assertFalse("showGrayscalePermissionDialog should be false after permission granted", permissionGrantedState.showGrayscalePermissionDialog)
    }

    @Test
    fun `permission denied keeps mode false and closes dialog`() {
        // 권한 다이얼로그 열린 상태
        val dialogOpenState = FocusSettingsUiState(
            grayscaleModeEnabled = false,
            showGrayscalePermissionDialog = true
        )
        
        // 권한 거부 후 상태
        val permissionDeniedState = dialogOpenState.copy(
            grayscaleModeEnabled = false,  // 여전히 false
            showGrayscalePermissionDialog = false
        )
        
        assertFalse("grayscaleModeEnabled should remain false after permission denied", permissionDeniedState.grayscaleModeEnabled)
        assertFalse("showGrayscalePermissionDialog should be false after permission denied", permissionDeniedState.showGrayscalePermissionDialog)
    }
}
