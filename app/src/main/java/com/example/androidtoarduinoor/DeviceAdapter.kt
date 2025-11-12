package com.example.androidtoarduinoor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val devices: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val deviceName = devices[position]
        holder.tvDeviceName.text = deviceName
        holder.itemView.setOnClickListener { onItemClick(deviceName) }
    }

    override fun getItemCount() = devices.size
}
