package vn.tiki.android.precachesample

import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class VideoPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    var showDetails = listOf<VideoData>()

    override fun getItemCount(): Int {
        return showDetails.size
    }

    override fun createFragment(position: Int): Fragment {
        return VideoFragment().apply {
            arguments = bundleOf(
                VideoFragment.KEY_POSITION to position
            )

        }
    }
}
