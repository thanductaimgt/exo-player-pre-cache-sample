package vn.tiki.android.precachesample

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.hls.offline.HlsDownloader
import kotlinx.android.synthetic.main.fragment_video.*
import kotlinx.coroutines.*

class VideoFragment : Fragment() {
    private val mainActivity: MainActivity
        get() = activity as MainActivity

    private val player: SimpleExoPlayer
        get() = mainActivity.player

    private val uri by lazy { Uri.parse(mainActivity.videoDatas[position].streamUrl) }

    private val cacheStreamKeys = arrayListOf(
        StreamKey(0, 1),
        StreamKey(1, 1),
        StreamKey(2, 1),
        StreamKey(3, 1),
        StreamKey(4, 1)
    )

    private val mediaSource: MediaSource by lazy {
        val dataSourceFactory = mainActivity.cacheDataSourceFactory
        HlsMediaSource.Factory(dataSourceFactory)
            .setStreamKeys(cacheStreamKeys)
            .setAllowChunklessPreparation(true)
            .createMediaSource(uri)
    }

    private var position = -2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        position = arguments?.getInt(KEY_POSITION) ?: -2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity.pagerLastItem.observe(viewLifecycleOwner, {
            if (it == position) {
                cancelPreCache()
                Log.d(TAG, "Cancel preload at position: $position")
                player.stop(true)
                player.setVideoSurfaceView(renderView)
                player.prepare(mediaSource)
            }
        })

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            preCacheVideo()
        }
    }

    private val downloadConstructorHelper by lazy {
        DownloaderConstructorHelper(
            mainActivity.cache,
            mainActivity.upstreamDataSourceFactory,
            mainActivity.cacheDataSourceFactory,
            null,
            null
        )
    }

    private val downloader by lazy {
        HlsDownloader(uri, cacheStreamKeys, downloadConstructorHelper)
    }

    private fun cancelPreCache() {
        downloader.cancel()
    }

    private suspend fun preCacheVideo() = withContext(Dispatchers.IO) {
        runCatching {
            // do nothing if already cache enough
            if (mainActivity.cache.isCached(uri.toString(), 0, PRE_CACHE_SIZE)) {
                Log.d(TAG, "video has been cached, return")
                return@runCatching
            }

            Log.d(TAG, "start pre-caching for position: $position")

            downloader.download { contentLength, bytesDownloaded, percentDownloaded ->
                if (bytesDownloaded >= PRE_CACHE_SIZE) downloader.cancel()
                Log.d(
                    TAG,
                    "contentLength: $contentLength, bytesDownloaded: $bytesDownloaded, percentDownloaded: $percentDownloaded"
                )
            }
        }.onFailure {
            if (it is InterruptedException) return@onFailure

            Log.d(TAG, "Cache fail for position: $position with exception: $it}")
            it.printStackTrace()
        }.onSuccess {
            Log.d(TAG, "Cache success for position: $position")
        }
        Unit
    }

    companion object {
        const val KEY_POSITION = "KEY_POSITION"
        private const val PRE_CACHE_SIZE = 5 * 1024 * 1024L
        private const val TAG = "VideoFragment"
    }
}
