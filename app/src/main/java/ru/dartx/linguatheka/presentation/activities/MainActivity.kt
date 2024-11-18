package ru.dartx.linguatheka.presentation.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import ru.dartx.linguatheka.BuildConfig
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ActivityMainBinding
import ru.dartx.linguatheka.databinding.NavHeaderBinding
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.db.entities.Card
import ru.dartx.linguatheka.presentation.activities.CardActivity.Companion.NEED_TO_SCROLL
import ru.dartx.linguatheka.presentation.adapters.CardAdapter
import ru.dartx.linguatheka.presentation.dialogs.AboutAppDialog
import ru.dartx.linguatheka.presentation.viewmodels.MainViewModel
import ru.dartx.linguatheka.settings.SettingsActivity
import ru.dartx.linguatheka.utils.BitmapManager
import ru.dartx.linguatheka.utils.LanguagesManager
import ru.dartx.linguatheka.utils.ThemeManager
import ru.dartx.linguatheka.workers.NotificationsWorker

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(), CardAdapter.Listener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var nvBinding: NavHeaderBinding
    private lateinit var cardActivityLauncher: ActivityResultLauncher<Intent>
    private var edSearch: EditText? = null
    private var adapter: CardAdapter? = null
    private var textWatcher: TextWatcher? = null
    private var width = 0
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory(MainDataBase.getDataBase(applicationContext as MainApp))
    }
    private lateinit var defPreference: SharedPreferences
    private var currentTheme = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        nvBinding = NavHeaderBinding.bind(binding.navView.getHeaderView(0))
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.rcViewCardList) { v, insets ->
            val bottomPadding =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = bottomPadding)
            WindowInsetsCompat.CONSUMED
        }
        hideShowFloatActionButton()
        init()
        showHTU()
        requestPermissions()
        cardListObserver()
    }

    private fun applyTheme() {
        currentTheme = defPreference.getString("theme", "blue").toString()
        setTheme(ThemeManager.getSelectedTheme(this))
    }

    override fun onResume() {
        super.onResume()
        if (binding.btFab.isOrWillBeHidden) binding.btFab.show()
        LanguagesManager.getUsedLanguages(applicationContext as MainApp)
        if (defPreference.getString(
                "theme", "blue"
            ) != currentTheme
        ) recreate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        width = resources.displayMetrics.widthPixels
        edSearch?.width = width
    }

    private fun expandActionView(): MenuItem.OnActionExpandListener {
        return object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                width = resources.displayMetrics.widthPixels
                edSearch?.width = width
                edSearch?.post {
                    edSearch?.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(edSearch, InputMethodManager.SHOW_IMPLICIT)
                }
                edSearch?.addTextChangedListener(textWatcher)
                searchListObserver()
                mainViewModel.allCards.removeObservers(this@MainActivity)
                mainViewModel.searchCard("%%")
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                edSearch?.removeTextChangedListener(textWatcher)
                edSearch?.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, 0, 0
                )
                edSearch?.setText("")
                invalidateOptionsMenu()
                mainViewModel.foundCards.removeObservers(this@MainActivity)
                cardListObserver()
                edSearch?.post {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(
                        currentFocus?.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
                }
                return true
            }
        }
    }

    private fun hideShowFloatActionButton() {
        with(binding) {
            rcViewCardList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        if (btFab.isOrWillBeShown) btFab.hide()
                    } else if (dy < 0) {
                        if (btFab.isOrWillBeHidden) btFab.show()
                    }
                }
            })
        }
    }

    private fun init() = with(binding) {
        rcViewCardList.layoutManager = LinearLayoutManager(this@MainActivity)
        adapter = CardAdapter(this@MainActivity)
        rcViewCardList.adapter = adapter
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        val drawerToggle = object :
            ActionBarDrawerToggle(
                this@MainActivity,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
            ) {
            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_SETTLING) {
                    if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        onDrawerOpen()
                    }
                }
                super.onDrawerStateChanged(newState)
            }
        }
        drawerLayout.addDrawerListener(drawerToggle)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.settings -> startActivity(
                    Intent(
                        this@MainActivity,
                        SettingsActivity::class.java
                    )
                )

                R.id.faq -> startActivity(
                    LargeTextActivity.intentForHowToUse(this@MainActivity)
                )

                R.id.donate -> {
                    val uriUrl = Uri.parse(getString(R.string.donate_url))
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            uriUrl
                        )
                    )
                }

                R.id.about -> {
                    val message = BuildConfig.VERSION_NAME
                    AboutAppDialog.showDialog(this@MainActivity, message)
                }
            }
            drawerLayout.close()
            true
        }
        toolbar.inflateMenu(R.menu.top_menu)
        val search = toolbar.menu.findItem(R.id.search)
        edSearch = search.actionView?.findViewById(R.id.edSearch)!!
        search.setOnActionExpandListener(expandActionView())
        textWatcher = textWatcher()
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                if (binding.drawerLayout.isDrawerVisible(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else if (search.isActionViewExpanded) {
                    search.collapseActionView()
                } else finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(
                this@MainActivity,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (binding.drawerLayout.isDrawerVisible(GravityCompat.START)) {
                            binding.drawerLayout.closeDrawer(GravityCompat.START)
                        } else if (search.isActionViewExpanded) {
                            search.collapseActionView()
                        } else finish()
                    }
                })
        }

        cardActivityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val needToScroll = it.data?.getBooleanExtra(
                        NEED_TO_SCROLL,
                        false
                    ) ?: false
                    if (needToScroll)
                        binding.rcViewCardList.postDelayed({
                            binding.rcViewCardList.smoothScrollToPosition(0)
                        }, 1000)
                }
            }

        btFab.setOnClickListener {
            startActivity(Intent(this@MainActivity, CardActivity::class.java))
        }
    }

    private fun onDrawerOpen() {
        val name = defPreference.getString("user_name", "")
        if (!name.isNullOrEmpty()) {
            val statsHeaderText: String = name + getString(R.string.stats_header_with_name)
            nvBinding.tvStatsHeader.text = statsHeaderText
        }
        nvBinding.tvStats.text = statsCount()
        val imageS = defPreference.getString("avatar", "")
        if (!imageS.isNullOrEmpty()) {
            val imageB = BitmapManager.decodeToBase64(imageS)
            nvBinding.ivAvatar.setImageBitmap(imageB)
        } else nvBinding.ivAvatar.setImageResource(R.drawable.ic_avatar)
    }

    private fun cardListObserver() {
        mainViewModel.allCards.observe(this) {
            adapter?.submitList(it)
        }
    }

    private fun searchListObserver() {
        mainViewModel.foundCards.observe(this) {
            val tempCardList = ArrayList<Card>()
            it.forEach { card ->
                tempCardList.add(card)
            }
            adapter?.submitList(tempCardList)
        }
    }

    private fun statsCount(): String {
        val tempCardList = mainViewModel.allCards.value
        var count = 0
        tempCardList?.forEach { card ->
            if (card.step == 9) count++
        }
        return if (tempCardList != null) {
            "$count ${getString(R.string.of)} ${
                resources.getQuantityString(
                    R.plurals.words,
                    tempCardList.size,
                    tempCardList.size
                )
            }"
        } else ""
    }

    override fun onClickCard(card: Card) {
        val i = CardActivity.intentCardActivityForEdit(this, card)
        cardActivityLauncher.launch(i)
    }

    private fun textWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (edSearch != null) {
                    if (!s.isNullOrEmpty()) {
                        edSearch!!.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            R.drawable.ic_close,
                            0
                        )
                        val iconSize = edSearch!!.compoundDrawables[2].bounds.width()
                        edSearch!!.setOnTouchListener { _, motionEvent ->
                            if (motionEvent.rawX >= width - iconSize) {
                                edSearch!!.setText("")
                                true
                            } else false
                        }
                    } else {
                        edSearch!!.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, 0, 0
                        )
                    }
                }
                mainViewModel.searchCard("%$s%")
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }
    }

    private fun showHTU() {
        if (!defPreference.getBoolean("not_show_htu", false)) {
            startActivity(
                LargeTextActivity.intentForHowToUse(this)
            )
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    NotificationsWorker.startNotificationsWorker(applicationContext)
                } else {
                    //TODO: Explain why need Notifications
                }
                return
            }

            else -> {
                println("Unknown request")
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "wordCH"
        const val REQUEST_CODE = 112
    }
}