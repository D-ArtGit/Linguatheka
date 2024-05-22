package ru.dartx.linguatheka.presentation.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.databinding.FragmentCardViewBinding
import ru.dartx.linguatheka.presentation.adapters.ExampleAdapter
import ru.dartx.linguatheka.presentation.viewmodels.CardViewModel
import ru.dartx.linguatheka.presentation.viewmodels.CardViewModelFactory
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener.Companion.CARD_STATE_CHECK
import ru.dartx.linguatheka.presentation.viewmodels.OnActionListener.Companion.CARD_STATE_VIEW

class CardViewFragment : Fragment() {

    private var cardId: Int = -1
    private lateinit var binding: FragmentCardViewBinding
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
        if (cardId < 0) throw RuntimeException("Undefined Card ID")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCardViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exampleListAdapter = ExampleAdapter()
        binding.rvExampleList.adapter = exampleListAdapter
        observeViewModel()
        binding.btComplete.setOnClickListener {
            viewModel.cardWithExamplesUiState.value.card.id?.let { viewModel.completeStep(it) }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.cardWithExamplesUiState.collect {
                    binding.tvCardWord.text = it.card.word
                    binding.tvLang.text = it.cardLangText
                    exampleListAdapter.submitList(it.exampleList)
                    if (it.isNeedCheck) {
                        binding.btComplete.visibility = View.VISIBLE
                        onActionListener.setState(CARD_STATE_CHECK)
                    }
                    else {
                        binding.btComplete.visibility = View.GONE
                        onActionListener.setState(CARD_STATE_VIEW)
                    }
                }
            }
        }

        viewModel.shouldCloseActivity.observe(viewLifecycleOwner) {
            onActionListener.onFinished(it, true)
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
            CardViewFragment().apply {
                arguments = Bundle().apply {
                    putInt(CARD_ID, cardId)
                }
            }
    }
}