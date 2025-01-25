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

class TrainerSwipeActionCallback<T>(
    context: Context,
    private val onEdit: ((T) -> Unit)? = null,
    private val getItem: (Int) -> T?
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) { // Nur RIGHT für Edit

    private val editColor = context.getColor(R.color.primary)
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

        if (dX > 0) { // Nur für Rechts-Swipe
            drawEditBackground(c, itemView, dX)
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
                if (direction == ItemTouchHelper.RIGHT) {
                    onEdit?.invoke(item)
                    resetSwipeState()
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
}