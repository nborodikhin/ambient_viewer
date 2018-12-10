package com.pinnacleimagingsystems.ambientviewer.viewer

import android.arch.lifecycle.*
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.pinnacleimagingsystems.ambientviewer.ConsumableEvent
import com.pinnacleimagingsystems.ambientviewer.R

class ViewerActivity : AppCompatActivity(), ViewerFragment.Host {
    companion object {
        const val PARAM_FILES = "files"

        private const val FRAGMENT_TAG = "viewerFragment"
    }

    sealed class Command {
        data class OpenViewer(val file: String): Command()
        object CloseActivity: Command()

        fun asConsumable() = ConsumableEvent(this)
    }

    interface Presenter: LifecycleObserver {
        val latestCommand: LiveData<ConsumableEvent<Command>>
        val currentFile: LiveData<String>
        val files: Array<String>

        fun hasPrev(): Boolean
        fun hasNext(): Boolean

        fun viewNext()
        fun viewPrev()

        fun onViewerError(filename: String)
    }

    class PresenterImpl: Presenter, ViewModel() {
        val uninitializedFiles = arrayOf<String>()
        override var files = uninitializedFiles
        private val banned = mutableSetOf<String>()

        private var curIndex = 0

        override val latestCommand = MutableLiveData<ConsumableEvent<Command>>()
        override val currentFile = MutableLiveData<String>()

        fun init(files: Array<String>?) {
            @Suppress("SuspiciousEqualsCombination")
            if (this.files !== uninitializedFiles) {
                return
            }

            this.files = files ?: uninitializedFiles
            this.curIndex = -1

            if (hasNext()) {
                viewNext()
            } else {
                exit()
            }
        }

        private fun exit() {
            post(Command.CloseActivity)
        }

        private fun openViewer() {
            post(Command.OpenViewer(files[curIndex]))
            currentFile.postValue(files[curIndex])
        }

        override fun hasPrev(): Boolean {
            return prevIndex() != null
        }

        private fun prevIndex(): Int? {
            var index = curIndex - 1
            while (index >= 0 && files[index] in banned) {
                index--
            }

            return if (index >= 0) index else null
        }

        override fun hasNext(): Boolean {
            return nextIndex() != null
        }

        private fun nextIndex(): Int? {
            var index = curIndex + 1

            while(index < files.size && files[index] in banned) {
                index++
            }

            return if (index < files.size) index else null
        }

        override fun onViewerError(filename: String) {
            ban(filename)
            if (hasNext()) {
                viewNext()
            } else if (hasPrev()) {
                viewPrev()
            } else {
                exit()
            }
        }

        private fun ban(file: String) {
            banned.add(file)
        }

        override fun viewNext() {
            val index = nextIndex()
            if (index != null) {
                curIndex = index
                openViewer()
            } else {
                exit()
            }
        }

        override fun viewPrev() {
            val index = prevIndex()
            if (index != null) {
                curIndex = index
                openViewer()
            }
        }

        private fun post(command: Command) {
            latestCommand.postValue(command.asConsumable())
        }
    }

    private val views by lazy {
        object {
            val mainLayout: View = findViewById(R.id.main_layout)
            val viewPager: ViewPager = findViewById(R.id.view_pager)
            val fragmentContent: View = findViewById(R.id.fragment_content)
            val prevClicker: View = findViewById(R.id.prev_clicker)
            val nextClicker: View = findViewById(R.id.next_clicker)
        }
    }

    lateinit var presenter: Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_viewer)

        presenter = ViewModelProviders.of(this)[PresenterImpl::class.java].apply {
            init(intent.getStringArrayExtra(PARAM_FILES))
        }

        setupFullscreen()

        lifecycle.addObserver(presenter)

        presenter.latestCommand.observe(
                this,
                Observer { value -> value?.consume(this@ViewerActivity::onCommand) }
        )

        presenter.currentFile.observe(
                this,
                Observer { _ -> updateButtons() }
        )

        views.prevClicker.setOnClickListener { presenter.viewPrev() }
        views.nextClicker.setOnClickListener { presenter.viewNext() }

        val fragmentAdapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(index: Int) = ViewerFragment.create(presenter.files[index])

            override fun getCount() = presenter.files.size
        }
        views.viewPager.adapter = fragmentAdapter

        views.viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            fun getFragment(id: Int): Fragment? {
                val fragmentTag = makeFragmentName(views.viewPager.id, fragmentAdapter.getItemId(id))
                return supportFragmentManager.findFragmentByTag(fragmentTag)
            }

            override fun onPageScrollStateChanged(p0: Int) {
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                //(getFragment(p0) as ViewerFragment?)?.
            }

            private fun makeFragmentName(viewId: Int, id: Long): String {
                return "android:switcher: $viewId:$id"
            }

        })
    }

    override fun setFragmentViewTouchListener(view: View) {
        //view.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
    }

    private fun onCommand(command: Command?) {
        command ?: return

        @Suppress("UNUSED_VARIABLE")
        val foo = when(command) {
            is Command.CloseActivity -> {
                finish()
            }
            is Command.OpenViewer -> {
                val file = command.file
                //openFragment(file)
            }
        }
    }

    private fun updateButtons() {
        views.prevClicker.visibility = if (presenter.hasPrev()) View.VISIBLE else View.GONE
        views.nextClicker.visibility = if (presenter.hasNext()) View.VISIBLE else View.GONE
    }

    private fun openFragment(file: String) {
        with (supportFragmentManager) {
            val fragment = ViewerFragment.create(file)

            beginTransaction()
                    .replace(views.fragmentContent.id, fragment, FRAGMENT_TAG)
                    .setCustomAnimations(
                            FragmentTransaction.TRANSIT_FRAGMENT_FADE,
                            FragmentTransaction.TRANSIT_FRAGMENT_FADE
                    )
                    .commit()
        }
    }

    private fun setupFullscreen() {
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                makeUiFullscreen()
            }
        }

        makeUiFullscreen()
    }

    private fun makeUiFullscreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
    }

    override fun onViewerError(file: String?) {
        file ?: return
        presenter.onViewerError(file)
    }
}
