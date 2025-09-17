package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.QuranListenerAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentQuranListenerBinding
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.navigation.findNavController
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.edit

@AndroidEntryPoint
class QuranListenerFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentQuranListenerBinding
    private lateinit var quranListenerAdapter: QuranListenerAdapter
    private var fromFavorite: Boolean = false

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fromFavorite = arguments?.let { QuranListenerFragmentArgs.fromBundle(it).fromFavorite }!!

        sharedPreferences =
            requireActivity().getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val quranListenerViewModel = ViewModelProvider(this)[QuranListenerViewModel::class.java]

        binding = FragmentQuranListenerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)

        setupQuickActions(quranListenerViewModel)
        setupRecyclerView(quranListenerViewModel)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        loadData(quranListenerViewModel)

        return root
    }

    private fun setupQuickActions(quranListenerViewModel: QuranListenerViewModel) {

        if (fromFavorite) {
            binding.quickActionsContainer.visibility = View.GONE
            setupFavoriteScreen()
        } else {
            binding.quickActionsContainer.visibility = View.VISIBLE
            setupQuickActionClickListeners()
        }
    }

    private fun setupQuickActionClickListeners() {
        binding.cardReaderFavorites.setOnClickListener {
            navigateToReaderFavorites()
        }

        binding.cardSoraFavorites.setOnClickListener {
            navigateToSoraFavorites()
        }

        binding.cardListeningSaved.setOnClickListener {
            navigateToListeningSaved()
        }
    }

    private fun setupRecyclerView(quranListenerViewModel: QuranListenerViewModel) {
        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranListenerAdapter = QuranListenerAdapter(
            requireContext(), object : OnItemWithFavClickListener<QuranListenerReader> {

                override fun onItemClick(
                    itemObject: QuranListenerReader,
                    view: View?,
                    position: Int,
                ) {
                    sharedPreferences.edit {
                        putBoolean("isPlayingSora", true)
                    }
                    val action: NavDirections =
                        QuranListenerFragmentDirections.actionNavigationListenerToQuranListenerReaderFragment(
                            id = itemObject.id, readerName = itemObject.name
                        )
                    requireView().findNavController().navigate(action)
                }

                override fun onItemFavClick(itemObject: QuranListenerReader, view: View?) {
                    itemObject.isVaForte = !itemObject.isVaForte
                    quranListenerViewModel.updateQuranListenerReader(itemObject)
                }

                override fun onItemLongClick(
                    itemObject: QuranListenerReader,
                    view: View?,
                    position: Int,
                ) {
                }
            })
        binding.recyclerView.adapter = quranListenerAdapter
    }

    private fun setupFavoriteScreen() {
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.toolbar.title = getString(R.string.readerFav)
    }

    private fun loadData(quranListenerViewModel: QuranListenerViewModel) {
        if (!fromFavorite) {
            binding.toolbar.toolbar.title = getString(R.string.listener)

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    quranListenerViewModel.getAllQuranListenerReader().collect {
                        handleDataLoading(it)
                    }
                }
            }
        } else {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    quranListenerViewModel.getFavoriteQuranListenerReader().collect {
                        handleDataLoading(it, showEmptyText = true)
                    }
                }
            }
        }
    }

    private fun handleDataLoading(data: List<QuranListenerReader>, showEmptyText: Boolean = false) {
        if (data.isEmpty()) {
            binding.loadingContainer.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.lottieView.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.loadingText.visibility = if (showEmptyText) View.VISIBLE else View.GONE
        } else {
            binding.loadingContainer.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.lottieView.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            binding.loadingText.visibility = View.GONE
        }
        quranListenerAdapter.setData(data)
    }

    private fun navigateToReaderFavorites() {
        val action: NavDirections =
            QuranListenerFragmentDirections.actionNavigationListenerSelf(true)
        requireView().findNavController().navigate(action)
    }

    private fun navigateToSoraFavorites() {
        val action: NavDirections =
            QuranListenerFragmentDirections.actionNavigationListenerToSoraFavoriteFragment()
        requireView().findNavController().navigate(action)
    }

    private fun navigateToListeningSaved() {
        val action: NavDirections =
            QuranListenerFragmentDirections.actionNavigationListenerToListenerHelperFragment()
        requireView().findNavController().navigate(action)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView

        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                quranListenerAdapter.filter.filter(newText)
                return false
            }
        })
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                requireView().findNavController().popBackStack()
                true
            }
            else -> false
        }
    }
}