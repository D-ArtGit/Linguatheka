package ru.dartx.linguatheka.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Spanned
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.widget.addTextChangedListener
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ActivityCardBinding
import ru.dartx.linguatheka.db.ExampleAdapter
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.db.MainViewModel
import ru.dartx.linguatheka.dialogs.ConfirmDialog
import ru.dartx.linguatheka.entities.Card
import ru.dartx.linguatheka.entities.Example
import ru.dartx.linguatheka.model.ExampleItem
import ru.dartx.linguatheka.settings.SettingsActivity
import ru.dartx.linguatheka.utils.*
import ru.dartx.linguatheka.utils.TimeManager.addDays
import ru.dartx.linguatheka.utils.TimeManager.getCurrentTime
import ru.dartx.linguatheka.utils.TimeManager.isTimeToSetNewRemind

class CardActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var binding: ActivityCardBinding
    private var ab: ActionBar? = null
    private var card: Card? = null
    private var cardState = CARD_STATE_VIEW
    private var daysArray = intArrayOf()
    private var timeToSetRemind = false
    private var adapter: ExampleAdapter? = null

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory(MainDataBase.getDataBase(applicationContext as MainApp))
    }
    private var langArray = emptyArray<Array<String>>()
    private var index = -1
    private var defLang = ""
    private val exampleList: ArrayList<ExampleItem> = arrayListOf()
    private var requestFocusOnAddedExample = true


    override fun onCreate(savedInstanceState: Bundle?) {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(ThemeManager.getSelectedTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        langArray = LanguagesManager.getLanguages()
        daysArray = resources.getIntArray(R.array.remind_days)
        defLang = defPreference.getString("def_lang", "").toString()
        if (defLang.isEmpty()) defLang = "en"
        showLangSettings(defPreference)
        setAdapter()
        getCard()
        fieldState()
        actionBarSettings()
        setOnClickListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.card_menu, menu)
        val itemEdit = menu?.findItem(R.id.edit)
        val itemReset = menu?.findItem(R.id.reset)
        val itemDelete = menu?.findItem(R.id.delete)
        if (cardState == CARD_STATE_VIEW) {
            itemEdit?.isVisible = true
        }
        if (cardState == CARD_STATE_NEW) {
            itemReset?.isVisible = false
            itemDelete?.isVisible = false
            itemEdit?.isVisible = false
        }
        if (cardState == CARD_STATE_EDIT) {
            itemEdit?.isVisible = false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.delete -> deleteCard()
            R.id.reset -> resetCardState()
            R.id.edit -> editCardState()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getCard() {
        card = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(MainActivity.CARD_DATA, Card::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra(MainActivity.CARD_DATA) as Card?
        }
        if (card != null) {
            with(NotificationManagerCompat.from(applicationContext)) { cancel(card!!.id!!) }
            println("GetCard: ${card!!.id}")
            binding.apply {
                index = langArray[0].indexOf(card!!.lang)
                if (index < 0) index = langArray[0].indexOf(defLang)
                tvLang.text = langArray[1][index]
                tvCardWord.text = card!!.word
                edWord.setText(card?.word)
            }
            CoroutineScope(Dispatchers.Main).launch {
                val foundExamples = withContext(Dispatchers.IO) {
                    val examplesForCard = mainViewModel.findExampleByCardId(card!!.id!!)
                    examplesForCard
                }
                withContext(Dispatchers.Main) {
                    if (foundExamples.isNotEmpty()) {
                        foundExamples.forEachIndexed { index, example ->
                            exampleList.add(
                                ExampleItem(
                                    example.id,
                                    example.card_id,
                                    HtmlManager.getFromHtml(example.example).trim() as Spanned,
                                    HtmlManager.getFromHtml(example.translation).trim() as Spanned,
                                    View.VISIBLE,
                                    View.GONE,
                                    null,
                                    false,
                                    example.finished
                                )
                            )
                            adapter?.notifyItemInserted(index)
                        }
                    }
                }
            }
            timeToSetRemind = isTimeToSetNewRemind(card!!.remindTime)
            cardState = if (timeToSetRemind) CARD_STATE_CHECK
            else CARD_STATE_VIEW
        } else cardState = CARD_STATE_NEW
    }

    private fun setAdapter() = with(binding) {
        rvCardItems.layoutManager = LinearLayoutManager(this@CardActivity)
        adapter = ExampleAdapter(exampleList)
        rvCardItems.adapter = adapter
    }

    private fun fieldState() = with(binding) {
        when (cardState) {
            CARD_STATE_CHECK -> btSave.setImageResource(R.drawable.ic_check)
            CARD_STATE_VIEW -> btSave.visibility = View.GONE
            else -> editScreenState()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnClickListeners() = with(binding) {
        btAddExample.setOnClickListener {
            exampleListAddEmpty()
        }
        btSave.setOnClickListener {
            setMainResult()
        }
        edWord.addTextChangedListener {
            if (!it.isNullOrEmpty()) {
                edWord.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_close_grey,
                    0
                )
                val iconSize = edWord.compoundDrawables[2].bounds.width()
                edWord.setOnTouchListener { _, motionEvent ->
                    if (motionEvent.rawX >= edWord.width + 30 - iconSize) {
                        edWord.setText("")
                        true
                    } else false
                }
            } else {
                edWord.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, 0, 0
                )
            }
        }
    }

    private fun setMainResult() {
        binding.edWord.requestFocus()
        val tempCard = if (card == null) {
            newCard()
        } else {
            updateCard()
        }
        if (tempCard != null) {
            when (cardState) {
                CARD_STATE_NEW -> {
                    val tmpExampleList: ArrayList<Example> = arrayListOf()
                    exampleList.forEachIndexed { index, exampleItem ->
                        tmpExampleList.add(
                            Example(
                                index + 1,
                                0,
                                HtmlManager.toHtml(exampleItem.example),
                                HtmlManager.toHtml(exampleItem.translation),
                                false
                            )
                        )
                    }
                    mainViewModel.insertCard(tempCard, tmpExampleList)
                }
                CARD_STATE_EDIT, CARD_STATE_EDIT_AND_RESET -> {
                    val tmpExampleList: ArrayList<Example> = arrayListOf()
                    exampleList.forEachIndexed { index, exampleItem ->
                        tmpExampleList.add(
                            Example(
                                index + 1,
                                exampleItem.card_id,
                                HtmlManager.toHtml(exampleItem.example),
                                HtmlManager.toHtml(exampleItem.translation),
                                exampleItem.finished
                            )
                        )
                    }
                    mainViewModel.updateCardWithItems(tempCard, tmpExampleList)
                }
                else -> {
                    mainViewModel.updateCard(tempCard)
                }
            }
            val i = Intent().putExtra(MainActivity.CARD_STATE, cardState)
            setResult(RESULT_OK, i)
            finish()
        }
    }

    private fun newCard(): Card? {
        val currentTime = getCurrentTime()
        val remindTime = addDays(currentTime, daysArray[0])
        binding.apply {
            var emptyItemIndex =
                exampleList.indexOfFirst { it.example.isEmpty() && it.translation.isEmpty() }
            while (emptyItemIndex >= 0) {
                exampleList.removeAt(emptyItemIndex)
                adapter?.notifyItemRemoved(emptyItemIndex)
                if (exampleList.size > emptyItemIndex) {
                    adapter?.notifyItemRangeChanged(emptyItemIndex, exampleList.size - 1)
                }
                emptyItemIndex =
                    exampleList.indexOfFirst { it.example.isEmpty() && it.translation.isEmpty() }
            }
            if (edWord.text.isNullOrEmpty()) {
                edWord.error = getString(R.string.fill_field)
                return null
            } else if (exampleList.isEmpty()) {
                Toast.makeText(
                    this@CardActivity,
                    getString(R.string.no_examples),
                    Toast.LENGTH_LONG
                )
                    .show()
                exampleListAddEmpty()
                return null
            } else {
                var examplesForCard = ""
                var translationForCard = ""
                var exampleIsEmpty = false
                exampleList.forEachIndexed { index, it ->
                    if (it.example.isEmpty()) {
                        it.error = getString(R.string.no_example)
                        exampleIsEmpty = true
                        adapter?.notifyItemChanged(index)
                    } else {
                        if (!it.error.isNullOrEmpty()) {
                            it.error = null
                            adapter?.notifyItemChanged(index)
                        }
                        if (index > 0) {
                            examplesForCard += "\n"
                        }
                        examplesForCard += "${it.example}"
                        translationForCard += "${it.translation}"
                    }
                }
                return if (exampleIsEmpty) null
                else return Card(
                    null,
                    langArray[0][index],
                    edWord.text.toString(),
                    examplesForCard,
                    translationForCard,
                    currentTime,
                    remindTime,
                    0
                )
            }
        }
    }

    private fun updateCard(): Card? = with(binding) {
        var remindTime = addDays(getCurrentTime(), daysArray[0])
        var step = 0
        if (cardState != CARD_STATE_RESET && cardState != CARD_STATE_EDIT_AND_RESET) {
            step = card!!.step
            remindTime = card!!.remindTime
            if (timeToSetRemind) {
                step++
                remindTime = if (step <= 8) {
                    addDays(getCurrentTime(), daysArray[step])
                } else {
                    cardState = CARD_STATE_EDIT
                    TimeManager.ENDLESS_FUTURE
                }
            }
        }

        var emptyItemIndex =
            exampleList.indexOfFirst { it.example.isEmpty() && it.translation.isEmpty() }
        while (emptyItemIndex >= 0) {
            exampleList.removeAt(emptyItemIndex)
            adapter?.notifyItemRemoved(emptyItemIndex)
            if (exampleList.size > emptyItemIndex) {
                adapter?.notifyItemRangeChanged(emptyItemIndex, exampleList.size - 1)
            }
            emptyItemIndex =
                exampleList.indexOfFirst { it.example.isEmpty() && it.translation.isEmpty() }
        }
        if (edWord.text.isNullOrEmpty()) {
            edWord.error = getString(R.string.fill_field)
            return null
        } else if (exampleList.isEmpty()) {
            Toast.makeText(this@CardActivity, getString(R.string.no_examples), Toast.LENGTH_LONG)
                .show()
            exampleListAddEmpty()
            return null
        } else {
            var examplesForCard = ""
            var translationForCard = ""
            var exampleIsEmpty = false
            exampleList.forEachIndexed { index, it ->
                if (it.example.isEmpty()) {
                    it.error = getString(R.string.no_example)
                    exampleIsEmpty = true
                    adapter?.notifyItemChanged(index)
                } else {
                    if (!it.error.isNullOrEmpty()) {
                        it.error = null
                        adapter?.notifyItemChanged(index)
                    }
                    if (index > 0) {
                        examplesForCard += "\n"
                    }
                    examplesForCard += "${it.example}"
                    translationForCard += "${it.translation}"
                    if (step > 8) {
                        it.finished = true
                    }
                }
            }
            return if (exampleIsEmpty) null
            else card?.copy(
                word = edWord.text.toString(),
                lang = langArray[0][index],
                examples = examplesForCard,
                translation = translationForCard,
                remindTime = remindTime,
                step = step
            )
        }
    }

    private fun deleteCard() {
        val message = getString(R.string.confirm_delete)
        ConfirmDialog.showDialog(
            this, object : ConfirmDialog.Listener {
                override fun onClick() {
                    mainViewModel.deleteCard(card?.id!!)
                    finish()
                }

                override fun onCancel() {

                }
            },
            message
        )
    }

    private fun editCardState() {
        cardState = CARD_STATE_EDIT
        invalidateOptionsMenu()
        ab?.setTitle(R.string.edit_card)
        editScreenState()
    }

    private fun resetCardState() {
        val message = getString(R.string.confirm_reset)
        ConfirmDialog.showDialog(
            this, object : ConfirmDialog.Listener {
                override fun onClick() {
                    cardState = if (cardState == CARD_STATE_EDIT) CARD_STATE_EDIT_AND_RESET
                    else CARD_STATE_RESET
                    setMainResult()
                }

                override fun onCancel() {

                }
            },
            message
        )
    }

    private fun actionBarSettings() {
        ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)
        when (cardState) {
            CARD_STATE_CHECK -> ab?.setTitle(R.string.repeat_card)
            CARD_STATE_NEW -> ab?.setTitle(R.string.fill_card)
            CARD_STATE_VIEW -> ab?.setTitle(R.string.view_card)
        }
    }

    private fun editScreenState() {
        binding.apply {
            val spinner = spLang
            val spinnerArrayAdapter =
                ArrayAdapter(this@CardActivity, R.layout.spinner, langArray[1])
            spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            spinner.adapter = spinnerArrayAdapter
            index = langArray[0].indexOf(defLang)
            spinner.setSelection(index)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    index = position
                    tvLang.text = langArray[1][index]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
            btAddExample.visibility = View.VISIBLE
            btSave.visibility = View.VISIBLE
            btSave.setImageResource(R.drawable.ic_save)
            tvCardWord.visibility = View.GONE
            tvLang.visibility = View.GONE
            edWord.visibility = View.VISIBLE
            spLang.visibility = View.VISIBLE
            if (cardState == CARD_STATE_NEW) {
                requestFocusOnAddedExample = false
                exampleListAddEmpty()
                edWord.requestFocus()
            } else editListState()
        }
    }

    private fun editListState() {
        exampleList.forEachIndexed { index, it ->
            it.edVisibility = View.VISIBLE
            it.tvExampleVisibility = View.GONE
            adapter?.notifyItemChanged(index)
        }
    }

    private fun showLangSettings(defPreference: SharedPreferences) {
        if (defPreference.getString("def_lang", "") == "" ||
            defPreference.getString("native_lang", "") == ""
        ) {
            Toast.makeText(this, getString(R.string.choose_lang_settings), Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    this@CardActivity,
                    SettingsActivity::class.java
                )
            )
            finish()
        }
    }

    private fun exampleListAddEmpty() {
        var cardId = 0
        if (card != null) {
            cardId = card!!.id!!
            if (card!!.step > 8) cardState = CARD_STATE_EDIT_AND_RESET
        }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (!imm.isAcceptingText) {
            binding.edWord.requestFocus()
            imm.showSoftInput(
                binding.edWord,
                InputMethodManager.SHOW_IMPLICIT
            )
        }
        exampleList.add(
            ExampleItem(
                0,
                cardId,
                HtmlManager.getFromHtml(""),
                HtmlManager.getFromHtml(""),
                View.GONE,
                View.VISIBLE,
                null,
                requestFocus = requestFocusOnAddedExample,
                finished = false
            )
        )

        requestFocusOnAddedExample = true
        adapter?.notifyItemInserted(exampleList.size - 1)
        binding.scView.postDelayed({
            binding.scView.smoothScrollTo(0, resources.displayMetrics.heightPixels)
        }, 100)
    }

    companion object {
        const val CARD_STATE_NEW = 1
        const val CARD_STATE_EDIT = 2
        const val CARD_STATE_VIEW = 3
        const val CARD_STATE_EDIT_AND_RESET = 4
        const val CARD_STATE_CHECK = 5
        const val CARD_STATE_RESET = 6
    }
}