package ru.dartx.linguatheka.presentation.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.FragmentCardEditBinding
import ru.dartx.linguatheka.presentation.adapters.ExampleAdapter
import ru.dartx.linguatheka.presentation.dialogs.ConfirmDialog
import ru.dartx.linguatheka.presentation.viewmodels.CardViewModel
import ru.dartx.linguatheka.presentation.viewmodels.CardViewModelFactory
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener

class CardEditFragment : Fragment() {
    private var cardId = 0
    private var _binding: FragmentCardEditBinding? = null
    private val binding: FragmentCardEditBinding
        get() = _binding ?: throw RuntimeException("Binding is null")
    private val viewModelFactory: CardViewModelFactory by lazy {
        CardViewModelFactory(
            requireActivity().application,
            cardId
        )
    }
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[CardViewModel::class.java]
    }
    private lateinit var exampleListAdapter: ExampleAdapter
    private lateinit var onActionListener: OnActionListener
    private var isAlreadyAskedForReset = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnActionListener) onActionListener = context
        else throw RuntimeException("Activity must implement OnFinishedListener")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cardId = it.getInt(CARD_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.clCardEditFragment) { v, insets ->
            val bottomPadding =
                insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()).bottom
            v.setPadding(0, 0, 0, bottomPadding)
            WindowInsetsCompat.CONSUMED
        }

        exampleListAdapter = ExampleAdapter()
        binding.rvExampleList.adapter = exampleListAdapter
        setUpSpinner()
        observeViewModel()
        setOnClickListeners()
        setUpTextWatchers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpSpinner() {

        val spinner = binding.spLang
        val spinnerArrayAdapter =
            ArrayAdapter(
                requireContext(),
                R.layout.spinner,
                viewModel.langArray[1]
            )
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.adapter = spinnerArrayAdapter
        val langIndex = viewModel.langArray[0].indexOf(
            viewModel.cardWithExamplesUiState.value.card.lang
        )
        spinner.setSelection(langIndex)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.changeLang(position)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.cardWithExamplesUiState
                    .collect {
                        if (binding.edWord.text.isNullOrBlank()) {
                            if (it.card.word.isNotBlank()) {
                                binding.edWord.setText(it.card.word)
                            } else {
                                binding.edWord.requestFocus()
                            }
                        }
                        binding.edWord.error = it.inputWordError
                        exampleListAdapter.submitList(it.exampleList)
                        if (it.card.id == 0) {
                            onActionListener.setState(OnActionListener.CARD_STATE_NEW)
                        } else {
                            viewModel.setEditMode()
                            onActionListener.setState(OnActionListener.CARD_STATE_EDIT)
                        }
                    }
            }
        }

        viewModel.notifyItemInserted.observe(viewLifecycleOwner) {
            exampleListAdapter
                .notifyItemInserted(viewModel.cardWithExamplesUiState.value.exampleList.size - 1)
        }

        viewModel.notifyItemChanged.observe(viewLifecycleOwner) { indexOfUpdated ->
            indexOfUpdated.forEach {
                exampleListAdapter.notifyItemChanged(it)
            }
        }

        viewModel.notifyItemRemoved.observe(viewLifecycleOwner) { indexOfRemoved ->
            indexOfRemoved.forEach {
                exampleListAdapter.notifyItemRemoved(it)
            }
        }

        viewModel.shouldCloseActivity.observe(viewLifecycleOwner) {
            onActionListener.onFinished(it, false)
        }
    }

    private fun setOnClickListeners() {
        binding.btAddExample.setOnClickListener {
            if (
                viewModel.cardWithExamplesUiState.value.exampleList.isNotEmpty()
                && !isAlreadyAskedForReset
                && viewModel.cardWithExamplesUiState.value.card.step > 0
            ) {
                val message = getString(R.string.reset_for_added_example)
                ConfirmDialog.showDialog(
                    requireContext(), object : ConfirmDialog.Listener {
                        override fun onClick() {
                            viewModel.resetProgress(false)
                            isAlreadyAskedForReset = true
                            viewModel.addEmptyItem(true, null)
                        }

                        override fun onCancel() {
                            isAlreadyAskedForReset = true
                            viewModel.addEmptyItem(true, null)
                        }
                    },
                    message
                )
            } else {
                viewModel.addEmptyItem(true, null)
            }
        }

        exampleListAdapter.onRequestFocusWithKeyboard = { editText, id ->
            editText.requestFocus()
            editText.postDelayed({
                val imm =
                    requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(
                    editText,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }, 200L)
            binding.scView.postDelayed({
                binding.scView.smoothScrollTo(0, Int.MAX_VALUE)
            }, 600L)
            viewModel.resetRequestFocus(id)
        }

        exampleListAdapter.onDeleteClickListener = {
            viewModel.deleteItem(it)
        }

        exampleListAdapter.onTextChangedAfterError = { exampleItemId ->
            viewModel.resetInputExampleError(exampleItemId)
        }

        binding.btSave.setOnClickListener {
            viewModel.saveCard(binding.edWord.text.toString())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpTextWatchers() {
        with(binding) {
            edWord.addTextChangedListener {
                if (!it.isNullOrEmpty()) {
                    viewModel.resetInputWordError()
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
    }

    fun resetCard() {
        viewModel.resetProgress(true)
    }

    fun deleteCard() {
        viewModel.deleteCard()
    }

    companion object {
        private const val CARD_ID = "card_id"

        @JvmStatic
        fun newInstance(cardId: Int) =
            CardEditFragment().apply {
                arguments = Bundle().apply {
                    putInt(CARD_ID, cardId)
                }
            }
    }
}