package fr.berliat.hskwidget.androidResources

import android.content.Context
import android.widget.LinearLayout

class Dummy(context: Context) : LinearLayout(context) {
    init {
        inflate(context, R.layout.flashcard_widget, this)
    }
}