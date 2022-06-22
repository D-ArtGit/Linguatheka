package ru.dartx.wordcards.activities

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityMainBinding
import ru.dartx.wordcards.databinding.NavHeaderBinding
import ru.dartx.wordcards.db.CardAdapter
import ru.dartx.wordcards.db.MainViewModel
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.settings.SettingsActivity
import ru.dartx.wordcards.workers.NotificationsWorker
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), CardAdapter.Listener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var nvBinding: NavHeaderBinding
    private var edSearch: EditText? = null
    private var adapter: CardAdapter? = null
    private lateinit var textWatcher: TextWatcher
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((applicationContext as MainApp).database)
    }
    private lateinit var defPreference: SharedPreferences
    private var currentTheme = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        currentTheme = defPreference.getString("theme", "blue").toString()
        setTheme(getSelectedTheme())
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        nvBinding = NavHeaderBinding.bind(binding.navView.getHeaderView(0))
        setContentView(binding.root)
        startWorker()
        init()
        cardListObserver()
        binding.btFab.setOnClickListener {
            val i = Intent(this, CardActivity::class.java)
            startActivity(i)
        }
    }

    override fun onResume() {
        super.onResume()
        if (defPreference.getString("theme", "blue") != currentTheme) recreate()
    }

    private fun expandActionView(): MenuItem.OnActionExpandListener {
        return object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                edSearch?.addTextChangedListener(textWatcher)
                searchListObserver()
                mainViewModel.allCards.removeObservers(this@MainActivity)
                mainViewModel.searchCard("%%")
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                edSearch?.removeTextChangedListener(textWatcher)
                edSearch?.setText("")
                invalidateOptionsMenu()
                mainViewModel.foundCards.removeObservers(this@MainActivity)
                cardListObserver()
                return true
            }
        }
    }

    private fun init() = with(binding) {
        setThemeColor(currentTheme)
        rcViewCardList.layoutManager = LinearLayoutManager(this@MainActivity)
        adapter = CardAdapter(this@MainActivity)
        rcViewCardList.adapter = adapter
        toolbar.setNavigationOnClickListener {
            val name = defPreference.getString("user_name", "")
            if (!name.isNullOrEmpty()) {
                val statsHeaderText: String = name + getString(R.string.stats_header_with_name)
                nvBinding.tvStatsHeader.text = statsHeaderText
            }
            nvBinding.tvStats.text = statsCount()
            val imageS = defPreference.getString("avatar", "")
            if (!imageS.isNullOrEmpty()) {
                val imageB = decodeToBase64(imageS)
                nvBinding.ivAvatar.setImageBitmap(imageB)
            } else nvBinding.ivAvatar.setImageResource(R.drawable.ic_avatar)
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.settings -> startActivity(
                    Intent(
                        this@MainActivity,
                        SettingsActivity::class.java
                    )
                )
            }
            drawerLayout.close()
            true
        }
        toolbar.inflateMenu(R.menu.top_menu)
        val search = toolbar.menu.findItem(R.id.search)
        edSearch = search.actionView.findViewById(R.id.edSearch) as EditText
        search.setOnActionExpandListener(expandActionView())
        textWatcher = textWatcher()
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
        return "$count of ${tempCardList?.size} words"
    }

    override fun onClickCard(card: Card) {
        val i = Intent(this, CardActivity::class.java)
        i.putExtra(CARD_DATA, card)
        startActivity(i)
    }

    private fun textWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mainViewModel.searchCard("%$s%")
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }
    }

    private fun startWorker() {
        val notificationsRequest =
            PeriodicWorkRequestBuilder<NotificationsWorker>(
                defPreference.getString(
                    "notifications_repeat_time",
                    "15"
                )!!.toLong(), TimeUnit.MINUTES
            )
                .addTag("notifications")
                .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationsRequest
        )
    }

    private fun decodeToBase64(input: String): Bitmap {
        val decodedByte = Base64.getDecoder().decode(input)
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
    }

    private fun getSelectedTheme(): Int {
        return when (defPreference.getString(
            "theme",
            "blue"
        )) {
            "blue" -> R.style.Theme_WordCardsBlue_NoActionBar
            "green" -> R.style.Theme_WordCardsGreen_NoActionBar
            else -> R.style.Theme_WordCardsRed_NoActionBar
        }
    }

    private fun setThemeColor(theme: String) {
        binding.apply {
            when (theme) {
                "blue" -> {
                    cLMainActivity.setBackgroundColor(getColor(R.color.blue_700))
                    toolbar.setBackgroundColor(getColor(R.color.blue_700))
                    mcViewMainActivity.setCardBackgroundColor(getColor(R.color.blue_50))
                }
                "green" -> {
                    cLMainActivity.setBackgroundColor(getColor(R.color.green_700))
                    toolbar.setBackgroundColor(getColor(R.color.green_700))
                    mcViewMainActivity.setCardBackgroundColor(getColor(R.color.green_50))
                }
                else -> {
                    cLMainActivity.setBackgroundColor(getColor(R.color.red_700))
                    toolbar.setBackgroundColor(getColor(R.color.red_700))
                    mcViewMainActivity.setCardBackgroundColor(getColor(R.color.red_50))
                }
            }
        }
    }

    companion object {
        const val CARD_DATA = "card"
        const val CHANNEL_ID = "wordCH"
    }
}