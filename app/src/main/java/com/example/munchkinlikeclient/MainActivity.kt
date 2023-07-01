package com.example.munchkinlikeclient

import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.lifecycleScope
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity()
{
    private lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        dataStore = createDataStore(name = "settings")
        findViewById<Button>(R.id.LogButton).setOnClickListener{
            lifecycleScope.launch {
                sendLoginRequest();
                makeRegister();
            }
        }
    }

    private fun makeRegister()
    {
        setContentView(R.layout.name_form)
        val form = findViewById<EditText>(R.id.NameForm);

        findViewById<Button>(R.id.SubmitButton).setOnClickListener {
            lifecycleScope.launch {
                sendRegisterRequest(form.text);
            }
        }
    }

    private suspend fun sendRegisterRequest(name : Editable)
    {
        val queue = Volley.newRequestQueue(this)
        var token: String = read("token") ?: "";

        val stringRequest = object : StringRequest(Request.Method.POST,
            getString(R.string.back_url) + "register",
            Response.Listener
            { response ->
                val responseJson = JSONObject(response)
                if (!responseJson.isNull("token"))
                    lifecycleScope.launch{
                        save("token", responseJson.get("token").toString())
                    }
            },
            { response ->
                println(response);
            })
        {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String>
            {
                val headers = HashMap<String, String>();
                headers["Authorization"] = token ?: "";
                return headers;
            }

            override fun getParams(): Map<String, String>
            {
                val params: MutableMap<String, String> = HashMap()
                params["name"] = name.toString();
                return params
            }
        }
        queue.add(stringRequest)
    }

    private suspend fun sendLoginRequest()
    {
        val queue = Volley.newRequestQueue(this)
        var token: String = read("token") ?: "";

        val stringRequest = object : StringRequest(Request.Method.POST,
            getString(R.string.back_url) + "login",
            Response.Listener
            { response ->
                val responseJson = JSONObject(response)
                if (!responseJson.isNull("token"))
                    lifecycleScope.launch{
                        save("token", responseJson.get("token").toString())
                    }
            },
            { response ->
                println(response);
            })
            {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): MutableMap<String, String>
                {
                    val headers = HashMap<String, String>();
                    headers["Authorization"] = token ?: "";
                    return headers;
                }
            }
        queue.add(stringRequest)
    }

    private suspend fun save(key: String, value: String)
    {
        val dataStoreKey = preferencesKey<String>(key);
        dataStore.edit { settings -> settings[dataStoreKey] = value }
    }

    private suspend fun read(key: String): String?
    {
        val dataStoreKey = preferencesKey<String>(key);
        val preferences = dataStore.data.first()
        return (preferences[dataStoreKey]);
    }
}