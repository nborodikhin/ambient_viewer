package com.pinnacleimagingsystems.ambientviewer.main

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
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
            val sendFile = findViewById<Button>(R.id.send_file)
            val dataPointsText = findViewById<TextView>(R.id.data_points_text)
        }
    }

    lateinit var presenter: MainPresenter

    lateinit var dataStorage: DataStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter = ViewModelProviders.of(this)[MainPresenterImpl::class.java].apply {
            init(application)
        }
        dataStorage = Deps.dataStorage

        views.apply {
            loadImageButton.setOnClickListener { _ -> onLoadButtonClicked() }
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
        }
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
