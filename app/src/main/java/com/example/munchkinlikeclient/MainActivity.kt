package com.example.munchkinlikeclient

import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.nio.charset.Charset

class MainActivity : AppCompatActivity()
{
    private lateinit var dataStore: DataStore<Preferences>
    private var userProfile: JSONObject? = null;

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main);
        dataStore = createDataStore(name = "settings")
        //findViewById<Button>(R.id.LogButton).setOnClickListener{
            //runBlocking {
                sendLoginRequest();
           // }
       // }
    }

    private fun showUserProfile()
    {
        setContentView(R.layout.user_profile)
        val userLogin : TextView = findViewById(R.id.UserName);
        userLogin.setText(userProfile!!.get("name").toString());
    }

    private fun makeRegister()
    {
        setContentView(R.layout.name_form)
        val form = findViewById<EditText>(R.id.NameForm);

        findViewById<Button>(R.id.SubmitButton).setOnClickListener {
            runBlocking {
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
                println("response = ");
                println(response);
                userProfile = JSONObject(response)
                showUserProfile();
            },
            {
                error ->
                val tmp = String(error.networkResponse.data, Charset.forName("UTF-8"));
                val response = JSONObject(tmp);
                if (response.get("statusCode") === 400)
                    ;//toast returned message
                println(response.get("statusCode"))
                println(response.get("message"))
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

    private fun sendLoginRequest()
    {
        val queue = Volley.newRequestQueue(this)

        lifecycleScope.launch {
            var token = read("token") ?: "";
            val stringRequest = object : StringRequest(Request.Method.POST,
                getString(R.string.back_url) + "login",
                Response.Listener
                { response ->
                    userProfile = JSONObject(response)
                    println("login response = ")
                    println(response);
                    lifecycleScope.launch {
                        if (!userProfile!!.isNull("token"))
                            save("token", userProfile!!.get("token").toString())
                        if (userProfile!!.get("registered") === true)
                            showUserProfile();
                        else
                            makeRegister();
                    }
                },
                { response ->
                    println("error login request = ")
                    println(response);
                    //show toast network error
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>();
                        headers["Authorization"] = token;
                    return headers;
                }
            }
            queue.add(stringRequest)
        }
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