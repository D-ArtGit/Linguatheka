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
    private var isDuplicate = false
    private var alreadyAsked = false


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
            R.id.edit -> editCard()
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
            binding.apply {
                index = langArray[0].indexOf(card!!.lang)
                if (index < 0) index = langArray[0].indexOf(defLang)
                tvLang.text = langArray[1][index]
                tvCardWord.text = card!!.word
                edWord.setText(card?.word)
            }
            launch {
                val foundExamples = withContext(Dispatchers.IO) {
                    mainViewModel.findExampleByCardId(card!!.id!!)
                }
                withContext(Dispatchers.Main) {
                    if (foundExamples.isNotEmpty()) {
                        var needDivider = true
                        foundExamples.forEachIndexed { index, example ->
                            var dividerVisibility = false
                            if (example.finished && needDivider) {
                                needDivider = false
                                if (index > 0) dividerVisibility = true
                            }
                            exampleList.add(
                                ExampleItem(
                                    example.id,
                                    example.card_id,
                                    HtmlManager.getFromHtml(example.example).trim() as Spanned,
                                    HtmlManager.getFromHtml(example.translation).trim() as Spanned,
                                    View.VISIBLE,
                                    View.GONE,
                                    dividerVisibility,
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
                isDuplicate = false
                var cardId = 0
                if (card?.id != null && card?.id != 0) {
                    cardId = card!!.id!!
                }
                CoroutineScope(Dispatchers.Main).launch {
                    val duplicatesList = withContext(Dispatchers.IO) {
                        mainViewModel.findDuplicates(it.toString(), cardId)
                    }
                    withContext(Dispatchers.Main) {
                        if (duplicatesList.isNotEmpty()) isDuplicate = true
                    }
                }

                edWord.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, 0, 0
                )
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

                CARD_STATE_EDIT, CARD_STATE_EDIT_AND_RESET, CARD_STATE_EDIT_AND_CHECK -> {
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
            val exampleListIsEmpty = checkForEmptyList(exampleList)

            if (edWord.text.isNullOrEmpty()) {
                edWord.error = getString(R.string.fill_field)
                return null
            } else if (isDuplicate) {
                edWord.error = getString(R.string.word_exist)
                return null
            } else if (exampleListIsEmpty) {
                Toast.makeText(
                    this@CardActivity,
                    getString(R.string.no_examples),
                    Toast.LENGTH_LONG
                )
                    .show()
                if (exampleList.isEmpty()) exampleListAddEmpty()
                else {
                    exampleList[0].requestFocus = true
                    adapter?.notifyItemChanged(0)
                }
                return null
            } else {
                val texts = arrayListOf("", "")
                val exampleIsEmpty =
                    synchronizeCardWithList(exampleList, texts, 0)
                return if (exampleIsEmpty) null
                else return Card(
                    null,
                    langArray[0][index],
                    edWord.text.toString().trim(),
                    texts[0],
                    texts[1],
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
                    if (cardState == CARD_STATE_EDIT) cardState = CARD_STATE_EDIT_AND_CHECK
                    addDays(getCurrentTime(), daysArray[step])
                } else {
                    cardState = CARD_STATE_EDIT_AND_CHECK
                    TimeManager.ENDLESS_FUTURE
                }
            }
        }

        val exampleListIsEmpty = checkForEmptyList(exampleList)

        if (edWord.text.isNullOrEmpty()) {
            edWord.error = getString(R.string.fill_field)
            return null
        } else if (isDuplicate) {
            edWord.error = getString(R.string.word_exist)
            return null
        } else if (exampleListIsEmpty) {
            Toast.makeText(
                this@CardActivity,
                getString(R.string.no_examples),
                Toast.LENGTH_LONG
            )
                .show()
            if (exampleList.isEmpty()) exampleListAddEmpty()
            else {
                exampleList[0].requestFocus = true
                adapter?.notifyItemChanged(0)
            }
            return null
        } else {
            val texts = arrayListOf("", "")
            val exampleIsEmpty =
                synchronizeCardWithList(exampleList, texts, step)
            return if (exampleIsEmpty) null
            else card?.copy(
                word = edWord.text.toString().trim(),
                lang = langArray[0][index],
                examples = texts[0],
                translation = texts[1],
                remindTime = remindTime,
                step = step
            )
        }
    }

    private fun synchronizeCardWithList(
        exampleList: ArrayList<ExampleItem>,
        texts: ArrayList<String>,
        step: Int
    ): Boolean {
        exampleList.forEachIndexed { index, it ->
            if (it.example.isEmpty()) {
                it.error = getString(R.string.no_example)
                adapter?.notifyItemChanged(index)
                return true
            } else {
                if (!it.error.isNullOrEmpty()) {
                    it.error = null
                    adapter?.notifyItemChanged(index)
                }
                if (index > 0) {
                    texts[0] += "\n"
                }
                texts[0] += "${it.example}"
                texts[1] += "${it.translation}"
                if (step > 8) {
                    it.finished = true
                }
            }
        }
        return false
    }

    private fun checkForEmptyList(exampleList: ArrayList<ExampleItem>): Boolean {
        var exampleListIsEmpty = false
        if (exampleList.isEmpty()) {
            exampleListIsEmpty = true
        } else {
            var emptyItemIndex =
                exampleList.indexOfFirst { it.example.isEmpty() && it.translation.isEmpty() }
            while (emptyItemIndex >= 0) {
                if (exampleList.size > 1) {
                    exampleList.removeAt(emptyItemIndex)
                    adapter?.notifyItemRemoved(emptyItemIndex)
                    if (exampleList.size > emptyItemIndex) {
                        adapter?.notifyItemRangeChanged(emptyItemIndex, exampleList.size - 1)
                    }
                    emptyItemIndex =
                        exampleList.indexOfFirst { it.example.isEmpty() && it.translation.isEmpty() }
                } else {
                    exampleListIsEmpty = true
                    emptyItemIndex = -1
                }
            }
        }
        return exampleListIsEmpty
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

    private fun editCard() {
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
        if (card != null) cardId = card!!.id!!
        if (cardId != 0 && !alreadyAsked) {
            val message = getString(R.string.reset_for_added_example)
            ConfirmDialog.showDialog(
                this, object : ConfirmDialog.Listener {
                    override fun onClick() {
                        cardState = CARD_STATE_EDIT_AND_RESET
                        alreadyAsked = true
                        binding.scView.postDelayed({ newFocusedItem(cardId) }, 100)
                    }

                    override fun onCancel() {
                        alreadyAsked = true
                        binding.scView.postDelayed({ newFocusedItem(cardId) }, 100)
                    }
                },
                message
            )
        } else binding.scView.postDelayed({ newFocusedItem(cardId) }, 100)
    }

    private fun newFocusedItem(cardId: Int) {
        var delay = 100L
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (!imm.isAcceptingText) {
            delay = 400L
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
                false,
                null,
                requestFocus = requestFocusOnAddedExample,
                finished = false
            )
        )

        requestFocusOnAddedExample = true
        adapter?.notifyItemInserted(exampleList.size - 1)
        binding.scView.postDelayed({
            binding.scView.smoothScrollTo(0, Int.MAX_VALUE)
        }, delay)
    }

    companion object {
        const val CARD_STATE_NEW = 1
        const val CARD_STATE_EDIT = 2
        const val CARD_STATE_VIEW = 3
        const val CARD_STATE_EDIT_AND_RESET = 4
        const val CARD_STATE_EDIT_AND_CHECK = 5
        const val CARD_STATE_CHECK = 6
        const val CARD_STATE_RESET = 7
    }
}