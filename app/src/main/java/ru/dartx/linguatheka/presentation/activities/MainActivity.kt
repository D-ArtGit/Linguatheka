package ru.dartx.linguatheka.presentation.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import ru.dartx.linguatheka.BuildConfig
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ActivityMainBinding
import ru.dartx.linguatheka.databinding.NavHeaderBinding
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.db.entities.Card
import ru.dartx.linguatheka.presentation.activities.CardActivity.Companion.CARD_STATE
import ru.dartx.linguatheka.presentation.adapters.CardAdapter
import ru.dartx.linguatheka.presentation.dialogs.AboutAppDialog
import ru.dartx.linguatheka.presentation.viewmodels.MainViewModel
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener.Companion.CARD_STATE_CHECK
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener.Companion.CARD_STATE_VIEW
import ru.dartx.linguatheka.settings.SettingsActivity
import ru.dartx.linguatheka.utils.BitmapManager
import ru.dartx.linguatheka.utils.LanguagesManager
import ru.dartx.linguatheka.utils.ThemeManager

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
        setContentView(binding.root)
        init()
        showHTU()
        cardListObserver()
    }

    private fun applyTheme() {
        currentTheme = defPreference.getString("theme", "blue").toString()
        setTheme(ThemeManager.getSelectedThemeNoBar(this))
    }

    override fun onResume() {
        super.onResume()
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
        edSearch = search.actionView?.findViewById(R.id.edSearch) as EditText
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
                    val cardState = it.data?.getIntExtra(
                        CARD_STATE,
                        CARD_STATE_VIEW
                    )
                    if (cardState == CARD_STATE_CHECK)
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

    companion object {
        const val CHANNEL_ID = "wordCH"
    }
}