package biz.pock.coursebookingapp.ui.adapters.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseListAdapter<T : Any, VB : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseListAdapter.BaseViewHolder<VB>>(diffCallback) {

    abstract fun createBinding(inflater: LayoutInflater, parent: ViewGroup): VB
    abstract fun bind(binding: VB, item: T)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = createBinding(inflater, parent)
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        bind(holder.binding, getItem(position))
    }

    class BaseViewHolder<VB : ViewBinding>(
        val binding: VB
    ) : RecyclerView.ViewHolder(binding.root)
}