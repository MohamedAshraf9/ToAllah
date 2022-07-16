package com.megahed.eqtarebmenalla.feature_data.presentation.ui.tasbeh

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.FragmentTasbehBinding


class TasbehFragment : Fragment() {

    private lateinit var binding: FragmentTasbehBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTasbehBinding.inflate(inflater, container, false)
        val root: View = binding.root
        


        return root
    }

}