package com.example.munchkinlikeclient

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.munchkinlikeclient.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity()
{
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?)
    {

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dataStore = createDataStore(name = "settings")
        findViewById<Button>(R.id.LogButton).setOnClickListener{
            lifecycleScope.launch {
                sendLoginRequest();
                showProfile();
            }
        }
    }

    private fun showProfile()
    {
        setContentView(binding.root)
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