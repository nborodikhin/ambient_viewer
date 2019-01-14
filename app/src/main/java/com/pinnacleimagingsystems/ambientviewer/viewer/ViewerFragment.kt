package com.pinnacleimagingsystems.ambientviewer.viewer

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.chrisbanes.photoview.PhotoView
import com.pinnacleimagingsystems.ambientviewer.Deps
import com.pinnacleimagingsystems.ambientviewer.R
import com.pinnacleimagingsystems.ambientviewer.als.LightSensor
import kotlin.math.roundToInt

class ViewerFragment: Fragment() {
    companion object {
        const val PARAM_FILE = "file"
        const val PARAM_ID = "id"
        const val PARAM_VIEWER_MODE = "viewerMode"

        private const val MAXIMUM_SCALE = 64.0f

        private const val CHECKBOX_RESET_DELAY = 1000L

        fun create(file: String, id: Int, viewerMode: Boolean = false) = ViewerFragment().apply {
            arguments = Bundle().apply {
                putString(ViewerFragment.PARAM_FILE, file)
                putInt(ViewerFragment.PARAM_ID, id)
                putBoolean(ViewerFragment.PARAM_VIEWER_MODE, viewerMode)
            }
        }
    }

    interface Host {
        fun onViewerError(file: String?)
    }

    private fun <T: View> findViewById(id: Int) = view!!.findViewById<T>(id)

    private val views by lazy { object {
        val content: View = findViewById(R.id.content)
        val contentClickOverlay: View = findViewById(R.id.content_click_overlay)
        val progressBar: View = findViewById(R.id.progressBar)

        val photoView: PhotoView = findViewById(R.id.photo_view)
        val bitmapState: TextView = findViewById(R.id.bitmap_state)
        val parameterSlider: SeekBar = findViewById(R.id.parameter_slider)
        val saveCheckbox: CheckBox = findViewById(R.id.save_checkbox)
        val toggle: Switch = findViewById(R.id.adapted_toggle)
        val manualModeToggle: Switch = findViewById(R.id.manual_mode_switch)
        val automaticModeText: TextView = findViewById(R.id.automatic_mode_text)
    } }

    private val host get() = activity!! as Host

    var fileId: Int = -1
        private set

    private lateinit var presenter: ViewerPresenter

    private lateinit var lightSensor: LightSensor

    private val shortAnimTime: Long
        get() = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

    private val longAnimTime: Long
            get() = resources.getInteger(android.R.integer.config_longAnimTime).toLong()

    private val inViewerMode
            get() = arguments?.getBoolean(PARAM_VIEWER_MODE) ?: false

    private val inTesterMode
        get() = !inViewerMode

    private val manualModeEnabled
        get() = inTesterMode || views.manualModeToggle.isChecked

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_viewer, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lightSensor = (context!!.applicationContext as LightSensor.Holder).getLightSensor()

        fileId = arguments!!.getInt(PARAM_ID)

        presenter = ViewModelProviders.of(this)[ViewerPresenterImpl::class.java].apply {
            init(lightSensor, activity!!.windowManager, enableContinuousUpdate = inViewerMode)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lifecycleOwner: LifecycleOwner = this

        with(views) {
            photoView.apply {
                maximumScale = MAXIMUM_SCALE
                setOnScaleChangeListener { _, _, _ -> updateLabel() }
                setOnClickListener { _ -> presenter.onImageClicked() }
            }
            toggle.setOnClickListener { presenter.onImageClicked() }
            contentClickOverlay.setOnClickListener { }
            saveCheckbox.setOnClickListener { _ -> onSaveClicked() }
            manualModeToggle.setOnCheckedChangeListener { _, isChecked ->
                onManualModeUpdated(manualMode = isChecked)
            }

            if (inViewerMode) {
                saveCheckbox.visibility = View.GONE
                manualModeToggle.visibility = View.VISIBLE
            } else {
                saveCheckbox.visibility = View.VISIBLE
                manualModeToggle.visibility = View.GONE
            }
        }

        onManualModeUpdated(manualMode = manualModeEnabled, activeNotification = false)

        with(presenter.state) {
            state.observe(lifecycleOwner, Observer { state -> onStateChanged(state!!) })
            event.observe(lifecycleOwner, Observer { event -> event!!.consume(this@ViewerFragment::onEvent) })
            displayingImage.observe(lifecycleOwner, Observer { image -> onDisplayingImageChanged(image!!) })
        }

        lightSensor.value.observe(lifecycleOwner, Observer { _ -> onLightSensorChange() })
    }

    var initialized: Boolean = false

    fun onVisible() {
        if (!initialized) {
            initialized = true
            startFlow()
        }
    }

    private fun startFlow() {
        val started = presenter.startFlow()
        views.parameterSlider.init()
        views.parameterSlider.setParameter(presenter.state.curParameter.value!!)
        if (started) {
            processIntent()
        }
    }

    private fun postDelayed(delayMillis: Long, block: () -> Unit) {
        views.content.postDelayed(block, delayMillis)
    }

    var updatingProgress = false

    private fun SeekBar.init() {
        val (min, range) = with (Deps.algorithm.meta) {
            parameterMin() to parameterMax() - parameterMin()
        }
        max = 100

        fun Int.asParameter() = min + (toFloat() / max) * range

        setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!updatingProgress) {
                    presenter.onSetParameter(progress.asParameter(), manualInput = true)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun SeekBar.setParameter(parameter: Float) {
        updatingProgress = true
        val (min, range) = with (Deps.algorithm.meta) {
            parameterMin() to parameterMax() - parameterMin()
        }
        val max = 100
        val sliderPos = max * (parameter - min) / range
        progress = sliderPos.toInt()
        updatingProgress = false
    }

    private fun processIntent() {
        val file = arguments!!.getString(PARAM_FILE)!!
        presenter.loadFile(file)
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

    private fun onEvent(event: ViewerPresenter.Event) {
        @Suppress("UNUSED_VARIABLE")
        val dummy = when (event) {
            is ViewerPresenter.Event.NonSrgbWarning -> {
                Toast.makeText(activity, "Unsupported colorspace (non-sRGB)", Toast.LENGTH_SHORT).show()
            }
            is ViewerPresenter.Event.DataPointSaved -> {
                Toast.makeText(activity, "Saved!", Toast.LENGTH_SHORT).show()
                with (views.saveCheckbox) {
                    isClickable = false
                    postDelayed(CHECKBOX_RESET_DELAY) {
                        isChecked = false
                        isClickable = true
                    }
                }
            }
            is ViewerPresenter.Event.UnsupportedFileType -> {
                Toast.makeText(activity, "Unsupported file type: ${event.mimetype}", Toast.LENGTH_SHORT).show()
            }
            is ViewerPresenter.Event.ReadError -> {
                Toast.makeText(activity, "Error reading file: ${event.exception.message}", Toast.LENGTH_SHORT).show()
                host.onViewerError(presenter.state.filePath)
            }
            is ViewerPresenter.Event.LightSensorParameterComputed -> {
                onLightSensorParameterComputed(event.parameter)
            }
        }
    }

    private fun onDisplayingImageChanged(image: ViewerPresenter.Image) {
        views.photoView.replaceBitmap(image.bitmap)
        when (image.type) {
            ViewerPresenter.ImageType.ORIGINAL -> views.toggle.isChecked = false
            ViewerPresenter.ImageType.WORKING -> views.toggle.isChecked = true
        }
        updateLabel()
    }

    private fun onLightSensorChange() {
        updateLabel()
        presenter.onLightSensorChanged()
    }

    private fun onLightSensorParameterComputed(parameter: Float) {
        if (manualModeEnabled) {
            return
        }
        presenter.onSetParameter(parameter, manualInput = false)
        views.parameterSlider.setParameter(parameter)
    }

    private fun PhotoView.replaceBitmap(bitmap: Bitmap) {
        val matrix = Matrix()

        getSuppMatrix(matrix)
        setImageBitmap(bitmap)
        setSuppMatrix(matrix)
    }

    private fun updateLabel() {
        val image = presenter.state.displayingImage.value ?: return

        val scale = views.photoView.scale

        val label = when (image.type) {
            ViewerPresenter.ImageType.ORIGINAL ->
                String.format(
                        getString(R.string.original),
                        scale,
                        lightSensor.value.value!!.roundToInt()
                )
            ViewerPresenter.ImageType.WORKING ->
                String.format(
                        getString(R.string.adapted),
                        scale,
                        lightSensor.value.value!!.roundToInt(),
                        image.parameters?.parameter
                )
        }


        views.bitmapState.text = label
    }

    private fun onSaveClicked() {
        val image = presenter.state.displayingImage.value ?: return
        val viewingLux = lightSensor.value.value?.roundToInt() ?: -1

        presenter.onSaveButtonClicked(image, viewingLux)
    }

    private fun onManualModeUpdated(manualMode: Boolean, activeNotification: Boolean = true) {
        views.automaticModeText.visibility = if (manualMode) View.GONE else View.VISIBLE
        views.parameterSlider.visibility = if (manualMode) View.VISIBLE else View.GONE

        if (manualMode && activeNotification) {
            Toast.makeText(context, R.string.manual_mode_toast, Toast.LENGTH_SHORT).show()
        }
    }
}