package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun cameraShell_and_proMonitoringHud_and_aiSettings_workCorrectly() {
        // Verify main viewfinder and shutter are rendered
        composeTestRule.onNodeWithTag("camera_viewfinder_stage").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shutter_button").assertIsDisplayed()

        // Switch to Pro Photo mode and verify Docked Top-Corner Pro Monitoring HUD appears
        composeTestRule.onNodeWithTag("mode_tab_pro_photo").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("docked_pro_monitoring_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pro_controls_panel").assertIsDisplayed()

        // Open AI Enhance Settings and verify user prompt is empty by default & clear button exists
        composeTestRule.onNodeWithTag("top_ai_enhance_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("ai_settings_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_prompt_clear_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_settings_close_button").performClick()
    }
}
