package ru.dartx.wordcards.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityMainBinding
import ru.dartx.wordcards.db.CardAdapter
import ru.dartx.wordcards.db.MainViewModel
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.TimeManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var edSearch: EditText? = null
    private lateinit var adapter: CardAdapter
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((applicationContext as MainApp).database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val card = it.data?.getSerializableExtra("card") as Card
                mainViewModel.insertCard(card)
            }
        }
        init()
        binding.btFab.setOnClickListener {
            val i = Intent(this, NewCardActivity::class.java)
            i.putExtra("card", 0)
            launcher.launch(i)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        val search = menu!!.findItem(R.id.search)
        edSearch = search.actionView.findViewById(R.id.edSearch) as EditText
        search.setOnActionExpandListener(expandActionView())
        return true
    }

    private fun expandActionView(): MenuItem.OnActionExpandListener {
        return object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                invalidateOptionsMenu()
                return true
            }


        }
    }

    private fun init() = with(binding) {
        rcViewCardList.layoutManager = LinearLayoutManager(this@MainActivity)
        rcViewCardList.adapter = adapter
    }
}