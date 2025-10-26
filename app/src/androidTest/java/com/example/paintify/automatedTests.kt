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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.NavigatorProvider
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
import com.example.paintify.screens.DrawingViewModel
import com.example.paintify.screens.DrawScreen
import com.example.paintify.screens.SplashScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Rule
import io.mockk.mockk

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
        val repo = mockk<com.example.paintify.data.DrawingRepository>(relaxed = true)
        val vm = com.example.paintify.screens.DrawingViewModel(repo)

        vm.setBrush(ShapeType.LINE)
        vm.setColor(Color.Red)

        assertEquals(ShapeType.LINE, vm.selectedBrush.value)
        assertEquals(Color.Red, vm.selectedColor.value)
    }

    @Test
    fun DragMove_updatesViewModel() {
        val repo = mockk<com.example.paintify.data.DrawingRepository>(relaxed = true)
        val vm = com.example.paintify.screens.DrawingViewModel(repo)

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
        val repo = mockk<com.example.paintify.data.DrawingRepository>(relaxed = true)
        val vm = com.example.paintify.screens.DrawingViewModel(repo)

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
    fun can_switch_brush_and_draw_on_canvas() {
        lateinit var vm: DrawingViewModel

        rule.setContent {
            vm = androidx.lifecycle.viewmodel.compose.viewModel()
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        rule.onNodeWithText("LINE").assertIsEnabled().performClick()

        val canvas = rule.onNodeWithTag("drawingCanvas")
        canvas.assertExists()
        canvas.performTouchInput {
            val c = center
            down(c)
            moveTo(Offset(c.x + 100f, c.y + 50f))
            up()
        }

        rule.runOnIdle {
            assert(vm.strokes.value.isNotEmpty())
        }
    }

    @Test
    fun slider_changes_pen_width() {
        lateinit var vm: DrawingViewModel

        rule.setContent {
            vm = androidx.lifecycle.viewmodel.compose.viewModel()
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        val slider = rule.onNodeWithTag("penWidthSlider")
        slider.assertExists()

        slider.performTouchInput {
            val c = center
            down(c)
            moveTo(androidx.compose.ui.geometry.Offset(right - 5f, c.y))
            up()
        }

        rule.runOnIdle {
            assert(vm.penWidth.value > 12f)
        }
    }

    @Test
    fun color_buttons_change_selected_color() {
        lateinit var vm: DrawingViewModel

        rule.setContent {
            vm = androidx.lifecycle.viewmodel.compose.viewModel()
            DrawScreen(navController = rememberNavController(), vm = vm)
        }

        rule.onNodeWithText("RED").performClick()
        rule.runOnIdle { assert(vm.selectedColor.value == androidx.compose.ui.graphics.Color.Red) }

        rule.onNodeWithText("BLUE").performClick()
        rule.runOnIdle { assert(vm.selectedColor.value == androidx.compose.ui.graphics.Color.Blue) }

        rule.onNodeWithText("ERASER").performClick()
        rule.runOnIdle { assert(vm.selectedColor.value == androidx.compose.ui.graphics.Color.White) }
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
