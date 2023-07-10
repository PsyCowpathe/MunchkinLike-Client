package com.example.munchkinlikeclient

public class DataModel(id: Number, name: String)
{
    private var pair: Pair<Number, String> = Pair(id, name);

    fun getPair(): Pair<Number, String>
    {
        return pair;
    }
}