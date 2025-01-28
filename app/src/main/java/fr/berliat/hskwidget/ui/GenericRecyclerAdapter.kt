package fr.berliat.hskwidget.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

interface BindableViewHolderInterface<T> {
    fun bind(item: T)
}

abstract class BindableViewHolder<T>(itemView: View) : BindableViewHolderInterface<T>,
    RecyclerView.ViewHolder(itemView) { }

open class GenericRecyclerAdapter<T, H: BindableViewHolder<T>, B: ViewBinding>(
    private val inflater: (LayoutInflater, ViewGroup, Boolean) -> B,
    private val dataChangedListener: ItemChangedListener<T>,
    private val itemFactory: (B) -> H
) : RecyclerView.Adapter<H>() {

    private val results = mutableListOf<T>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): H {
        val binding = inflater(LayoutInflater.from(parent.context), parent, false)

        return itemFactory(binding)
    }

    override fun onBindViewHolder(holder: H, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int {
        return results.size
    }

    // Method to add data to the adapter
    fun addData(newResults: List<T>) {
        results.addAll(newResults)
        notifySearchResultsChange()
    }

    // Method to clear existing data
    fun clearData() {
        results.clear()
        notifySearchResultsChange()
    }

    private fun notifySearchResultsChange() {
        notifyDataSetChanged() // You can optimize with notifyItemRangeInserted
        dataChangedListener.onDataChanged(results)  // Notify the activity or fragment
    }

    interface ItemChangedListener<T> {
        fun onDataChanged(newData: List<T>)
    }
}
