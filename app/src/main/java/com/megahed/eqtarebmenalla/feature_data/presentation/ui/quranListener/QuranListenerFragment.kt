package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.megahed.eqtarebmenalla.databinding.FragmentQuranListenerBinding

class QuranListenerFragment : Fragment() {

    private lateinit var binding: FragmentQuranListenerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(QuranListenerViewModel::class.java)

        binding = FragmentQuranListenerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }


}