import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color

@Composable
fun SplashScreen(
    logoResId: Int,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    holdMillis: Long = 2000,             // how long to show after the animation
    onFinished: () -> Unit = {}
) {
    var started by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(true) }

    // Animate logo in
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.85f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutBack),
        label = "logo-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "logo-alpha"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(holdMillis)
        visible = false
        onFinished()
    }

    if (visible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = null,
                modifier = Modifier
                    .size(170.dp)
                    .scale(scale)
                    .alpha(alpha)
            )
        }
    }
}
