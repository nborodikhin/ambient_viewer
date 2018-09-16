package com.pinnacleimagingsystems.ambientviewer.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.pinnacleimagingsystems.ambientviewer.BuildConfig
import com.pinnacleimagingsystems.ambientviewer.R
import com.pinnacleimagingsystems.ambientviewer.loadThumbnailBitmap
import com.pinnacleimagingsystems.ambientviewer.viewer.ViewerActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val PICK_IMAGE_CODE = 123
    }

    private val views by lazy {
        object {
            val loadImageButton = findViewById<View>(R.id.load_image)
            val event = findViewById<TextView>(R.id.event)
            val loadLastButton = findViewById<View>(R.id.load_last)
            val lastContainer = findViewById<View>(R.id.last_file_container)
            val lastFileName = findViewById<TextView>(R.id.last_file_name)
            val lastFilePreview = findViewById<ImageView>(R.id.last_file_preview)
            val version = findViewById<TextView>(R.id.version)
        }
    }

    lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter = ViewModelProviders.of(this)[MainPresenterImpl::class.java].apply {
            init(application)
        }

        views.apply {
            loadImageButton.setOnClickListener { _ -> onLoadButtonClicked() }
            loadLastButton.setOnClickListener { _ -> onLoadLastClicked() }
            lastFileName.setOnClickListener { _ -> onLoadLastClicked() }
            version.text = getString(R.string.version, BuildConfig.VERSION_NAME)
        }
    }

    override fun onStart() {
        super.onStart()

        presenter.state.eventDescription.observe(this, Observer { text ->
            views.event.text = text
        })

        presenter.state.event.observe(this, Observer { event -> event!!.consume(::onEvent) })
        presenter.state.lastFile.observe(this, Observer { lastFile -> onLastFileChanged(lastFile!!) })
    }

    private fun onEvent(event: MainPresenter.State.Event) {
        when(event) {
            is MainPresenter.State.Event.ViewFile -> {
                val intent = Intent(this, ViewerActivity::class.java).apply {
                    putExtra(ViewerActivity.PARAM_FILE, event.file)
                }
                startActivity(intent)
            }
        }
    }

    private fun onLoadButtonClicked() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        if (intent.resolveActivity(packageManager) == null) {
            return
        }

        startActivityForResult(intent, PICK_IMAGE_CODE)
    }

    private fun onLoadLastClicked() {
        presenter.onLastFileClicked()
    }

    private fun onLastFileChanged(lastFile: File) {
        views.loadLastButton.isEnabled = true
        views.lastContainer.visibility = View.VISIBLE
        views.lastFileName.text = lastFile.toString()
        views.lastFilePreview.setPreviewFromFile(lastFile)
    }

    private fun ImageView.setPreviewFromFile(file: File) {
        val thumbnailBitmap = loadThumbnailBitmap(file.absolutePath)
        if (thumbnailBitmap != null) {
            setImageBitmap(thumbnailBitmap)
        } else {
            setImageURI(Uri.fromFile(file))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_CODE && data != null && data.data != null) {
            presenter.onFileSelected(data.data!!)
        }
    }
}
