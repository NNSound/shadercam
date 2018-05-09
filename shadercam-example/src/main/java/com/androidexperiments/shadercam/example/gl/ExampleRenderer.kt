package com.androidexperiments.shadercam.example.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20

import com.androidexperiments.shadercam.fragments.CameraFragment
import com.androidexperiments.shadercam.gl.CameraRenderer

/**
 * Example renderer that changes colors and tones of camera feed
 * based on touch position.
 */
class ExampleRenderer
/**
 * By not modifying anything, our default shaders will be used in the assets folder of shadercam.
 *
 * Base all shaders off those, since there are some default uniforms/textures that will
 * be passed every time for the camera coordinates and texture coordinates
 */
(context: Context, previewSurface: SurfaceTexture, width: Int, height: Int)//other setup if need be done here
    : CameraRenderer(context, previewSurface, width, height, "touchcolor.frag.glsl", "touchcolor.vert.glsl") {
    private var offsetR = 0.5f
    private var offsetG = 0.5f
    private var offsetB = 0.5f

    /**
     * we override [.setUniformsAndAttribs] and make sure to call the super so we can add
     * our own uniforms to our shaders here. CameraRenderer handles the rest for us automatically
     */
    override fun setUniformsAndAttribs() {
        super.setUniformsAndAttribs()

        val offsetRLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "offsetR")
        val offsetGLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "offsetG")
        val offsetBLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "offsetB")

        GLES20.glUniform1f(offsetRLoc, offsetR)
        GLES20.glUniform1f(offsetGLoc, offsetG)
        GLES20.glUniform1f(offsetBLoc, offsetB)
    }

    /**
     * take touch points on that textureview and turn them into multipliers for the color channels
     * of our shader, simple, yet effective way to illustrate how easy it is to integrate app
     * interaction into our glsl shaders
     * @param rawX raw x on screen
     * @param rawY raw y on screen
     */
    fun setTouchPoint(rawX: Float, rawY: Float) {
        offsetR = rawX / mSurfaceWidth
        offsetG = rawY / mSurfaceHeight
        offsetB = offsetR / offsetG
    }
}
