package com.example.lab7

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.lab7.data.StepsData
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MyAdapter(
    private val context: Context,
    private val activity: MainActivity,
    private val arrayList: MutableList<StepsData>,
    private val viewModel: MainViewModel,
) :
    BaseAdapter() {
    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val convertViewCopy = LayoutInflater.from(context).inflate(R.layout.row, parent, false)
        convertViewCopy.findViewById<TextView>(R.id.fromTime).text =
            arrayList[position].timestamp.toString()
        convertViewCopy.findViewById<TextView>(R.id.countSteps).text =
            arrayList[position].count.toString()
        convertViewCopy.findViewById<FloatingActionButton>(R.id.btnRemove).setOnClickListener {
            viewModel.removeSteps(activity, arrayList[position].timestamp)
        }

        return convertViewCopy
    }
}