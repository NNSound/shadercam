package com.androidexperiments.shadercam.example

import android.Manifest
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentTransaction
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.Toast

import com.androidexperiments.shadercam.example.gl.ExampleRenderer
import com.androidexperiments.shadercam.fragments.CameraFragment
import com.androidexperiments.shadercam.fragments.PermissionsHelper
import com.androidexperiments.shadercam.gl.CameraRenderer
import com.androidexperiments.shadercam.utils.ShaderUtils

import java.io.File
import java.util.Arrays

import butterknife.ButterKnife
import butterknife.InjectView
import butterknife.OnClick

/**
 * Written by Anthony Tripaldi
 *
 * Very basic implemention of shader camera.
 */
open class SimpleShaderActivity : FragmentActivity(), CameraRenderer.OnRendererReadyListener, PermissionsHelper.PermissionsListener {

    /**
     * We inject our views from our layout xml here using [ButterKnife]
     * it didn't work so I
     */
    @InjectView(R.id.texture_view)
    internal var mTextureView: TextureView? = null
    @InjectView(R.id.btn_record)
    internal var mRecordBtn: Button? = null

    /**
     * Custom fragment used for encapsulating all the [android.hardware.camera2] apis.
     */
    private var mCameraFragment: CameraFragment? = null

    /**
     * Our custom renderer for this example, which extends [CameraRenderer] and then adds custom
     * shaders, which turns shit green, which is easy.
     */
    private var mRenderer: CameraRenderer? = null

    /**
     * boolean for triggering restart of camera after completed rendering
     */
    private var mRestartCamera = false

    private var mPermissionsHelper: PermissionsHelper? = null
    private var mPermissionsSatisfied = false

    private val videoFile: File
        get() = File(Environment.getExternalStorageDirectory(), TEST_VIDEO_FILE_NAME)


    /**
     * [android.view.TextureView.SurfaceTextureListener] responsible for setting up the rest of the
     * rendering and recording elements once our TextureView is good to go.
     */
    private val mTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            //convenience method since we're calling it from two places
            setReady(surface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            mCameraFragment!!.configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ButterKnife.inject(this)
        mTextureView = findViewById(R.id.texture_view) as TextureView
        mRecordBtn = findViewById(R.id.btn_record) as Button
        setupCameraFragment()
        setupInteraction()

        //setup permissions for M or start normally
        if (PermissionsHelper.isMorHigher())
            setupPermissions()
    }

    private fun setupPermissions() {
        mPermissionsHelper = PermissionsHelper.attach(this)
        mPermissionsHelper!!.setRequestedPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

        )
    }

    /**
     * create the camera fragment responsible for handling camera state and add it to our activity
     */
    private fun setupCameraFragment() {
        if (mCameraFragment != null && mCameraFragment!!.isAdded)
            return

        mCameraFragment = CameraFragment.getInstance()
        mCameraFragment!!.setCameraToUse(CameraFragment.CAMERA_PRIMARY) //pick which camera u want to use, we default to forward
        mCameraFragment!!.setTextureView(mTextureView)

        //add fragment to our setup and let it work its magic
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(mCameraFragment, TAG_CAMERA_FRAGMENT)
        transaction.commit()
    }

    /**
     * add a listener for touch on our surface view that will pass raw values to our renderer for
     * use in our shader to control color channels.
     */
    private fun setupInteraction() {
        mTextureView!!.setOnTouchListener(View.OnTouchListener { v, event ->
            if (mRenderer is ExampleRenderer) {
                (mRenderer as ExampleRenderer).setTouchPoint(event.rawX, event.rawY)
                return@OnTouchListener true
            }
            false
        })
    }

    /**
     * Things are good to go and we can continue on as normal. If this is called after a user
     * sees a dialog, then onResume will be called next, allowing the app to continue as normal.
     */
    override fun onPermissionsSatisfied() {
        Log.d(TAG, "onPermissionsSatisfied()")
        mPermissionsSatisfied = true
    }

    /**
     * User did not grant the permissions needed for out app, so we show a quick toast and kill the
     * activity before it can continue onward.
     * @param failedPermissions string array of which permissions were denied
     */
    override fun onPermissionsFailed(failedPermissions: Array<String>) {
        Log.e(TAG, "onPermissionsFailed()" + Arrays.toString(failedPermissions))
        mPermissionsSatisfied = false
        Toast.makeText(this, "shadercam needs all permissions to function, please try again.", Toast.LENGTH_LONG).show()
        this.finish()
    }

    override fun onResume() {
        super.onResume()

        Log.d(TAG, "onResume()")

        ShaderUtils.goFullscreen(this.window)

        /**
         * if we're on M and not satisfied, check for permissions needed
         * [PermissionsHelper.checkPermissions] will also instantly return true if we've
         * checked prior and we have all the correct permissions, allowing us to continue, but if its
         * false, we want to `return` here so that the popup will trigger without [.setReady]
         * being called prematurely
         */
        //
        if (PermissionsHelper.isMorHigher() && !mPermissionsSatisfied) {
            if (!mPermissionsHelper!!.checkPermissions())
                return
            else
                mPermissionsSatisfied = true //extra helper as callback sometimes isnt quick enough for future results
        }

        if (!mTextureView!!.isAvailable)
            mTextureView!!.surfaceTextureListener = mTextureListener //set listener to handle when its ready
        else
            setReady(mTextureView!!.surfaceTexture, mTextureView!!.width, mTextureView!!.height)
    }

    override fun onPause() {
        super.onPause()

        shutdownCamera(false)
        mTextureView!!.surfaceTextureListener = null
    }

    /**
     * [ButterKnife] uses annotations to make setting [android.view.View.OnClickListener]'s
     * easier than ever with the [OnClick] annotation.
     */
    @OnClick(R.id.btn_record)
    fun onClickRecord() {
        if (mRenderer!!.isRecording)
            stopRecording()

        else
            startRecording()
    }

    @OnClick(R.id.btn_swap_camera)
    fun onClickSwapCamera() {
        mCameraFragment!!.swapCamera()
    }

    /**
     * called whenever surface texture becomes initially available or whenever a camera restarts after
     * completed recording or resuming from onpause
     * @param surface [SurfaceTexture] that we'll be drawing into
     * @param width width of the surface texture
     * @param height height of the surface texture
     */
    protected fun setReady(surface: SurfaceTexture, width: Int, height: Int) {
        mRenderer = getRenderer(surface, width, height)
        mRenderer!!.setCameraFragment(mCameraFragment)
        mRenderer!!.setOnRendererReadyListener(this)
        mRenderer!!.start()

        //initial config if needed
        mCameraFragment!!.configureTransform(width, height)
    }

    /**
     * Override this method for easy usage of stock example setup, allowing for easy
     * recording with any shader.
     */
    protected open fun getRenderer(surface: SurfaceTexture, width: Int, height: Int): CameraRenderer {
        return ExampleRenderer(this, surface, width, height)
    }

    private fun startRecording() {
        mRenderer!!.startRecording(videoFile)
        mRecordBtn!!.text = "Stop"
    }

    private fun stopRecording() {
        mRenderer!!.stopRecording()
        mRecordBtn!!.text = "Record"

        //restart so surface is recreated
        shutdownCamera(true)

        Toast.makeText(this, "File recording complete: " + videoFile.absolutePath, Toast.LENGTH_LONG).show()
    }

    /**
     * kills the camera in camera fragment and shutsdown render thread
     * @param restart whether or not to restart the camera after shutdown is complete
     */
    private fun shutdownCamera(restart: Boolean) {
        //make sure we're here in a working state with proper permissions when we kill the camera
        if (PermissionsHelper.isMorHigher() && !mPermissionsSatisfied) return

        //check to make sure we've even created the cam and renderer yet
        if (mCameraFragment == null || mRenderer == null) return

        mCameraFragment!!.closeCamera()

        mRestartCamera = restart
        mRenderer!!.renderHandler.sendShutdown()
        mRenderer = null
    }

    /**
     * Interface overrides from our [com.androidexperiments.shadercam.gl.CameraRenderer.OnRendererReadyListener]
     * interface. Since these are being called from inside the CameraRenderer thread, we need to make sure
     * that we call our methods from the [.runOnUiThread] method, so that we don't
     * throw any exceptions about touching the UI from non-UI threads.
     *
     * Another way to handle this would be to create a Handler/Message system similar to how our
     * [com.androidexperiments.shadercam.gl.CameraRenderer.RenderHandler] works.
     */
    override fun onRendererReady() {
        runOnUiThread {
            mCameraFragment!!.setPreviewTexture(mRenderer!!.previewTexture)
            mCameraFragment!!.openCamera()
        }
    }

    override fun onRendererFinished() {
        runOnUiThread {
            if (mRestartCamera) {
                setReady(mTextureView!!.surfaceTexture, mTextureView!!.width, mTextureView!!.height)
                mRestartCamera = false
            }
        }
    }

    companion object {
        private val TAG = SimpleShaderActivity::class.java.simpleName
        private val TAG_CAMERA_FRAGMENT = "tag_camera_frag"

        /**
         * filename for our test video output
         */
        private val TEST_VIDEO_FILE_NAME = "test_video.mp4"
    }

}
