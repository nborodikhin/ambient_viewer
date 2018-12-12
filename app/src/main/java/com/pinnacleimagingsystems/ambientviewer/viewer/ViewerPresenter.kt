package com.pinnacleimagingsystems.ambientviewer.viewer

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import android.net.Uri
import android.support.annotation.AnyThread
import android.support.annotation.WorkerThread
import android.support.media.ExifInterface
import android.util.DisplayMetrics
import android.view.WindowManager
import com.pinnacleimagingsystems.ambientviewer.ConsumableEvent
import com.pinnacleimagingsystems.ambientviewer.Deps
import com.pinnacleimagingsystems.ambientviewer.als.LightSensor
import com.pinnacleimagingsystems.ambientviewer.loadBitmap
import com.pinnacleimagingsystems.ambientviewer.tasks.CopyTask
import com.pinnacleimagingsystems.ambientviewer.toDisplayName
import java.io.File

abstract class ViewerPresenter: ViewModel() {
    enum class State {
        UNINITIALIZED,
        LOADING,
        DISPLAYING,
        PROCESSING,
    }

    enum class ImageType {
        ORIGINAL,
        WORKING
    }

    data class Image(
            val type: ImageType,
            val bitmap: Bitmap,
            val parameters: AlgorithmParameters? = null
    )

    sealed class Event {
        object NonSrgbWarning: Event()
        object DataPointSaved: Event()
        data class UnsupportedFileType(val mimetype: String): Event()
        data class ReadError(val exception: Exception): Event()

        fun asConsumable(): ConsumableEvent<Event> = ConsumableEvent(this)
    }

    class ViewerState {
        val displayName by lazy { MutableLiveData<String>() }
        val state by lazy { MutableLiveData<State>().apply { value = State.UNINITIALIZED } }

        val curParameter by lazy { MutableLiveData<Int>() }
        val originalImage by lazy { MutableLiveData<Image>() }
        var filePath: String? = null
        var workingImage: Image? = null

        val displayingImage by lazy { MutableLiveData<Image>() }

        val event by lazy { MutableLiveData<ConsumableEvent<Event>>() }
    }

    val state = ViewerState()

    abstract fun startFlow(): Boolean
    abstract fun loadFile(file: String)
    abstract fun onSetParameter(parameter: Int)
    abstract fun onImageClicked()
    abstract fun onSaveButtonClicked(image: Image, viewingLux: Int)
}

class ViewerPresenterImpl: ViewerPresenter() {
    private val contentResolver = Deps.contentResolver
    private val bgExecutor = Deps.bgExecutor
    private val mainExecutor = Deps.mainExecutor
    private val algorithm = Deps.algorithm
    private val dataStorage = Deps.dataStorage

    private lateinit var lightSensor: LightSensor
    private var screenMaxSize: Int = 0

    private lateinit var workingBitmap: Bitmap

    fun init(lightSensor: LightSensor, windowsManager: WindowManager) {
        this.lightSensor = lightSensor

        val displayMetrics = DisplayMetrics()
        windowsManager.defaultDisplay.getMetrics(displayMetrics)
        screenMaxSize = maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    override fun startFlow(): Boolean {
        if (state.curParameter.value == null) {
            val lux = lightSensor.value.value?.toInt() ?: 0
            state.curParameter.value = algorithm.meta.defaultParameter(lux)
            return true
        } else {
            return false
        }
    }

    override fun loadFile(file: String) {
        state.filePath = file

        if (state.state.value!! != State.UNINITIALIZED) {
            return
        }

        var fileName = file

        state.state.value = State.LOADING

        val uri = Uri.parse(fileName)
        val displayName = uri.toDisplayName(contentResolver)
        state.displayName.value = displayName

        val copy = CopyTask(Deps.applicationContext)

        bgExecutor.execute {
            val temporary: Boolean
            val copyResult = if (uri.scheme == "file" || fileName.startsWith('/')) {
                temporary = false
                val contentResolver = Deps.applicationContext.contentResolver
                val mimeType = contentResolver.getType(uri)

                CopyTask.CopyResult.Success(mimeType ?: "image/jpeg", File(fileName))
            } else {
                temporary = true
                copy.copyFile(uri, displayName)
            }

            when (copyResult) {
                is CopyTask.CopyResult.UnsupportedType -> {
                    state.event.postValue(Event.UnsupportedFileType(copyResult.mimeType).asConsumable())
                    return@execute
                }
                is CopyTask.CopyResult.Failure -> {
                    state.event.postValue(Event.ReadError(copyResult.exception).asConsumable())
                    return@execute
                }
                is CopyTask.CopyResult.Success -> {
                    fileName = copyResult.file.absolutePath
                }
            }

            val bitmap: Bitmap
            val exif: ExifInterface

            try {
                bitmap = loadBitmap(fileName, screenMaxSize)
                exif = ExifInterface(fileName)
            } finally {
                if (temporary) {
                    File(fileName).delete()
                }
            }

            val colorSpaceInt = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, ExifInterface.COLOR_SPACE_UNCALIBRATED)
            if (colorSpaceInt != ExifInterface.COLOR_SPACE_S_RGB) {
                state.event.postValue(Event.NonSrgbWarning.asConsumable())
            }

            workingBitmap = bitmap.copy(bitmap.config, true)

            val originalImage = Image(ImageType.ORIGINAL, bitmap)
            state.originalImage.postValue(originalImage)

            mainExecutor.execute {
                processImage(state.curParameter.value!!)
            }

            state.displayingImage.postValue(originalImage)
        }
    }

    override fun onImageClicked() {
        switchDisplayingImages()
    }

    override fun onSetParameter(parameter: Int) {
        state.state.value = State.PROCESSING

        processImage(parameter)
    }

    override fun onSaveButtonClicked(image: Image, viewingLux: Int) {
        val parameters = image.parameters ?: return

        dataStorage.saveDataPoint(
                createDataPoint(state.displayName.value!!, parameters, viewingLux)
        )

        state.event.postValue(Event.DataPointSaved.asConsumable())
    }

    private fun processImage(parameter: Int) {
        state.curParameter.postValue(parameter)

        bgExecutor.execute {
            val originalBitmap = state.originalImage.value!!.bitmap

            val parameters = AlgorithmParameters(
                    parameter
            )
            algorithm.init(parameter)
            updateBitmap(originalBitmap, workingBitmap)

            val image = Image(ImageType.WORKING, workingBitmap, parameters)
            state.workingImage = image
            setDisplayingImage(ViewerPresenter.ImageType.WORKING)

            state.state.apply {
                postValue(State.DISPLAYING)
            }
        }
    }

    @AnyThread
    fun switchDisplayingImages() {
        val currentImage = state.displayingImage.value ?: return

        when (currentImage.type) {
            ViewerPresenter.ImageType.ORIGINAL -> setDisplayingImage(ViewerPresenter.ImageType.WORKING)
            ViewerPresenter.ImageType.WORKING -> setDisplayingImage(ViewerPresenter.ImageType.ORIGINAL)
        }
    }

    @AnyThread
    fun setDisplayingImage(type: ViewerPresenter.ImageType) {
        when (type) {
            ViewerPresenter.ImageType.ORIGINAL -> state.originalImage.value?.let { image ->
                state.displayingImage.postValue(image)
            }
            ViewerPresenter.ImageType.WORKING -> state.workingImage?.let{ image ->
                state.displayingImage.postValue(image)
            }
        }
    }

    @WorkerThread
    private fun updateBitmap(origBitmap: Bitmap, newBitmap: Bitmap) {
        val width = origBitmap.width
        val height = origBitmap.height

        val pixels = IntArray(width * height)

        origBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        algorithm.apply(pixels, width, height)

        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}


