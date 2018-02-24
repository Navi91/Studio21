package com.studio21.android.view.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import butterknife.BindView
import com.google.android.exoplayer2.SimpleExoPlayer

import com.studio21.android.R
import com.studio21.android.view.Studio21Fragment

class RadioFragment : Studio21Fragment() {

    @BindView(R.id.volume)
    lateinit var volumeSeekBar: SeekBar

    lateinit var player: SimpleExoPlayer

    companion object {
        fun newInstance(): RadioFragment {
            val fragment = RadioFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.f_radio, container, false)

        butter(view)

//        volumeSeekBar.setPadding(0, 0, 0, 0)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


    }
}
