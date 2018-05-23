package com.chochitos.inspirehep_citations

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter



class ViewAdapter(context: Context,
                  data: List<Any>,
                  rowBinder: (Int,View,Any) -> View,
                  layoutChoice: (Int) -> Int = {rowType->0},
                  getItemViewTypeFunction: (List<Any>, Int) -> Int = {listData,position -> 0},
                  getItemFunction: (List<Any>, Int) -> Any = {listData,position->listData[position]},
                  getViewTypeCountFunction: () -> Int = {1}
): BaseAdapter() {

    private val mData = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val mGetItemViewTypeFunction = getItemViewTypeFunction
    private val mGetItemFunction = getItemFunction
    private val mGetViewTypeCountFunction = getViewTypeCountFunction
    private val mLayoutChoice = layoutChoice
    private val mRowBinder = rowBinder


    override fun getCount(): Int {
        return mData.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): Any {
        return mGetItemFunction(mData,position)
    }

    override fun getItemViewType(position: Int): Int {
        return mGetItemViewTypeFunction(mData, position)
    }

    override fun getViewTypeCount(): Int {
        return mGetViewTypeCountFunction()
    }


    //for renring out eachview
    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {

        val rowType = getItemViewType(position)
        val view = convertView ?: mInflater.inflate(mLayoutChoice(rowType), viewGroup, false)

        return mRowBinder(rowType, view, getItem(position))

    }




}