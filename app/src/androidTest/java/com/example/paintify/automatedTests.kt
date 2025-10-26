/**
 * Paintify - Automated Tests
 * --------------------------------
 * Runs Compose UI and ViewModel integration tests on an Android device/emulator.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * Verifies core behaviors including brush/color selection, stroke creation via gestures,
 * pen width clamping and slider changes, splash visibility timing, and canvas interactions.
 */

package com.example.paintify

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.rememberNavController
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.paintify.Navigation.AppNavHost
import com.example.paintify.models.ShapeType
import com.example.paintify.models.ToolType
import com.example.paintify.screens.DrawingViewModel
import com.example.paintify.screens.DrawScreen
import com.example.paintify.screens.DrawingViewModelProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Rule
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class automatedTests {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.paintify", appContext.packageName)
    }

    @Test
    fun SetBrushandSetColor_updatesViewModel() {
        lateinit var vm: DrawingViewModel

        rule.setContent {
            vm = viewModel(factory = DrawingViewModelProvider.Factory)
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        vm.setBrush(ShapeType.LINE)
        vm.setColor(Color.Red)

        assertEquals(ShapeType.LINE, vm.selectedBrush.value)
        assertEquals(Color.Red, vm.selectedColor.value)
    }

    @Test
    fun DragMove_updatesViewModel() {
        lateinit var vm: DrawingViewModel

        rule.setContent {
            vm = viewModel(factory = DrawingViewModelProvider.Factory)
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        vm.setBrush(ShapeType.CIRCLE)
        vm.setColor(Color.Black)
        vm.setWidthPx(10f)

        vm.onDragStart(Offset(10f, 10f))
        vm.onDragMove(Offset(20f, 20f))
        vm.onDragMove(Offset(40f, 40f))
        vm.onDragEnd()

        val strokes = vm.strokes.value
        assertTrue(strokes.isNotEmpty())
        assertEquals(ShapeType.CIRCLE, strokes.last().brush)
        assertEquals(Color.Black, strokes.last().color)
        assertEquals(10f, strokes.last().widthPx, 0.01f)
        assertTrue(strokes.last().points.size >= 2)
    }

    @Test
    fun PenWidth() {
        lateinit var vm: DrawingViewModel

        rule.setContent {
            vm = viewModel(factory = DrawingViewModelProvider.Factory)
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        vm.setWidthPx(-5f)
        assertEquals(1f, vm.penWidth.value, 0.0f)

        vm.setWidthPx(500f)
        assertEquals(128f, vm.penWidth.value, 0.0f)
    }

    @Test
    fun splashScreen_showsLogo() {
        // Make test time deterministic
        rule.mainClock.autoAdvance = false

        var finished = false
        rule.setContent {
            com.example.paintify.screens.SplashScreen(
                logoResId = R.drawable.logo, // ensure this exists in androidTest or main
                holdMillis = 800,
                onFinished = { finished = true }
            )
        }

        // Initial frame
        rule.onNodeWithTag("splashLogo").assertExists()

        // Advance just before hold -> still visible
        rule.mainClock.advanceTimeBy(799)
        rule.onNodeWithTag("splashLogo").assertExists()


    }


    @Test
    fun slider_changes_pen_width() {
        lateinit var vm: DrawingViewModel

        rule.setContent {
            vm = viewModel(factory = DrawingViewModelProvider.Factory)
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        val slider = rule.onNodeWithTag("penWidthSlider")
        slider.assertExists()

        slider.performTouchInput {
            val c = center
            down(c)
            moveTo(Offset(right - 5f, c.y))
            up()
        }

        rule.runOnIdle {
            assert(vm.penWidth.value > 12f)
        }
    }

    @Test
    fun color_picker_sets_color_and_eraser_toggle_switches_tool() {
        lateinit var vm: DrawingViewModel

        rule.setContent {
            vm = viewModel(factory = DrawingViewModelProvider.Factory)
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        // Open color picker
        rule.onNodeWithTag("openColorPicker").assertExists().performClick()

        // Push RED to max (255)
        val red = rule.onNodeWithTag("redSlider")
        red.assertExists()
        red.performTouchInput {
            val c = center
            down(c)
            moveTo(Offset(right - 5f, c.y))
            up()
        }

        // Set GREEN and BLUE near 0
        rule.onNodeWithTag("greenSlider").performTouchInput {
            val c = center
            down(c)
            moveTo(Offset(left + 5f, c.y))
            up()
        }
        rule.onNodeWithTag("blueSlider").performTouchInput {
            val c = center
            down(c)
            moveTo(Offset(left + 5f, c.y))
            up()
        }

        // Confirm
        rule.onNodeWithText("Select").performClick()

        // Verify VM color ~ pure red
        rule.runOnIdle {
            val c = vm.selectedColor.value
            fun ch(x: Float) = (x * 255f).roundToInt()
            val r = ch(c.red)
            val g = ch(c.green)
            val b = ch(c.blue)

            // allow ±2 for rounding
            assert(r in 240..255) { "Expected R≈255, got $r" }
            assert(g in 0..10)     { "Expected G≈0, got $g" }
            assert(b in 0..10)     { "Expected B≈0, got $b" }
        }
        // Toggle eraser ON
        rule.onNodeWithTag("eraserToggle").assertExists().performClick()
        rule.runOnIdle {
            assert(vm.toolType.value == ToolType.ERASER)
            // also white selected by design
            val c = vm.selectedColor.value
            assert(abs(c.red - 1f) < 0.01f && abs(c.green - 1f) < 0.01f && abs(c.blue - 1f) < 0.01f)
        }

        // Toggle eraser OFF (back to pen)
        rule.onNodeWithTag("eraserToggle").performClick()
        rule.runOnIdle {
            assert(vm.toolType.value == ToolType.PEN)
        }
    }

    @Test
    fun rgb_picker_color_applies_to_new_stroke() {
        lateinit var vm: DrawingViewModel
        rule.setContent {
            vm = viewModel(factory = DrawingViewModelProvider.Factory)
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        // Open color picker and set to purple-ish: R=200,G=0,B=200
        rule.onNodeWithTag("openColorPicker").performClick()

        fun moveSliderToPercent(tag: String, pct: Float) {
            rule.onNodeWithTag(tag).performTouchInput {
                val c = center
                val x = left + (right - left) * pct
                down(c)
                moveTo(Offset(x, c.y))
                up()
            }
        }

        moveSliderToPercent("redSlider", 200f / 255f)
        moveSliderToPercent("greenSlider", 0f)
        moveSliderToPercent("blueSlider", 200f / 255f)
        rule.onNodeWithText("Select").performClick()

        // Draw a stroke
        rule.onNodeWithTag("drawingCanvas").performTouchInput {
            val c = center
            down(c); moveTo(Offset(c.x + 80f, c.y + 30f)); up()
        }

        // Verify last stroke color ~ (200,0,200)
        rule.runOnIdle {
            val last = vm.strokes.value.last()
            assert(abs(last.color.red - 200f / 255f) < 0.03f)
            assert(last.color.green < 0.03f)
            assert(abs(last.color.blue - 200f / 255f) < 0.03f)
        }
    }


    @Test
    fun pen_width_slider_changes_stroke_width() {
        lateinit var vm: DrawingViewModel
        rule.setContent {
            vm = viewModel(factory = DrawingViewModelProvider.Factory)
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        // Baseline stroke at default width
        rule.onNodeWithTag("drawingCanvas").performTouchInput {
            val c = center; down(c); moveTo(Offset(c.x + 50f, c.y)); up()
        }
        var baseWidth = 0f
        rule.runOnIdle { baseWidth = vm.strokes.value.last().widthPx }

        // Slide width near max
        rule.onNodeWithTag("penWidthSlider").performTouchInput {
            val c = center
            down(c); moveTo(Offset(right - 5f, c.y)); up()
        }

        // Draw again
        rule.onNodeWithTag("drawingCanvas").performTouchInput {
            val c = center; down(c); moveTo(Offset(c.x + 50f, c.y + 10f)); up()
        }

        // Width should increase
        rule.runOnIdle {
            val newWidth = vm.strokes.value.last().widthPx
            assert(newWidth > baseWidth)
        }
    }

    @Test
    fun eraser_toggle_draws_white_strokes() {
        lateinit var vm: DrawingViewModel
        rule.setContent {
            vm = viewModel(factory = DrawingViewModelProvider.Factory)
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        // Ensure pen on a colored stroke first
        rule.onNodeWithTag("openColorPicker").performClick()
        // set a non-white color (e.g., green mid)
        rule.onNodeWithTag("greenSlider").performTouchInput {
            val c = center; down(c); moveTo(Offset((left + right) / 2f, c.y)); up()
        }
        rule.onNodeWithText("Select").performClick()

        rule.onNodeWithTag("drawingCanvas").performTouchInput {
            val c = center; down(c); moveTo(Offset(c.x + 60f, c.y)); up()
        }
        rule.runOnIdle {
            val c = vm.strokes.value.last().color
            assert(!(abs(c.red - 1f) < 0.01f && abs(c.green - 1f) < 0.01f && abs(c.blue - 1f) < 0.01f))
        }

        // Turn eraser on
        rule.onNodeWithTag("eraserToggle").performClick()
        rule.runOnIdle { assert(vm.toolType.value == ToolType.ERASER) }

        // Draw eraser stroke
        rule.onNodeWithTag("drawingCanvas").performTouchInput {
            val c = center; down(c); moveTo(Offset(c.x + 60f, c.y + 15f)); up()
        }
        rule.runOnIdle {
            val c = vm.strokes.value.last().color
            // white ≈ (1,1,1)
            assert(abs(c.red - 1f) < 0.02f && abs(c.green - 1f) < 0.02f && abs(c.blue - 1f) < 0.02f)
        }
    }
}

@RunWith(AndroidJUnit4::class)
@LargeTest
class AppNavHostNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()  // NOTE: not createAndroidComposeRule<MainActivity>()

    private fun makeNavController(context: Context): TestNavHostController {
        return TestNavHostController(context).apply {
            // Register Compose + Dialog navigators on the existing provider (don’t replace it)
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())
        }
    }

    @Test
    fun splash_navigates_to_home_after_delay() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val navController = makeNavController(ctx)

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppNavHost(navController = navController, startDestination = "splash")
        }

        // Initially on splash
        assertEquals("splash", navController.currentBackStackEntry?.destination?.route)

        // Advance past your SplashScreen’s holdMillis (800ms in your code)
        composeRule.mainClock.advanceTimeBy(1200L)
        composeRule.waitForIdle()

        assertEquals("home", navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun home_fab_new_navigates_to_canvas() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val navController = makeNavController(ctx)

        composeRule.setContent {
            AppNavHost(navController = navController, startDestination = "home")
        }

        composeRule.onNodeWithContentDescription("New").performClick()
        assertEquals("canvas", navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun navigate_to_detail_and_back_returns_home() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val navController = makeNavController(ctx)

        composeRule.setContent {
            AppNavHost(navController = navController, startDestination = "home")
        }

        composeRule.runOnUiThread {
            navController.navigate("detail/42")
        }
        composeRule.waitForIdle()

        assertEquals("detail/{id}", navController.currentBackStackEntry?.destination?.route)
        assertEquals(42L, navController.currentBackStackEntry?.arguments?.getLong("id"))

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()

        assertEquals("home", navController.currentBackStackEntry?.destination?.route)
    }
}
