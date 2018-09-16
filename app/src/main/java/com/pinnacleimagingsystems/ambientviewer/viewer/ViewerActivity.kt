package com.pinnacleimagingsystems.ambientviewer.viewer

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.github.chrisbanes.photoview.PhotoView
import com.pinnacleimagingsystems.ambientviewer.R

class ViewerActivity : AppCompatActivity() {
    companion object {
        const val PARAM_FILE = "file"

        private const val MAXIMUM_SCALE = 64.0f
    }

    fun ignoreClick(view: View) {}

    private val views by lazy { object {
        val content: View = findViewById(R.id.content)
        val contentClickOverlay: View = findViewById(R.id.content_click_overlay)
        val progressBar: View = findViewById(R.id.progressBar)

        val photoView: PhotoView = findViewById(R.id.photo_view)
        val bitmapState: TextView = findViewById(R.id.bitmap_state)
        val parameterSlider: SeekBar = findViewById(R.id.parameter_slider)
    } }

    private lateinit var presenter: ViewerPresenter

    private val shortAnimTime: Long
        get() = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

    private val longAnimTime: Long
            get() = resources.getInteger(android.R.integer.config_longAnimTime).toLong()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        presenter = ViewModelProviders.of(this)[ViewerPresenterImpl::class.java]

        setupFullscreen()

        views.photoView.apply {
            maximumScale = MAXIMUM_SCALE
            setOnScaleChangeListener { _, _, _ -> updateLabel() }
            setOnClickListener { _ -> presenter.onImageClicked() }
        }
        views.contentClickOverlay.setOnClickListener { view -> ignoreClick(view) }

        presenter.state.state.observe(this, Observer { state -> onStateChanged(state!!) })
        presenter.state.event.observe(this, Observer { event -> event!!.consume(this::onEvent) })
        presenter.state.displayingImage.observe(this, Observer { image -> onDisplayingImageChanged(image!!) } )
        setSlider(presenter.state.curParameter.value!!)


        views.content.postDelayed(this::processIntent, 200L)
    }

    private fun setSlider(parameter: Int) = with (views.parameterSlider) {
        setOnSeekBarChangeListener(null)

        setProgress(parameter, false)

        setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                presenter.onSetParameter(progress)
            }
        })
    }

    private fun processIntent() {
        val file = intent.extras!!.getString(PARAM_FILE)!!
        presenter.loadFile(file)
    }

    private fun setupFullscreen() {
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                makeUiFullscreen()
            }
        }

        makeUiFullscreen()
    }

    var currentState: ViewerPresenter.State? = null

    private fun onStateChanged(state: ViewerPresenter.State) {
        when (state) {
            ViewerPresenter.State.UNINITIALIZED -> {
                views.content.visibility = View.GONE
                views.contentClickOverlay.visibility = View.GONE
                views.progressBar.visibility = View.GONE
            }
            ViewerPresenter.State.LOADING -> {
                views.content.visibility = View.GONE
                views.contentClickOverlay.visibility = View.GONE
                views.progressBar.animateFadeIn(duration = shortAnimTime)
            }
            ViewerPresenter.State.PROCESSING -> {
                views.content.visibility = View.VISIBLE
                views.contentClickOverlay.visibility = View.VISIBLE
                views.progressBar.animateFadeIn(duration = shortAnimTime)
            }
            ViewerPresenter.State.DISPLAYING -> {
                views.contentClickOverlay.visibility = View.GONE
                views.content.animateFadeIn(from = views.content.alpha, duration = longAnimTime)
                views.progressBar.animateFadeOut(duration = longAnimTime)
            }
        }
        currentState = state
    }

    private fun View.animateFadeIn(from: Float = 0.0f, to: Float = 1.0f, duration: Long) {
        animate().cancel()

        alpha = from
        visibility = View.VISIBLE
        animate()
                .alpha(to)
                .setDuration(duration)
                .start()
    }

    private fun View.animateFadeOut(to: Float = 0.0f, duration: Long) {
        animate().cancel()

        animate()
                .alpha(to)
                .setDuration(duration)
                .withEndAction {
                    visibility = View.GONE
                }
                .start()
    }

    private fun onEvent(event: ViewerPresenter.Event) = when (event) {
        ViewerPresenter.Event.NonSrgbWarning -> {
            Toast.makeText(this, "Unsupported colorspace (non-sRGB)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onDisplayingImageChanged(image: ViewerPresenter.Image) {
        views.photoView.replaceBitmap(image.bitmap)
        updateLabel()
    }

    private fun makeUiFullscreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
    }

    private fun PhotoView.replaceBitmap(bitmap: Bitmap) {
        val matrix = Matrix()

        getSuppMatrix(matrix)
        setImageBitmap(bitmap)
        setSuppMatrix(matrix)
    }

    private fun updateLabel() {
        val image = presenter.state.displayingImage.value ?: return

        val scale = views.photoView.scale.toString()

        val label = when (image.type) {
            ViewerPresenter.ImageType.ORIGINAL ->
                String.format(
                        getString(R.string.original),
                        scale
                )
            ViewerPresenter.ImageType.WORKING ->
                String.format(
                        getString(R.string.adapted),
                        scale,
                        image.parameters?.slider
                )
        }


        views.bitmapState.text = label
    }
}
