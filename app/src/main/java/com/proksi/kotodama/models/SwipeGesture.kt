package com.proksi.kotodama.models

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.kotodama.tts.R


abstract class SwipeGesture(context: Context) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.deletetrash)
    private val background = ColorDrawable(Color.RED)
    private val clearPaint = Paint().apply { color = Color.TRANSPARENT }


    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Implement this in the subclass
    }

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
        val itemHeight = itemView.bottom - itemView.top

        // Limit the swipe to 20% of the item's width
        val maxSwipeDistance = itemView.width * 0.2f
        val clampedDx = dX.coerceAtMost(maxSwipeDistance)

        val isCancelled = clampedDx == 0f && !isCurrentlyActive

        if (isCancelled) {
            clearCanvas(c, itemView.right + clampedDx, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw the red delete background
        background.setBounds(itemView.right + clampedDx.toInt(), itemView.top, itemView.right, itemView.bottom)
        background.draw(c)

        // Calculate the position of the delete icon
        val iconMargin = (itemHeight - deleteIcon!!.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemHeight - deleteIcon.intrinsicHeight) / 2
        val iconBottom = iconTop + deleteIcon.intrinsicHeight
        val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
        val iconRight = itemView.right - iconMargin

        // Draw the delete icon
        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        deleteIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        c.drawRect(left, top, right, bottom,clearPaint)
    }
}
