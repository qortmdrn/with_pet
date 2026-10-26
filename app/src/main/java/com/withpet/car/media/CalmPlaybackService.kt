package com.withpet.car.media

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 안정 사운드 재생. 앱 화면을 벗어나도 계속 재생되고 차량 미디어 컨트롤과 연동된다. */
@AndroidEntryPoint
class CalmPlaybackService : MediaSessionService() {

    @Inject lateinit var volume: AppPlaybackVolume

    private var session: MediaSession? = null
    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_ALL }
        session = MediaSession.Builder(this, player).build()
        // 펫 모드가 정한 앱 재생 볼륨 (pleos). fake 빌드는 바뀌지 않아 100%
        scope.launch { volume.percent.collect { player.volume = it / 100f } }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
