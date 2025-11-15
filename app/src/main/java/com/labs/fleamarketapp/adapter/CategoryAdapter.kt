package com.labs.fleamarketapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.labs.fleamarketapp.R
import com.labs.fleamarketapp.data.HomeCategory

class CategoryAdapter(
    private val categories: List<HomeCategory>,
    private val onCategoryClick: (HomeCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_card, parent, false)
        return CategoryViewHolder(view as MaterialCardView)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = categories.size

    private fun setSelected(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(selectedPosition)
    }

    inner class CategoryViewHolder(private val cardView: MaterialCardView) :
        RecyclerView.ViewHolder(cardView) {
        private val title: TextView = cardView.findViewById(R.id.categoryTitle)
        private val subtitle: TextView = cardView.findViewById(R.id.categorySubtitle)

        fun bind(category: HomeCategory, isSelected: Boolean) {
            cardView.isCheckable = true
            title.text = category.title
            subtitle.text = category.subtitle
            cardView.isChecked = isSelected
            cardView.strokeWidth = if (isSelected) 3 else 1
            cardView.strokeColor = cardView.context.getColor(
                if (isSelected) R.color.primary else R.color.divider
            )

            cardView.setOnClickListener {
                val position = adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                setSelected(position)
                onCategoryClick(category)
            }
        }
    }
}
