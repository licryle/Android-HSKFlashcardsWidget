package fr.berliat.hskwidget.data.store

import android.content.Context
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation.ClassLevel
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation.ClassType

class AppPreferencesStore(private val context: Context):
    PrefixedPreferenceDataStoreBridge(context.dataStore, "app") {
    var lastAnnotatedClassLevel : ClassLevel
        get() {
            val field = getString("class_level", "其他") ?: return ClassLevel.NotFromClass

            return ClassLevel.from(field) ?: ClassLevel.NotFromClass
        }
        set(classLevel : ClassLevel) {
            putString("class_level", classLevel.toString())
        }

    var lastAnnotatedClassType : ClassType
        get() {
            val field = getString("class_type", "其他") ?: return ClassType.NotFromClass

            return ClassType.from(field) ?: ClassType.NotFromClass
        }
        set(classType : ClassType) {
            putString("class_type", classType.toString())
        }
}