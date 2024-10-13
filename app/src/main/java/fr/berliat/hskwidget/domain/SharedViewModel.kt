package fr.berliat.hskwidget.domain

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    var segmenter = JiebaHSKTextSegmenter()

    init {
        viewModelScope.launch {
            segmenter.preload()
        }
    }

    companion object {
        fun getInstance(activity: AppCompatActivity): SharedViewModel {
            return ViewModelProvider(activity).get(SharedViewModel::class.java)
        }

        fun getInstance(fragment: Fragment): SharedViewModel {
            return ViewModelProvider(fragment.requireActivity()).get(SharedViewModel::class.java)
        }
    }
}