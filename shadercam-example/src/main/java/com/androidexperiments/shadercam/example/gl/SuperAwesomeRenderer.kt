package com.androidexperiments.shadercam.example.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.os.SystemClock

import com.androidexperiments.shadercam.gl.CameraRenderer

/**
 * Our super awesome shader. It calls its super constructor with the new
 * glsl files we've created for this. Then it overrides [.setUniformsAndAttribs]
 * to pass in our global time uniform
 */
class SuperAwesomeRenderer(context: Context, texture: SurfaceTexture, width: Int, height: Int) : CameraRenderer(context, texture, width, height, "superawesome.frag.glsl", "superawesome.vert.glsl") {
    private var mTileAmount = 1f

    override fun setUniformsAndAttribs() {
        //always call super so that the built-in fun stuff can be set first
        super.setUniformsAndAttribs()

        val globalTimeHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "iGlobalTime")
        GLES20.glUniform1f(globalTimeHandle, SystemClock.currentThreadTimeMillis() / 100.0f)

        val resolutionHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "iResolution")
        GLES20.glUniform3f(resolutionHandle, mTileAmount, mTileAmount, 1f)
    }

    fun setTileAmount(tileAmount: Float) {
        this.mTileAmount = tileAmount
    }
}
