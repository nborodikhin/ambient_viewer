package com.pinnacleimagingsystems.ambientviewer.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.github.chrisbanes.photoview.PhotoView
import com.pinnacleimagingsystems.ambientviewer.R

class ViewerActivity : AppCompatActivity() {
    companion object {
        const val PARAM_FILE = "file"

        const val MAXIMUM_SCALE = 64.0f
    }

    private val views by lazy { object {
        val photoView: PhotoView = findViewById(R.id.photo_view)
        val bitmapState: TextView = findViewById(R.id.bitmap_state)
    } }

    lateinit var bitmaps: Array<Bitmap>
    lateinit var labels: Array<String>

    private var bitmapIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        views.photoView.apply {
            maximumScale = MAXIMUM_SCALE
            setOnScaleChangeListener { _, _, _ -> updateLabel() }
        }

        val file = intent.extras!!.getString(PARAM_FILE)!!

        val bitmap = BitmapFactory.decodeFile(file)
        val updatedBitmap = bitmap.copy(bitmap.config, true)

        bitmaps = arrayOf(bitmap, updatedBitmap)
        labels = arrayOf(
                getString(R.string.original),
                getString(R.string.adapted)
        )

        bitmapIndex = bitmaps.size - 1

        views.photoView.setOnClickListener {
            setNextBitmap()
        }

        updateBitmap()

        setNextBitmap()
    }

    private fun updateBitmap() {
        val (origBitmap, newBitmap) = bitmaps

        val width = origBitmap.width
        val height = origBitmap.height

        val pixels = IntArray(width)

        for (y in 0 until height) {
            origBitmap.getPixels(pixels, 0, width, 0, y, width, 1)
            newBitmap.setPixels(pixels, 0, width, 0, (height - 1) - y, width, 1)
        }
    }

    private fun setNextBitmap() {
        bitmapIndex = (bitmapIndex + 1).rem(bitmaps.size)

        val bitmap = bitmaps[bitmapIndex]

        views.photoView.replaceBitmap(bitmap)

        updateLabel()
    }

    private fun PhotoView.replaceBitmap(bitmap: Bitmap) {
        val matrix = Matrix()

        getSuppMatrix(matrix)
        setImageBitmap(bitmap)
        setSuppMatrix(matrix)
    }

    private fun updateLabel() {
        val label = labels[bitmapIndex]
        val scale = views.photoView.scale

        views.bitmapState.text = "$label, scale $scale"

    }
}
