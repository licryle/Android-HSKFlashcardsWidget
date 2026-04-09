package fr.berliat.hskwidget.ui.screens.widget

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue

import org.jetbrains.compose.resources.stringResource

import platform.AVFoundation.*
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.UIKit.UIView
import platform.CoreGraphics.CGRectZero

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.ui.components.PrettyCard
import fr.berliat.hskwidget.widgets_intro

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WidgetsListScreen(
    onWidgetPreferenceSaved: (Int) -> Unit,
    expectsActivityResult: Boolean,
    modifier: Modifier,
    selectedWidgetId: Int?,
    viewModel: WidgetsListViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(Res.string.widgets_intro),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(24.dp))

        val videoUrl = remember {
            NSBundle.mainBundle.URLForResource("AddWidget", "MP4")
        }

        if (videoUrl != null) {
            PrettyCard(
                borderColor = Color.Red
            ) {
                VideoPlayer(
                    url = videoUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 19.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
fun VideoPlayer(url: NSURL, modifier: Modifier = Modifier) {
    val player = remember(url) {
        AVPlayer.playerWithURL(url).apply {
            muted = true
        }
    }

    val playerLayer = remember(player) {
        AVPlayerLayer.playerLayerWithPlayer(player).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
        }
    }

    LaunchedEffect(player) {
        player.play()
    }

    DisposableEffect(player) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player.currentItem,
            queue = null
        ) { _ ->
            player.seekToTime(CMTimeMake(0, 1))
            player.play()
        }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            player.pause()
        }
    }

    UIKitView(
        factory = {
            val container = object : UIView(CGRectZero.readValue()) {
                override fun layoutSubviews() {
                    super.layoutSubviews()
                    CATransaction.begin()
                    CATransaction.setDisableActions(true)
                    playerLayer.frame = bounds
                    CATransaction.commit()
                }
            }
            container.layer.addSublayer(playerLayer)
            container.clipsToBounds = true
            container
        },
        update = {
            player.play()
        },
        modifier = modifier
    )
}
