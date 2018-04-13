package com.androidexperiments.shadercam.example

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.widget.SeekBar

import com.androidexperiments.shadercam.example.gl.SuperAwesomeRenderer
import com.androidexperiments.shadercam.gl.CameraRenderer

/**
 * For our NYC Android Developers Meetup, we've created a super simple
 * implementation of ShaderCam, with sliders
 */
class MeetupActivity : SimpleShaderActivity(), SeekBar.OnSeekBarChangeListener {
    private var mMyRenderer: SuperAwesomeRenderer? = null
    private var mSeekbar: SeekBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSeekbar = findViewById(R.id.seek_bar) as SeekBar
        mSeekbar!!.setOnSeekBarChangeListener(this)
    }

    /**
     * 好吧我可能用很爛的解法來處理kotlin呼叫的問題
     * 我不確定正確呼叫的方式是麼
     * 這個寫法只是能夠編譯 需要動到fun的時候還是會crash
     * */
    override fun getRenderer(surface: SurfaceTexture, width: Int, height: Int): CameraRenderer {
        var mMyRenderer = SuperAwesomeRenderer(this, surface, width, height)
        //var mMyRenderer = new SuperAwesomeRenderer(this, surface, width, height) how can i do this?

        return mMyRenderer
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        mMyRenderer!!.setTileAmount(map(progress.toFloat(), 0f, 100f, 0.1f, 1.9f))
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        //dont need
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        //dont need
    }

    /**
     * Takes a value, assumes it falls between start1 and stop1, and maps it to a value
     * between start2 and stop2.
     *
     * For example, above, our slide goes 0-100, starting at 50. We map 0 on the slider
     * to .1f and 100 to 1.9f, in order to better suit our shader calculations
     */
    internal fun map(value: Float, start1: Float, stop1: Float, start2: Float, stop2: Float): Float {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1))
    }
}
