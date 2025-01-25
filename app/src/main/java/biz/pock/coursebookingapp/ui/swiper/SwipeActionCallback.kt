package biz.pock.coursebookingapp.ui.swiper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import biz.pock.coursebookingapp.R
import timber.log.Timber

class SwipeActionCallback<T>(
    context: Context,
    private val onDelete: ((T, resetSwipeState: () -> Unit) -> Unit)? = null,
    private val onEdit: ((T) -> Unit)? = null,
    private val getItem: (Int) -> T?
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteColor = context.getColor(R.color.error)
    private val editColor = context.getColor(R.color.primary)

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)?.apply {
        setTint(context.getColor(R.color.on_error))
    }
    private val editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit)?.apply {
        setTint(context.getColor(R.color.on_primary))
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.4f

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView

        when {
            dX < 0 -> drawDeleteBackground(c, itemView, dX)
            dX > 0 -> drawEditBackground(c, itemView, dX)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        try {
            val position = viewHolder.adapterPosition
            if (position < 0) return

            val recyclerView = viewHolder.itemView.parent as? RecyclerView
            val resetSwipeState: () -> Unit = {
                recyclerView?.adapter?.notifyItemChanged(position)
            }

            getItem(position)?.let { item ->
                when (direction) {
                    ItemTouchHelper.LEFT -> onDelete?.invoke(item, resetSwipeState)
                    ItemTouchHelper.RIGHT -> {
                        onEdit?.invoke(item)
                        resetSwipeState()
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error executing swipe action")
        }
    }

    private fun drawEditBackground(c: Canvas, itemView: View, dX: Float) {
        val background = RectF(
            itemView.left.toFloat(),
            itemView.top.toFloat(),
            dX,
            itemView.bottom.toFloat()
        )

        c.drawRect(background, Paint().apply { color = editColor })

        editIcon?.let {
            val iconMargin = (itemView.height - it.intrinsicHeight) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + it.intrinsicHeight
            val iconLeft = itemView.left + iconMargin
            val iconRight = iconLeft + it.intrinsicWidth
            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            it.draw(c)
        }
    }

    private fun drawDeleteBackground(c: Canvas, itemView: View, dX: Float) {
        val background = RectF(
            itemView.right.toFloat() + dX,
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )

        c.drawRect(background, Paint().apply { color = deleteColor })

        deleteIcon?.let {
            val iconMargin = (itemView.height - it.intrinsicHeight) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + it.intrinsicHeight
            val iconRight = itemView.right - iconMargin
            val iconLeft = iconRight - it.intrinsicWidth
            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            it.draw(c)
        }
    }
}