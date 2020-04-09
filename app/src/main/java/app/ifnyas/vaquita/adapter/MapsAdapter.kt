package app.ifnyas.vaquita.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.ifnyas.vaquita.R
import app.ifnyas.vaquita.data.MapsData
import kotlinx.android.synthetic.main.item_maps.view.*
import java.math.RoundingMode


class MapsAdapter(private var items: List<MapsData> = ArrayList()) :
    RecyclerView.Adapter<MapsAdapter.MViewHolder>() {

    class MViewHolder(view: View) : RecyclerView.ViewHolder(view)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MViewHolder {
        return MViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_maps, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MViewHolder, position: Int) {

        if (items[position].insider == 1) {
            holder.itemView.visibility = View.VISIBLE
        }

        val distanceKm = items[position].distance / 1000
        val distanceText = distanceKm.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toFloat()

        val locText = "Lat: ${items[position].lat}, Lng: ${items[position].lng}"
        val nameText = "${items[position].displayName} ($distanceText Km)"
        holder.itemView.name_view.text = nameText
        holder.itemView.loc_view.text = locText
    }

    override fun getItemCount() = items.size
}

interface OnItemClickListener {
    fun onItemClicked(position: Int, view: View)
}

fun RecyclerView.addOnItemClickListener(listener: OnItemClickListener) {
    addOnChildAttachStateChangeListener(object: RecyclerView.OnChildAttachStateChangeListener {
        override fun onChildViewDetachedFromWindow(view: View) {
            view.setOnClickListener(null)
        }

        override fun onChildViewAttachedToWindow(view: View) {
            view.setOnClickListener {
                val holder = getChildViewHolder(view)
                listener.onItemClicked(holder.adapterPosition, view)
            }
        }
    })
}