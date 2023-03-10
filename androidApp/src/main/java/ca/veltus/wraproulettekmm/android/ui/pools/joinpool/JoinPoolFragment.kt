package ca.veltus.wraproulettekmm.android.ui.pools.joinpool

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import ca.veltus.wraproulettekmm.android.R
import ca.veltus.wraproulettekmm.android.base.BaseFragment
import ca.veltus.wraproulettekmm.android.databinding.DatePickerDialogBinding
import ca.veltus.wraproulettekmm.android.databinding.FragmentJoinPoolBinding
import ca.veltus.wraproulettekmm.android.ui.WrapRouletteActivity
import ca.veltus.wraproulettekmm.android.ui.pools.PoolsViewModel
import ca.veltus.wraproulettekmm.android.utils.Constants
import ca.veltus.wraproulettekmm.android.utils.temporaryFocus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class JoinPoolFragment : BaseFragment() {

    override val _viewModel by viewModels<PoolsViewModel>()
    private var _binding: FragmentJoinPoolBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val activityCast by lazy { activity as WrapRouletteActivity }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_join_pool, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = _viewModel
        binding.fragment = this
        binding.lifecycleOwner = viewLifecycleOwner

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Launches an alert dialog for the user to select the date of the pool they are trying to join.
     */
    fun launchDatePickerDialog(editText: View) {
        editText.temporaryFocus()

        val calendar = Calendar.getInstance()

        val builder = MaterialAlertDialogBuilder(
            activityCast, R.style.NumberPickerDialog_MaterialComponents_MaterialAlertDialog
        )
        val view = DatePickerDialogBinding.inflate(LayoutInflater.from(requireContext()))
        view.apply {
            datePicker.minDate = calendar.time.time - Constants.YEAR
            datePicker.maxDate = calendar.time.time + Constants.YEAR
        }
        builder.apply {
            setView(view.root)
            setNeutralButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            setPositiveButton(getString(R.string.set)) { _, _ -> }
        }

        val dialog = builder.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            view.datePicker.apply {
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            _viewModel.setPoolDate(formatter.format(calendar.time))
            dialog.dismiss()
        }
        dialog.setOnDismissListener { editText.clearFocus() }
    }
}