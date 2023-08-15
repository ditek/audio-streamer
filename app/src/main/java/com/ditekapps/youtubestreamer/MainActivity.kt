package com.ditekapps.youtubestreamer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE_INTERNET = 724
//    val REQUEST_URL = "http://www.google.com"
//    val REQUEST_URL = "http://10.0.2.2/src/put_url.php?url=www.new_url.com"
    val REQUEST_URL = "http://10.0.2.2/streams?url=www.new_url.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            if (checkPermission()) {
                sendRequests()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun checkPermission(): Boolean {
        // Check for permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // Explain to the user why you need the permission before asking again
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE_INTERNET
                );
            }
            return false
        }
        return true
    }

    fun sendRequests() {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "http://www.google.com"
//        val x = StringRe
        val jsonBody = JSONObject()
        jsonBody.put("url", url)
        jsonBody.put("valid", true)

        // Request a string response from the provided URL.
        val jsonRequest = JsonObjectRequest(
            Request.Method.POST, REQUEST_URL,
            jsonBody,
            Response.Listener<JSONObject>{response ->
                txt_main.text = "\"Response is: ${response.toString().substring(0, 500)}\""
            },
            Response.ErrorListener {
                txt_main.text = it.message
            })

        txt_main.text = "Request Sent"

//        val queue = Volley.newRequestQueue(this)
//        val url = "http://www.google.com"
//
//// Request a string response from the provided URL.
//        val stringRequest = StringRequest(Request.Method.GET, url,
//            Response.Listener<String> { response ->
//                // Display the first 500 characters of the response string.
//                txt_main.text = "Response is: ${response.substring(0, 500)}"
//            },
//            Response.ErrorListener { txt_main.text = it.message })
//
//// Add the request to the RequestQueue.
//        queue.add(stringRequest)


        val stringRequest = StringRequest(
            Request.Method.GET, REQUEST_URL,
            Response.Listener<String> { response ->
                // Display the first 500 characters of the response string.
                txt_main.text = "\"Response is: $response\""
            },
            Response.ErrorListener {
                txt_main.text = it.message
            })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
//        queue.add(jsonRequest)
    }

    // Handle the permissions request response
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE_INTERNET -> {
                // If request is canceled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    sendRequests()
                } else {
                    // permission denied. Disable the functionality.
                    Toast.makeText(
                        this, "Storage access denied. Can't load files.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }// other 'case' lines to check for other
        // permissions this app might request.
    }
}
