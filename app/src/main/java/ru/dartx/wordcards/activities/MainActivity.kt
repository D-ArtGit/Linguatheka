package ru.dartx.wordcards.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityMainBinding
import ru.dartx.wordcards.db.CardAdapter
import ru.dartx.wordcards.db.MainViewModel
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.workers.NotificationsWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), CardAdapter.Listener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var edSearch: EditText? = null
    private var adapter: CardAdapter? = null
    private lateinit var textWatcher: TextWatcher
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((applicationContext as MainApp).database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startWorker()
        init()
        cardListObserver()
        onEditResult()
        binding.btFab.setOnClickListener {
            val i = Intent(this, CardActivity::class.java)
            i.putExtra(CARD_STATE, CARD_STATE_NEW)
            launcher.launch(i)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        val search = menu!!.findItem(R.id.search)
        edSearch = search.actionView.findViewById(R.id.edSearch) as EditText
        search.setOnActionExpandListener(expandActionView())
        textWatcher = textWatcher()
        return true
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

    override fun onClickCard(card: Card) {
        val i = Intent(this, CardActivity::class.java)
        i.putExtra(CARD_DATA, card)
        i.putExtra(CARD_STATE, CARD_STATE_VIEW)
        launcher.launch(i)
    }

    private fun onEditResult() {
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                when (it.data?.getIntExtra(CARD_STATE, CARD_STATE_VIEW)) {
                    CARD_STATE_NEW -> {
                        mainViewModel.insertCard(it.data?.getSerializableExtra(CARD_DATA) as Card)
                    }
                    CARD_STATE_DELETE -> {
                        mainViewModel.deleteCard(it.data?.getStringExtra(CARD_ID)!!.toInt())
                    }
                    else -> {
                        mainViewModel.updateCard(it.data?.getSerializableExtra(CARD_DATA) as Card)
                    }
                }
            }
        }
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
        const val CARD_ID = "card_id"
        const val CARD_STATE = "card_state"
        const val CARD_STATE_NEW = 1
        const val CARD_STATE_EDIT = 2
        const val CARD_STATE_VIEW = 3
        const val CARD_STATE_DELETE = 4
        const val CARD_STATE_CHECK = 5
        const val CARD_STATE_RESET = 6
        const val CHANNEL_ID = "wordCH"
    }
}