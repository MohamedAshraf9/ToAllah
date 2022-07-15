package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.megahed.eqtarebmenalla.databinding.FragmentQuranListenerReaderBinding

class QuranListenerReaderFragment : Fragment() {

    private lateinit var binding: FragmentQuranListenerReaderBinding
    private lateinit var viewModel: QuranListenerReaderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(QuranListenerReaderViewModel::class.java)
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuranListenerReaderBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }


}