package fr.berliat.hskwidget.ui.flashcard

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.databinding.FlashcardWidgetConfigureBinding
import fr.berliat.hskwidget.domain.FlashcardManager
import fr.berliat.hskwidget.domain.Utils


/**
 * The configuration screen for the [FlashcardWidgetProvider] AppWidget.
 */
class FlashcardConfigureActivity : AppCompatActivity() {
    private var _confFragment: FlashcardConfigureFragment? = null

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val confFragment get() = _confFragment!!

    private var addWidgetClick = View.OnClickListener {
        val context = this@FlashcardConfigureActivity

        // It is the responsibility of the configuration activity to update the app widget
        FlashcardManager.getInstance(context, widgetId).updateWord()

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_OK, resultValue)

        Utils.logAnalyticsWidgetAction(context, Utils.ANALYTICS_EVENTS.WIDGET_RECONFIGURE, widgetId)

        finish()
    }
    private lateinit var binding: FlashcardWidgetConfigureBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            widgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        _confFragment = FlashcardConfigureFragment.newInstance(widgetId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.flashcard_configure_container, confFragment)
            .commit()

        binding = FlashcardWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar!!.title = applicationContext.getString(R.string.add_widget_title)

        binding.flashcardWidgetConfigureAddwidget.setOnClickListener(addWidgetClick)
    }

    override fun onDestroy() {
        supportFragmentManager.beginTransaction().remove(confFragment).commitAllowingStateLoss()

        super.onDestroy()
    }
}