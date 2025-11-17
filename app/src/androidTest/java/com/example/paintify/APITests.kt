package com.example.paintify

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.paintify.screens.AnalysisContent
import com.example.paintify.screens.AnalysisViewModel
import com.example.paintify.screens.DetectedLabel
import com.example.paintify.screens.DetectedObject
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun analysisContent_displaysObjectsAndLabelsCorrectly() {
        val fakeUri = Uri.parse("file://dummy/path/image.png")
        val fakeObjects = listOf(
            DetectedObject(
                name = "cat",
                category = "animal",
                confidence = 0.95f,
                xMin = 0.1f,
                yMin = 0.2f,
                xMax = 0.4f,
                yMax = 0.6f
            )
        )
        val fakeLabels = listOf(
            DetectedLabel(name = "cat", confidence = 0.95f),
            DetectedLabel(name = "pet", confidence = 0.80f)
        )

        composeTestRule.setContent {
            MaterialTheme {
                AnalysisContent(
                    imageUri = fakeUri,
                    imageWidth = 800,
                    imageHeight = 600,
                    labels = fakeLabels,
                    objects = fakeObjects
                )
            }
        }

        composeTestRule
            .onNodeWithText("Detected Objects")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Labels")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("cat")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Confidence: 95%  animal")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("- cat — 95%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("- pet — 80%")
            .assertIsDisplayed()
    }


    private fun invokeParseDetectionJson(json: String): Pair<List<DetectedLabel>, List<DetectedObject>> {
        val vm = AnalysisViewModel()
        val method = AnalysisViewModel::class.java.getDeclaredMethod(
            "parseDetectionJson",
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(vm, json) as Pair<List<DetectedLabel>, List<DetectedObject>>
    }

    @Test
    fun parseDetectionJson_parsesLabelsAndObjects() {
        val json = """
            {
              "labels": [
                { "name": "cat", "confidence": 0.95 },
                { "name": "pet", "confidence": 0.80 }
              ],
              "objects": [
                {
                  "name": "cat",
                  "category": "animal",
                  "confidence": 0.93,
                  "box": {
                    "xMin": 0.1,
                    "yMin": 0.2,
                    "xMax": 0.4,
                    "yMax": 0.6
                  }
                }
              ]
            }
        """.trimIndent()

        val (labels, objects) = invokeParseDetectionJson(json)

        assertEquals(2, labels.size)
        assertEquals("cat", labels[0].name)
        assertEquals(0.95f, labels[0].confidence, 1e-3f)

        assertEquals("pet", labels[1].name)
        assertEquals(0.80f, labels[1].confidence, 1e-3f)

        assertEquals(1, objects.size)
        val obj = objects[0]
        assertEquals("cat", obj.name)
        assertEquals("animal", obj.category)
        assertEquals(0.93f, obj.confidence, 1e-3f)
        assertEquals(0.1f, obj.xMin, 1e-3f)
        assertEquals(0.2f, obj.yMin, 1e-3f)
        assertEquals(0.4f, obj.xMax, 1e-3f)
        assertEquals(0.6f, obj.yMax, 1e-3f)
    }

    @Test
    fun parseDetectionJson_noDetections_returnsEmptyLists() {
        val json = """
            {
              "labels": [],
              "objects": []
            }
        """.trimIndent()

        val (labels, objects) = invokeParseDetectionJson(json)

        assertEquals(0, labels.size)
        assertEquals(0, objects.size)
    }

    @Test
    fun analysisContent_noDetections_showsFriendlyMessages() {
        val fakeUri = Uri.parse("file://dummy/path/image.png")

        composeTestRule.setContent {
            MaterialTheme {
                AnalysisContent(
                    imageUri = fakeUri,
                    imageWidth = 800,
                    imageHeight = 600,
                    labels = emptyList(),
                    objects = emptyList()
                )
            }
        }

        composeTestRule.onNodeWithText("Detected Objects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Labels").assertIsDisplayed()


        composeTestRule
            .onNodeWithText("No objects detected.")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("No labels detected.")
            .assertIsDisplayed()
    }


}
