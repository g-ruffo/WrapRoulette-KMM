package ca.veltus.wraproulettekmm.android.ui.home.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import ca.veltus.wraproulettekmm.android.R
import ca.veltus.wraproulettekmm.android.base.BaseFragment
import ca.veltus.wraproulettekmm.android.data.objects.MemberBidItem
import ca.veltus.wraproulettekmm.android.data.objects.WinnerMemberItem
import ca.veltus.wraproulettekmm.android.databinding.FragmentSummaryBinding
import ca.veltus.wraproulettekmm.android.databinding.OptionsDialogBinding
import ca.veltus.wraproulettekmm.android.ui.WrapRouletteActivity
import ca.veltus.wraproulettekmm.android.ui.home.HomeViewModel
import ca.veltus.wraproulettekmm.android.utils.Constants.ADMIN_DIALOG
import ca.veltus.wraproulettekmm.android.utils.Constants.BID_DIALOG
import ca.veltus.wraproulettekmm.android.utils.Constants.MARGIN_DIALOG
import ca.veltus.wraproulettekmm.android.utils.Constants.PIR_DIALOG
import ca.veltus.wraproulettekmm.android.utils.FirebaseStorageUtil
import ca.veltus.wraproulettekmm.android.utils.intToStringOrdinal
import ca.veltus.wraproulettekmm.android.utils.toMemberBidItem
import ca.veltus.wraproulettekmm.android.utils.toWinnerMemberItem
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.OnItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SummaryFragment : BaseFragment() {

    private var _binding: FragmentSummaryBinding? = null
    override val _viewModel by viewModels<HomeViewModel>(ownerProducer = { requireParentFragment() })
    private val activityCast by lazy { activity as WrapRouletteActivity }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val onItemClick = OnItemClickListener { item, _ ->
        if (item is WinnerMemberItem) {
            launchViewWinnerEmailDialog(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = _viewModel
        binding.summaryCardView.viewModel = _viewModel
        binding.fragment = this
        binding.lifecycleOwner = viewLifecycleOwner

        setupScrollingListener()

        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    _viewModel.poolBetsList.collectLatest {
                        // Convert values to groupie MemberItems before sending it to the adapter.
                        if (it.isNotEmpty()) setupBidsRecyclerView(it.toMemberBidItem())
                    }
                }
                launch {
                    _viewModel.poolWinningMembers.collectLatest {
                        // Convert values to groupie WinnerMemberItems before sending it to the adapter.
                        setupWinnersRecyclerView(it.toWinnerMemberItem())
                    }
                }
                launch {
                    _viewModel.poolAdminProfileImage.collectLatest {
                        // If the admin has uploaded a profile image, load it into the card view.
                        if (!it.isNullOrEmpty()) Glide.with(requireContext()).asBitmap()
                            .load(FirebaseStorageUtil.pathToReference(it))
                            .placeholder(R.drawable.no_profile_image_member)
                            .into(binding.adminImage)
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        _viewModel.setIsScrolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (binding.summaryScrollView.scrollY > 100) {
            _viewModel.setIsScrolling(true)
        }

        if (_viewModel.poolAdminProfileImage.value != null) {
            // If the admin has uploaded a profile image, load it into the card view.
            Glide.with(requireContext()).asBitmap()
                .load(FirebaseStorageUtil.pathToReference(_viewModel.poolAdminProfileImage.value!!))
                .placeholder(R.drawable.no_profile_image_member).into(binding.adminImage)
        }
    }

    /**
     * Used to display pool details and rules in an alert dialog message when the corresponding card view
     * is clicked.
     */
    fun showPoolDetailDialog(dialogValue: Int) {
        if (_viewModel.currentPool.value == null) {
            _viewModel.postToastMessage(getString(R.string.poolHasntLoadedMessage))
        } else {
            var title = ""
            var description = ""
            when (dialogValue) {
                BID_DIALOG -> {
                    title = getString(R.string.bidAmountDialogTitle)
                    description = getString(
                        R.string.bidAmountDialogMessage, _viewModel.currentPool.value?.betAmount
                    )
                }
                ADMIN_DIALOG -> {
                    title = getString(R.string.poolAdminDialogTitle)
                    description = getString(
                        R.string.poolAdminDialogMessage, _viewModel.currentPool.value?.adminName
                    )
                }
                MARGIN_DIALOG -> {
                    title = getString(R.string.bettingMarginDialogTitle)
                    description = getString(
                        R.string.bettingMarginDialogPoolMessage,
                        _viewModel.currentPool.value?.margin
                    )
                }
                PIR_DIALOG -> {
                    title = getString(R.string.priceIsRightRules)
                    description = getString(
                        R.string.priceIsRightRulesDialogMessage,
                        _viewModel.currentPool.value?.pIRRulesEnabled.toString()
                    )
                }
            }
            val builder = MaterialAlertDialogBuilder(
                activityCast, R.style.NumberPickerDialog_MaterialComponents_MaterialAlertDialog
            )
            val view = OptionsDialogBinding.inflate(LayoutInflater.from(requireContext()))
            view.message.text = description
            view.title.text = title

            builder.apply {
                setView(view.root)
                setPositiveButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            }.show()
        }
    }

    /**
     * Sets an OnScrollChangeListener that hides the floating action buttons when the user starts to
     * scroll down.
     */
    private fun setupScrollingListener() {
        binding.summaryScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY < 100) {
                _viewModel.setIsScrolling()
            } else {
                _viewModel.setIsScrolling(true)
            }
        }
    }

    private fun setupBidsRecyclerView(items: List<MemberBidItem>) {
        setPositionTextView(items)
        val groupieAdapter = GroupieAdapter().apply {
            addAll(items)
        }
        binding.memberBidsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupieAdapter
            layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation)
        }
    }

    /**
     * Sets the users bid position that is displayed in the details card view.
     */
    private fun setPositionTextView(items: List<MemberBidItem>) {
        for (i in items.indices) {
            if (items[i].member.uid == _viewModel.userAccount.value!!.uid && items[i].member.displayName == _viewModel.userAccount.value!!.displayName) {
                binding.summaryCardView.positionTextView.text = intToStringOrdinal(i + 1)
            }
        }
    }

    private fun setupWinnersRecyclerView(items: List<WinnerMemberItem>) {
        val groupieAdapter = GroupieAdapter().apply {
            addAll(items)
            setOnItemClickListener(onItemClick)
        }
        binding.winnersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupieAdapter
            layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation)
        }
    }

    /**
     * Displays the winners email address in an alert dialog message when list item is clicked. If
     * member does not have an email saved, notify user with a snack bar message.
     */
    private fun launchViewWinnerEmailDialog(memberItem: WinnerMemberItem) {
        val email = memberItem.member.email
        if (email.isNullOrEmpty()) {
            _viewModel.postSnackBarMessage(getString(R.string.noMemberEmailMessage))
            return
        } else {
            val builder = MaterialAlertDialogBuilder(
                activityCast, R.style.NumberPickerDialog_MaterialComponents_MaterialAlertDialog
            )
            val view = OptionsDialogBinding.inflate(LayoutInflater.from(requireContext()))
            view.message.text = email
            view.title.text =
                getString(R.string.winnerEmailDialogTitle, memberItem.member.displayName)
            builder.apply {
                setView(view.root)
                setPositiveButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            }.show()
        }
    }
}