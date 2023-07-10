package com.example.munchkinlikeclient

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.munchkinlikeclient.DataModel

class ListAdapter(private val context: Context, values: ArrayList<DataModel?>?) :
    ArrayAdapter<DataModel?>(context, R.layout.row_item, values!!)
{
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
    {
        val dataModel = getItem(position)
        var rowView: View? = null
        var inflater: LayoutInflater? = null
        if (rowView == null) inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        rowView = inflater?.inflate(R.layout.row_item, parent, false)
        val nameView = rowView!!.findViewById<View>(R.id.name) as TextView
        nameView.text = dataModel!!.getPair().second;
        return rowView
    }
}