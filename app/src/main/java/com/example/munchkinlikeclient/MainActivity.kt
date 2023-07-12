package com.example.munchkinlikeclient

import android.os.Bundle
import android.text.Editable
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.lifecycleScope
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset


class MainActivity : AppCompatActivity()
{
    private lateinit var _dataStore: DataStore<Preferences>
    private var _userProfile: JSONObject? = null
    private var _gameInfo: JSONObject? = null
    private var _gameList: JSONArray? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        _dataStore = createDataStore(name = "settings")
        runBlocking {
            sendLoginRequest()
        }
    }

    private fun showMenu()
    {
        setContentView(R.layout.user_profile)
        val userLogin: TextView = findViewById(R.id.UserName)
        userLogin.text = _userProfile!!.get("name").toString()
        findViewById<Button>(R.id.CreateButton).setOnClickListener {
            setContentView(R.layout.new_game)
                createGame()
        }
        findViewById<Button>(R.id.ResumeButton).setOnClickListener {
            setContentView(R.layout.resume_game)
            runBlocking {
                getGameList()
            }
        }
        findViewById<Button>(R.id.JoinButton).setOnClickListener {
            setContentView(R.layout.join_game)
                joinGame()
        }
    }

    private fun showGameInfo()
    {
        setContentView(R.layout.game_info)
        val gameName: TextView = findViewById(R.id.GameName)
        val gameOwner: TextView = findViewById(R.id.GameOwner)
        val joinToken: TextView = findViewById(R.id.JoinToken)
        val userList: ListView = findViewById(R.id.UserList)
        val result: ArrayList<String> = ArrayList()

        gameName.text = _gameInfo!!.getJSONObject("gameInfo").get("name").toString()
        gameOwner.text = _gameInfo!!.getJSONObject("gameInfo").getJSONObject("owner").get("name").toString()
        joinToken.text = _gameInfo!!.getJSONObject("gameInfo").get("joinToken").toString()
        val data: JSONArray = _gameInfo!!.getJSONArray("joinnedUser")
        var i = 0
        while (i < data.length())
        {
            result.add(data.getJSONObject(i).get("name").toString())
            i++
        }
        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this, R.layout.simple_list, R.id.Title, result)
        userList.adapter = arrayAdapter

    }

    //=============================== CREATE A GAME ==============================================

    private fun createGame()
    {
        val form : EditText = findViewById(R.id.GameNameForm)
        val slide : Slider = findViewById(R.id.PlayerSlider)

        findViewById<Button>(R.id.CreateButton).setOnClickListener {
            runBlocking {
                sendCreateRequest(form.text, slide.value)
            }
        }
    }

    private suspend fun sendCreateRequest(gameName : Editable, maxPlayer : Number)
    {
        val queue = Volley.newRequestQueue(this)
        val token: String = read("token") ?: ""

        val stringRequest = object : StringRequest(
            Method.POST,
            getString(R.string.back_url) + "game/" + "create",
            Response.Listener
            { response ->
                println("create response = ")
                println(response)
                _gameInfo = JSONObject(response)
                showGameInfo()
            },
            {
                    error ->
                println("create request")
                val tmp = String(error.networkResponse.data, Charset.forName("UTF-8"))
                val response = JSONObject(tmp)
                val toPrint = "Error " + response.get("statusCode").toString() + " : "  + response.get("message")
                Toast.makeText(this, toPrint, Toast.LENGTH_LONG).show()
            })
        {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String>
            {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token
                return headers
            }

            override fun getParams(): Map<String, String>
            {
                val params: MutableMap<String, String> = HashMap()
                params["GameName"] = gameName.toString()
                params["MaxPlayer"] = maxPlayer.toInt().toString()
                return params
            }
        }
        queue.add(stringRequest)
    }


    //=============================== RESUME A GAME ==============================================

    private suspend fun getGameList()
    {
        val queue = Volley.newRequestQueue(this)
        val token: String = read("token") ?: ""

        val stringRequest = object : StringRequest(Method.GET,
            getString(R.string.back_url) + "game/" + "getgamelist",
            Response.Listener
            { response ->
                println("gamelist response = ")
                println(response)
                _gameList = JSONArray(response)
                showGameList()
            },
            {
                    error ->
                println("gamelist request")
                val tmp = String(error.networkResponse.data, Charset.forName("UTF-8"))
                val response = JSONObject(tmp)
                val toPrint = "Error " + response.get("statusCode").toString() + " : "  + response.get("message")
                Toast.makeText(this, toPrint, Toast.LENGTH_LONG).show()
            })
        {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String>
            {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token
                return headers
            }
        }
        queue.add(stringRequest)
    }

    private fun showGameList()
    {
        val adapter: ListAdapter
        val projectList: ArrayList<DataModel?> = ArrayList()
        val list: ListView = findViewById(R.id.projectList)
        var i = 0
        var gameId: Number
        var name: String
        var max: Number

        if (_gameList === null)
            return
        while (i < _gameList!!.length())
        {
            gameId = _gameList?.getJSONObject(i)?.get("id").toString().toInt()
            name = _gameList?.getJSONObject(i)?.get("name").toString()
            max = _gameList?.getJSONObject(i)?.get("maxPlayer").toString().toInt()
            projectList.add(DataModel(gameId, name, max))
            i++
        }
        adapter = ListAdapter(applicationContext, projectList)
        list.adapter = adapter
        list.onItemClickListener = OnItemClickListener { parent, view, position, id ->
        }
    }


    //=============================== Join A GAME ==============================================

    private fun joinGame()
    {
        val form = findViewById<EditText>(R.id.InviteForm)

        findViewById<Button>(R.id.SubmitButton).setOnClickListener {
            runBlocking {
                sendJoinRequest(form.text)
            }
        }
    }

    private suspend fun sendJoinRequest(code : Editable)
    {
        val queue = Volley.newRequestQueue(this)
        val token: String = read("token") ?: ""

        val stringRequest = object : StringRequest(
            Method.POST,
            getString(R.string.back_url) + "game/" + "joinrequest",
            Response.Listener
            { response ->
                println("join response = ")
                println(response)
                _gameInfo = JSONObject(response)
                showGameInfo()
            },
            {
                    error ->
                println("join request")
                val tmp = String(error.networkResponse.data, Charset.forName("UTF-8"))
                val response = JSONObject(tmp)
                val toPrint = "Error " + response.get("statusCode").toString() + " : "  + response.get("message")
                Toast.makeText(this, toPrint, Toast.LENGTH_LONG).show()
            })
        {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String>
            {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token
                return headers
            }

            override fun getParams(): Map<String, String>
            {
                val params: MutableMap<String, String> = HashMap()
                params["Code"] = code.toString()
                return params
            }
        }
        queue.add(stringRequest)
    }



    //=============================== LOGIN ==============================================

    private suspend fun sendLoginRequest()
    {
        val queue = Volley.newRequestQueue(this)

            val token = read("token") ?: ""
            val stringRequest = object : StringRequest(
                Method.POST,
                getString(R.string.back_url) + "auth/" + "login",
                Response.Listener
                { response ->
                    _userProfile = JSONObject(response)
                    println("login response = ")
                    println(response)
                    lifecycleScope.launch {
                        if (!_userProfile!!.isNull("token"))
                            save("token", _userProfile!!.get("token").toString())
                        if (_userProfile!!.get("registered") == true)
                            showMenu()
                        else
                            makeRegister()
                    }
                },
                {
                        error ->
                    println("login request")
                    val tmp = String(error.networkResponse.data, Charset.forName("UTF-8"))
                    val response = JSONObject(tmp)
                    val toPrint = "Error " + response.get("statusCode").toString() + " : "  + response.get("message")
                    Toast.makeText(this, toPrint, Toast.LENGTH_LONG).show()
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = token
                    return headers
                }
            }
            queue.add(stringRequest)
    }

    private fun makeRegister()
    {
        setContentView(R.layout.name_form)
        val form = findViewById<EditText>(R.id.NameForm)

        findViewById<Button>(R.id.SubmitButton).setOnClickListener {
            runBlocking {
                sendRegisterRequest(form.text)
            }
        }
    }

    private suspend fun sendRegisterRequest(name : Editable)
    {
        val queue = Volley.newRequestQueue(this)
        val token: String = read("token") ?: ""

        val stringRequest = object : StringRequest(
            Method.POST,
            getString(R.string.back_url) + "auth/" + "register",
            Response.Listener
            { response ->
                println("register response = ")
                println(response)
                _userProfile = JSONObject(response)
                showMenu()
            },
            {
                    error ->
                println("register request")
                val tmp = String(error.networkResponse.data, Charset.forName("UTF-8"))
                val response = JSONObject(tmp)
                val toPrint = "Error " + response.get("statusCode").toString() + " : "  + response.get("message")
                Toast.makeText(this, toPrint, Toast.LENGTH_LONG).show()
            })
        {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String>
            {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token
                return headers
            }

            override fun getParams(): Map<String, String>
            {
                val params: MutableMap<String, String> = HashMap()
                params["name"] = name.toString()
                return params
            }
        }
        queue.add(stringRequest)
    }


    //=============================== UTILITY ==============================================

    private suspend fun save(key: String, value: String)
    {
        val dataStoreKey = preferencesKey<String>(key)
        _dataStore.edit { settings -> settings[dataStoreKey] = value }
    }

    private suspend fun read(key: String): String?
    {
        val dataStoreKey = preferencesKey<String>(key)
        val preferences = _dataStore.data.first()
        return (preferences[dataStoreKey])
    }
}