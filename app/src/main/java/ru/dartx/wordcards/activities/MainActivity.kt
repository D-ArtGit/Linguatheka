package ru.dartx.wordcards.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
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
import ru.dartx.wordcards.workers.NotificationsWorker
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

    override fun onCreate(savedInstanceState: Bundle?) {
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
        rcViewCardList.layoutManager = LinearLayoutManager(this@MainActivity)
        adapter = CardAdapter(this@MainActivity)
        rcViewCardList.adapter = adapter
        toolbar.setNavigationOnClickListener {
            nvBinding.tvStats.text = statsCount()
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            when (menuItem.itemId) {
                R.id.settings -> Toast
                    .makeText(this@MainActivity, "Setting", Toast.LENGTH_SHORT)
                    .show()
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
            PeriodicWorkRequestBuilder<NotificationsWorker>(15, TimeUnit.MINUTES)
                .addTag("notifications")
                .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationsRequest
        )
    }

    companion object {
        const val CARD_DATA = "card"
        const val CHANNEL_ID = "wordCH"
    }
}