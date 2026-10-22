package com.withpet.car.media

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.withpet.car.domain.model.CalmPlaybackState
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.repository.CalmSoundPlayer
import com.withpet.car.domain.repository.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** [CalmPlaybackService]에 MediaController로 연결해 재생을 제어한다. */
@Singleton
class MediaControllerCalmSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
) : CalmSoundPlayer {

    private val mutex = Mutex()
    private var controller: MediaController? = null
    private val _state = MutableStateFlow(CalmPlaybackState())
    override val state: StateFlow<CalmPlaybackState> = _state.asStateFlow()

    override suspend fun play(situation: CalmSituation): Result<Unit> = runCatching {
        val playlist = requireNotNull(playlistRepository.get(situation)) { "플레이리스트 없음: $situation" }
        // 음원은 scripts/generate_calm_sounds.py로 합성한 임시본. 출시용 음원이 정해지면 res/raw만 교체한다 (13장).
        val items = playlist.tracks.map { track ->
            MediaItem.Builder()
                .setMediaId("${situation.name}/${track.id}")
                .setUri("android.resource://${context.packageName}/raw/${track.rawResName}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist("위드펫 · ${playlist.title}")
                        .build(),
                )
                .build()
        }
        withContext(Dispatchers.Main) {
            controller().run {
                setMediaItems(items)
                prepare()
                play()
            }
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            controller?.run {
                stop()
                clearMediaItems()
            }
        }
    }

    private suspend fun controller(): MediaController = mutex.withLock {
        controller ?: connect().also { controller = it }
    }

    private suspend fun connect(): MediaController {
        val token = SessionToken(context, ComponentName(context, CalmPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val controller = suspendCancellableCoroutine { cont ->
            future.addListener({
                runCatching { future.get() }
                    .onSuccess { cont.resume(it) }
                    .onFailure { cont.resumeWithException(it) }
            }, ContextCompat.getMainExecutor(context))
            cont.invokeOnCancellation { future.cancel(true) }
        }
        controller.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = publish(player)
        })
        publish(controller)
        return controller
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = CalmPlaybackState(
            situation = item?.mediaId?.substringBefore('/')
                ?.let { id -> CalmSituation.entries.find { it.name == id } },
            trackTitle = item?.mediaMetadata?.title?.toString(),
            isPlaying = player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING),
        )
    }
}
