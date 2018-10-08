package com.pinnacleimagingsystems.ambientviewer.main

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.pinnacleimagingsystems.ambientviewer.*
import com.pinnacleimagingsystems.ambientviewer.share.ShareDataPointProvider
import com.pinnacleimagingsystems.ambientviewer.storage.DataStorage
import com.pinnacleimagingsystems.ambientviewer.viewer.ViewerActivity
import java.io.File
import java.text.DateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val PICK_IMAGE_CODE = 123
        const val SEND_DATA_FILE_CODE = 124
        const val REQUEST_PERMISSION = 125
        const val SELECT_MULIPLE_IMAGE_CODE = 126
    }

    private val views by lazy {
        object {
            val loadImageButton: View = findViewById(R.id.load_image)
            val loadMultipleButton: View = findViewById(R.id.load_multiple_images)
            val event: TextView = findViewById(R.id.event)
            val loadLastButton: View = findViewById(R.id.load_last)
            val lastContainer: View = findViewById(R.id.last_file_container)
            val lastFileName: TextView = findViewById(R.id.last_file_name)
            val lastFilePreview: ImageView = findViewById(R.id.last_file_preview)
            val version: TextView = findViewById(R.id.version)
            val sendFile: Button = findViewById(R.id.send_file)
            val dataPointsText: TextView = findViewById(R.id.data_points_text)
        }
    }

    lateinit var presenter: MainPresenter

    lateinit var dataStorage: DataStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_main)

        presenter = ViewModelProviders.of(this)[MainPresenterImpl::class.java].apply {
            init(application)
        }
        dataStorage = Deps.dataStorage

        views.apply {
            loadImageButton.setOnClickListener { _ -> onLoadButtonClicked() }
            loadMultipleButton.setOnClickListener { _ -> onLoadMultipleButtonClicked() }
            loadLastButton.setOnClickListener { _ -> onLoadLastClicked() }
            lastContainer.setOnClickListener { _ -> onLoadLastClicked() }
            sendFile.setOnClickListener { _ -> onSendFileClicked() }
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

    override fun onResume() {
        super.onResume()
        updateDataPoints()
    }

    private fun onEvent(event: MainPresenter.State.Event) {
        when(event) {
            is MainPresenter.State.Event.ViewFile -> {
                val files = arrayOf(event.file)
                val intent = Intent(this, ViewerActivity::class.java).apply {
                    putExtra(ViewerActivity.PARAM_FILES, files)
                }
                startActivity(intent)
            }
            is MainPresenter.State.Event.ViewFiles -> {
                val files = event.uris.map { it.toString() }.toTypedArray()
                val intent = Intent(this, ViewerActivity::class.java).apply {
                    putExtra(ViewerActivity.PARAM_FILES, files)
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

    private fun onLoadMultipleButtonClicked() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        if (intent.resolveActivity(packageManager) == null) {
            return
        }

        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_images)), SELECT_MULIPLE_IMAGE_CODE)
    }

    private fun onLoadLastClicked() {
        presenter.onLastFileClicked()
    }

    private fun onLastFileChanged(lastFile: File) {
        views.loadLastButton.isEnabled = true
        views.lastContainer.visibility = View.VISIBLE
        views.lastFileName.text = Uri.fromFile(lastFile).toDisplayName(contentResolver)
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
        val intentData = data?.data

        when(requestCode) {
            PICK_IMAGE_CODE -> {
                if (resultCode == RESULT_OK && intentData != null) {
                    presenter.onFileSelected(intentData)
                }
            }
            SELECT_MULIPLE_IMAGE_CODE -> {
                val uris = getSelectedImages(data)
                if (resultCode == Activity.RESULT_OK && uris.isNotEmpty()) {
                    presenter.onMultipleFilesSelected(uris)
                }
            }
        }
    }

    private fun getSelectedImages(intent: Intent?): List<Uri> {
        val result = mutableListOf<Uri>()
        if (intent == null) {
            return result
        }

        val intentData = intent.data
        if (intentData != null) {
            // single image
            return listOf(intentData)
        }

        val clipData = intent.clipData
        if (clipData != null && clipData.itemCount > 0) {
            for (i in 0 until clipData.itemCount) {
                val item = clipData.getItemAt(i) ?: continue
                val uri = item.uri ?: continue

                result.add(uri)
            }
        }

        return result
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            REQUEST_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onSendFileClicked()
                }
            }
        }
    }

    private fun onSendFileClicked() {
        when (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)) {
            PackageManager.PERMISSION_GRANTED -> {}
            PackageManager.PERMISSION_DENIED -> {
                requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION)
                return
            }
        }

        ShareDataPointProvider.prepareSharedFile { uri ->
            sendAsAttachment(uri!!)
        }
    }

    private fun sendAsAttachment(uri: Uri) {
        val date = DateFormat.getDateInstance().format(Date())
        val body = getString(R.string.send_file_text, date)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, body)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(intent, getString(R.string.send_title))
        startActivityForResult(chooserIntent, SEND_DATA_FILE_CODE)
    }

    private fun updateDataPoints() {
        dataStorage.getAllDataPoints { values ->
            val count = values.size
            views.dataPointsText.text = getString(R.string.data_points, count)
        }
    }
}
