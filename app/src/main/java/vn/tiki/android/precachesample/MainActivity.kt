package vn.tiki.android.precachesample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    val cache by lazy {
        return@lazy cacheInstance ?: {
            val exoCacheDir = File("${cacheDir.absolutePath}/exo")
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            SimpleCache(exoCacheDir, evictor, ExoDatabaseProvider(this))
        }.invoke().also {
            cacheInstance = it
        }
    }

    val upstreamDataSourceFactory by lazy { DefaultDataSourceFactory(this, "Android") }

    val cacheDataSourceFactory by lazy {
        CacheDataSourceFactory(
            cache,
            upstreamDataSourceFactory,
            FileDataSource.Factory(),
            CacheDataSinkFactory(cache, CacheDataSink.DEFAULT_FRAGMENT_SIZE),
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
            object : CacheDataSource.EventListener {
                override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
                    Log.d(
                        TAG,
                        "onCachedBytesRead. cacheSizeBytes:$cacheSizeBytes, cachedBytesRead: $cachedBytesRead"
                    )
                }

                override fun onCacheIgnored(reason: Int) {
                    Log.d(TAG, "onCacheIgnored. reason:$reason")
                }
            }
        )
    }

    val player by lazy {
        SimpleExoPlayer.Builder(this)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                addListener(object : Player.EventListener {
                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        super.onPlayerStateChanged(playWhenReady, playbackState)
                        Log.d(
                            TAG,
                            "onPlayerStateChanged. playWhenReady: $playWhenReady, playbackState: $playbackState)"
                        )
                    }

                    override fun onPlayerError(error: ExoPlaybackException) {
                        super.onPlayerError(error)
                        Log.d(TAG, "onPlayerError")
                        error.printStackTrace()
                    }
                })
            }
    }

    var pagerLastItem = MutableLiveData(0)

    private var i = -1
    val videoDatas = listOf(
        VideoData(
            id = i++,
            streamUrl = "https://edge.tikicdn.com/data/hls/902297/1/3/1478/manifest.m3u8"
        ),
        VideoData(
            id = i++,
            streamUrl = "https://edge.tikicdn.com/data/hls/901262/1/3/1478/manifest.m3u8"
        ),
        VideoData(
            id = i++,
            streamUrl = "https://edge.tikicdn.com/data/hls/901261/1/3/1478/manifest.m3u8"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager.apply {
            adapter = VideoPagerAdapter(this@MainActivity).apply {
                showDetails = this@MainActivity.videoDatas
            }
            orientation = ViewPager2.ORIENTATION_VERTICAL
            offscreenPageLimit = 1
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        if (currentItem != pagerLastItem.value) {
                            pagerLastItem.value = currentItem
                        }
                        player.playWhenReady = true
                    } else {
                        player.playWhenReady = false
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    companion object {
        private const val CACHE_SIZE = 50 * 1024 * 1024L
        private var cacheInstance: Cache? = null
        private const val TAG = "MainActivity"
    }
}
