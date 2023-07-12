package com.example.munchkinlikeclient

public class DataModel(id: Number, name: String, max: Number)
{
    private var id: Number = id;
    private var name: String = name;
    private var max: Number = max;

    fun getId(): Number
    {
        return id;
    }

    fun getName(): String
    {
        return name;
    }

    fun getMax(): Number
    {
        return max;
    }
}