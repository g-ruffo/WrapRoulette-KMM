package ca.veltus.wraproulettekmm.android.ui.pools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import ca.veltus.wraproulettekmm.android.R
import ca.veltus.wraproulettekmm.android.base.BaseFragment
import ca.veltus.wraproulettekmm.android.data.objects.PoolItem
import ca.veltus.wraproulettekmm.android.databinding.FragmentPoolsBinding
import ca.veltus.wraproulettekmm.android.utils.toPoolItem
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.OnItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PoolsFragment : BaseFragment() {

    override val _viewModel by viewModels<PoolsViewModel>()

    private var _binding: FragmentPoolsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val onItemClick = OnItemClickListener { item, _ ->
        if (item is PoolItem) {
            binding.poolsRecyclerView.isClickable = false
            _viewModel.setUsersActivePool(item.pool.docId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_pools, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                _viewModel.userAccount.zip(_viewModel.pools) { user, pools ->
                    Pair(pools, user)
                }.collect {
                    if (it.second != null) {
                        setupRecyclerView(it.first.toPoolItem(it.second!!))
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView(items: List<PoolItem>) {
        val groupieAdapter = GroupieAdapter().apply {
            addAll(items.sortedByDescending { it.pool.startTime })
            setOnItemClickListener(onItemClick)
        }
        binding.poolsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupieAdapter
        }
    }
}