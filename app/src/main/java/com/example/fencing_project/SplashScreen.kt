package com.example.fencing_project

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    val revealProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            )
        )
        delay(300)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(25,25,33)),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .matchParentSize(),


        ) {
            // Базовая картинка
            Image(
                painter = painterResource(R.drawable.logo6),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Серая картинка с "проявлением"
       /* Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    val height = size.height * revealProgress.value
                    clipRect(
                        left = 0f,
                        top = size.height - height,
                        right = size.width,
                        bottom = size.height
                    ) {
                        this@drawWithContent.drawContent()
                    }
                }
        ) {
            Image(
                painter = painterResource(R.drawable.logo52),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center)
            )
        }*/
    }
}

