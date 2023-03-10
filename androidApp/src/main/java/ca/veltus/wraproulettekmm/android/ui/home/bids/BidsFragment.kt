package ca.veltus.wraproulettekmm.android.ui.home.bids

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import ca.veltus.wraproulettekmm.android.R
import ca.veltus.wraproulettekmm.android.base.BaseFragment
import ca.veltus.wraproulettekmm.android.data.objects.MemberItem
import ca.veltus.wraproulettekmm.android.databinding.AddMemberDialogBinding
import ca.veltus.wraproulettekmm.android.databinding.FragmentBidsBinding
import ca.veltus.wraproulettekmm.android.databinding.OptionsDialogBinding
import ca.veltus.wraproulettekmm.android.databinding.TimePickerDialogBinding
import ca.veltus.wraproulettekmm.android.ui.WrapRouletteActivity
import ca.veltus.wraproulettekmm.android.ui.home.HomeViewModel
import ca.veltus.wraproulettekmm.android.utils.toMemberItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.OnItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class BidsFragment : BaseFragment() {

    override val _viewModel by viewModels<HomeViewModel>(ownerProducer = { requireParentFragment() })
    private val activityCast by lazy { activity as WrapRouletteActivity }

    private var _binding: FragmentBidsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // This OnItemClickListener is only accessible by the pools admin. If pool is not active, notify user with toast message.
    private val onItemClick = OnItemClickListener { item, _ ->
        if ((item is MemberItem) && _viewModel.isPoolAdmin.value && !item.member.tempMemberUid.isNullOrEmpty()) {
            if (!_viewModel.isPoolActive.value) _viewModel.postSnackBarMessage(getString(R.string.poolFinishedCantUpdateMemberMessage))
            else launchTempMemberOptionsDialog(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBidsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.viewModel = _viewModel

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                _viewModel.memberList.collectLatest {
                    // Convert values to groupie MemberItems before sending it to the adapter.
                    setupRecyclerView(it.toMemberItem(_viewModel.userAccount.value?.uid ?: ""))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView(items: List<MemberItem>) {
        val groupieAdapter = GroupieAdapter().apply {
            // Display list items sorted by members bid times.
            addAll(items.sortedByDescending { it.member.bidTime })
            setOnItemClickListener(onItemClick)
        }

        binding.poolsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupieAdapter
        }
    }

    /**
     * This function can only be called by the pools admin and allows the user to either edit the temporary
     * members information or set the members bid time.
     */
    private fun launchTempMemberOptionsDialog(memberItem: MemberItem) {
        val builder = MaterialAlertDialogBuilder(
            activityCast, R.style.NumberPickerDialog_MaterialComponents_MaterialAlertDialog
        )
        val view = OptionsDialogBinding.inflate(LayoutInflater.from(requireContext()))
        view.message.text = getString(R.string.tempMemberOptionDialogMessage)
        view.title.text = getString(R.string.memberOptionsDialogTitle)

        builder.apply {
            setView(view.root)
            setPositiveButton(getString(R.string.bet)) { _, _ -> launchSetMemberBetDialog(memberItem) }
            setNegativeButton(getString(R.string.edit)) { _, _ ->
                launchUpdateTempMemberDialog(
                    memberItem
                )
            }
            setNeutralButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
        }.show()
    }

    /**
     * Called after the pools admin selects edit from the temporary members options dialog. If the pool is
     * not active or has finished, inform the user in a snack bar message that they are unable to make changes.
     */
    private fun launchUpdateTempMemberDialog(memberItem: MemberItem) {
        val builder = MaterialAlertDialogBuilder(
            activityCast, R.style.NumberPickerDialog_MaterialComponents_MaterialAlertDialog
        )
        val view = AddMemberDialogBinding.inflate(LayoutInflater.from(requireContext()))
        view.viewModel = _viewModel
        view.lifecycleOwner = viewLifecycleOwner
        view.member = memberItem.member

        // Load temporary members current saved information into the corresponding edit texts.
        _viewModel.loadTempMemberValues(memberItem.member)

        builder.apply {
            setView(view.root)
            setNeutralButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
            setNegativeButton(getString(R.string.delete)) { _, _ -> }
            setPositiveButton(getString(R.string.update)) { _, _ -> }
        }

        val dialog = builder.show()
        dialog.apply {
            setOnDismissListener { _viewModel.loadTempMemberValues(null) }
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (_viewModel.createUpdateTempMember(memberItem)) dialog.dismiss()
            }
            getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                if (memberItem.member.bidTime == null) {
                    launchDeleteMemberConfirmationDialog(memberItem)
                } else if (!_viewModel.isBettingOpen.value && _viewModel.isPoolActive.value) {
                    _viewModel.postSnackBarMessage(getString(R.string.bettingLockedPoolActiveCantRemoveMemberMessage))
                } else if (_viewModel.isBettingOpen.value && _viewModel.userBetTime.value != null) {
                    // Require the admin to clear the members bid time before removing them from the pool.
                    _viewModel.postSnackBarMessage(getString(R.string.clearBetBeforeLeavingMessage))
                    launchSetMemberBetDialog(memberItem)
                } else {
                    _viewModel.postSnackBarMessage(getString(R.string.unableRemoveMemberMessage))
                }
                dialog.dismiss()
            }
        }
    }

    /**
     * Called after the pools admin selects bet from the temporary members options dialog. The set bet
     * dialog is then shown to allow the admin to place a bet for the corresponding member.
     */
    private fun launchSetMemberBetDialog(memberItem: MemberItem) {
        val time = Calendar.getInstance()
        time.set(Calendar.SECOND, 0)
        time.set(Calendar.MILLISECOND, 0)

        val builder = MaterialAlertDialogBuilder(
            activityCast, R.style.NumberPickerDialog_MaterialComponents_MaterialAlertDialog
        )
        val view = TimePickerDialogBinding.inflate(LayoutInflater.from(requireContext()))
        view.apply {
            title.text = getString(R.string.setMemberBetTimeDialogTitle)
            message.text =
                getString(R.string.setMembersBetDialogSubtitle, memberItem.member.displayName)
            timePicker.setIs24HourView(true)
            timePicker.hour = time.get(Calendar.HOUR_OF_DAY)
            timePicker.minute = time.get(Calendar.MINUTE)
        }
        builder.apply {
            setView(view.root)
            setNeutralButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            setPositiveButton(getString(R.string.bet)) { _, _ -> }
        }

        if (memberItem.member.bidTime != null) builder.setNegativeButton(getString(R.string.clear)) { _, _ -> }

        val dialog = builder.show()

        dialog.apply {
            getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                _viewModel.setMemberPoolBet(
                    memberItem.member.poolId, memberItem.member.tempMemberUid!!, null
                )
                dialog.dismiss()
            }

            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                time.set(Calendar.HOUR_OF_DAY, view.timePicker.hour)
                time.set(Calendar.MINUTE, view.timePicker.minute)

                if (time.time.before(_viewModel.poolStartTime.value)) {
                    time.add(Calendar.DATE, 1)
                }
                _viewModel.setMemberPoolBet(
                    memberItem.member.poolId, memberItem.member.tempMemberUid!!, time.time
                )
                dialog.dismiss()
            }
        }
    }

    /**
     * Called after the pools admin selects delete from the temporary members edit dialog. If admin confirms
     * the temporary member is removed from the pool.
     */
    private fun launchDeleteMemberConfirmationDialog(memberItem: MemberItem) {
        val message = getString(R.string.areYouSureDialogSubtitle)
        val builder = MaterialAlertDialogBuilder(
            activityCast, R.style.NumberPickerDialog_MaterialComponents_MaterialAlertDialog
        )
        val view = OptionsDialogBinding.inflate(LayoutInflater.from(requireContext()))
        view.message.text = message
        view.title.text = getString(R.string.areYouSureDialogTitle)
        builder.apply {
            setView(view.root)
            setNeutralButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            setPositiveButton(getString(R.string.yes)) { _, _ ->
                _viewModel.deleteTempMember(memberItem)
            }
        }.show()
    }
}