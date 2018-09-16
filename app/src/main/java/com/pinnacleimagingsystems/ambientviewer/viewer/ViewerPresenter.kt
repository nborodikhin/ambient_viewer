package com.pinnacleimagingsystems.ambientviewer.viewer

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import android.support.annotation.AnyThread
import android.support.annotation.WorkerThread
import android.support.media.ExifInterface
import com.pinnacleimagingsystems.ambientviewer.ConsumableEvent
import com.pinnacleimagingsystems.ambientviewer.Deps
import com.pinnacleimagingsystems.ambientviewer.loadBitmap

abstract class ViewerPresenter: ViewModel() {
    companion object {
        const val PARAMETER_DEFAULT = 5
    }

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

    data class Parameters(
        val slider: Int,
        val lightSensor: Int = 0
    )

    data class Image(
            val type: ImageType,
            val bitmap: Bitmap,
            val parameters: Parameters? = null
    )

    sealed class Event {
        object NonSrgbWarning: Event()

        fun asConsumable(): ConsumableEvent<Event> = ConsumableEvent(this)
    }

    class ViewerState {
        val state by lazy { MutableLiveData<State>().apply { value = State.UNINITIALIZED } }

        val curParameter by lazy { MutableLiveData<Int>().apply { value = PARAMETER_DEFAULT } }
        val originalImage by lazy { MutableLiveData<Image>() }
        var workingImage: Image? = null

        val displayingImage by lazy { MutableLiveData<Image>() }

        val event by lazy { MutableLiveData<ConsumableEvent<Event>>() }
    }

    val state = ViewerState()

    abstract fun loadFile(file: String)
    abstract fun onSetParameter(parameter: Int)
    abstract fun onImageClicked()
}

class ViewerPresenterImpl: ViewerPresenter() {
    private val bgExecutor = Deps.bgExecutor
    private val mainExecutor = Deps.mainExecutor
    private val algorithm = Deps.createAlgorithm()
    private lateinit var workingBitmap: Bitmap

    override fun loadFile(file: String) {
        if (state.state.value!! != State.UNINITIALIZED) {
            return
        }

        state.state.value = State.LOADING

        bgExecutor.execute {
            val bitmap = loadBitmap(file)

            val exif = ExifInterface(file)
            val colorSpaceInt = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, ExifInterface.COLOR_SPACE_UNCALIBRATED)
            if (colorSpaceInt != ExifInterface.COLOR_SPACE_S_RGB) {
                state.event.postValue(Event.NonSrgbWarning.asConsumable())
            }

            workingBitmap = bitmap.copy(bitmap.config, true)

            val originalImage = Image(ImageType.ORIGINAL, bitmap)
            state.originalImage.postValue(originalImage)

            processImage(state.curParameter.value!!)

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

    fun processImage(parameter: Int) {
        state.curParameter.postValue(parameter)

        bgExecutor.execute {
            val originalBitmap = state.originalImage.value!!.bitmap

            val parameters = Parameters(
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


