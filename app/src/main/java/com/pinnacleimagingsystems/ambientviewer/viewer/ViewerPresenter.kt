package com.pinnacleimagingsystems.ambientviewer.viewer

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import android.support.annotation.WorkerThread
import android.support.media.ExifInterface
import com.pinnacleimagingsystems.ambientviewer.ConsumableEvent
import com.pinnacleimagingsystems.ambientviewer.Deps
import com.pinnacleimagingsystems.ambientviewer.loadBitmap

abstract class ViewerPresenter: ViewModel() {
    enum class State {
        UNINITIALIZED,
        LOADING,
        LOADED
    }

    enum class ImageType {
        ORIGINAL,
        WORKING
    }

    data class Image(val type: ImageType, val bitmap: Bitmap)

    sealed class Event {
        object NonSrgbWarning: Event()

        fun asConsumable(): ConsumableEvent<Event> = ConsumableEvent(this)
    }

    class ViewerState {
        val state by lazy { MutableLiveData<State>().apply { value = State.UNINITIALIZED } }

        val originalImage by lazy { MutableLiveData<Image>() }
        val workingImage by lazy { MutableLiveData<Image>() }

        val displayingImage by lazy { MutableLiveData<Image>() }

        val event by lazy { MutableLiveData<ConsumableEvent<Event>>() }
    }

    val state = ViewerState()

    abstract fun loadFile(file: String)
    abstract fun onImageClicked()
}

class ViewerPresenterImpl: ViewerPresenter() {
    private val bgExecutor = Deps.bgExecutor
    private val mainExecutor = Deps.mainExecutor
    private val algorithm = Deps.createAlgorithm()

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

            val originalImage = Image(ImageType.ORIGINAL, bitmap)
            state.originalImage.postValue(originalImage)

            val updatedBitmap = bitmap.copy(bitmap.config, true)

            algorithm.init(2)
            updateBitmap(bitmap, updatedBitmap)

            state.workingImage.postValue(Image(ImageType.WORKING, updatedBitmap))

            state.displayingImage.postValue(originalImage)

            state.state.postValue(State.LOADED)
        }
    }

    override fun onImageClicked() {
        val currentImage = state.displayingImage.value ?: return

        when (currentImage.type) {
            ViewerPresenter.ImageType.ORIGINAL -> state.workingImage.value?.let { image ->
                state.displayingImage.postValue(image)
            }
            ViewerPresenter.ImageType.WORKING -> state.originalImage.value?.let{ image ->
                state.displayingImage.postValue(image)
            }
        }
    }

    private fun updateBitmap(origBitmap: Bitmap, newBitmap: Bitmap) {
        val width = origBitmap.width
        val height = origBitmap.height

        val pixels = IntArray(width * height)

        origBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        algorithm.apply(pixels, width, height)

        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}


