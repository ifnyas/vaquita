package app.ifnyas.vaquita

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.ifnyas.vaquita.activity.MapsActivity
import app.ifnyas.vaquita.api.ApiClient
import app.ifnyas.vaquita.api.ApiService
import app.ifnyas.vaquita.data.MapsData
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import com.livinglifetechway.quickpermissions_kotlin.util.QuickPermissionsOptions
import kotlinx.android.synthetic.main.activity_splash.*
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.math.RoundingMode

class Splash : AppCompatActivity() {

    lateinit var lm: LocationManager
    lateinit var loc: Location

    private val items = ArrayList<MapsData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // init fun
        initFun()
    }

    private fun initFun() {
        // init btn
        ref_btn.setOnClickListener {
            recreate()
        }

        // next fun
        text_view.text = "Checking permissions..."
        checkPermit()
    }

    private fun checkPermit() = runWithPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION,
        options = QuickPermissionsOptions (
            permissionsDeniedMethod = { finish() }
        )
    ) {
        setProgress("Checking Network and GPS...", 0)
        reqLoc()
    }

    private fun reqLoc() {

        lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var gpsEnabled = false
        var networkEnabled = false

        try {
            gpsEnabled = lm!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) {
            setProgress(e.toString(), 1)
        }

        try {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (e: SecurityException) {
            setProgress(e.toString(), 1)
        }

        when {
            !gpsEnabled -> {
                AlertDialog.Builder(this)
                    .setMessage("GPS disabled")
                    .setPositiveButton("Enable") { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        finish()
                    }
                    .setNegativeButton("Close") { _, _ -> finish() }
                    .create().show()
            }
            !networkEnabled -> {
                try {
                    AlertDialog.Builder(this)
                        .setMessage("Network disabled")
                        .setPositiveButton("Enable") { _, _ ->
                            startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
                            finish()
                        }
                        .setNegativeButton("Close") { _, _ -> finish() }
                        .create().show()
                } catch (e: SecurityException) {
                    setProgress(e.toString(), 1)
                    progress_bar.visibility = View.INVISIBLE
                }
            }
            else -> {
                try {
                    setProgress("Request location...", 0)
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0L, 0f, locationListener
                    )
                } catch (e: SecurityException) {
                    setProgress(e.toString(),1)
                    progress_bar.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun getData() {
        // remove location listener
        lm.removeUpdates(locationListener)

        // init API Service
        val apiService = ApiClient.client.create(ApiService::class.java)

        // get data
        apiService.getData().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                c: Call<ResponseBody>,
                r: Response<ResponseBody>
            ) {
                if (r.isSuccessful) {

                    try {
                        val responseJSON = JSONObject(r.body()!!.string())
                        val areasJSON = responseJSON.getJSONArray("areas")

                        var ind = 0
                        for (i in 0 until areasJSON.length()) {
                            val areaJSON = areasJSON.getJSONObject(i)
                            val id = areaJSON.getString("id")
                            if (id == "indonesia") {
                                ind = i
                            }
                        }

                        val idJSON = areasJSON.getJSONObject(ind)
                        val idAreasJSON =  idJSON.getJSONArray("areas")

                        for (i in 0 until idAreasJSON.length()) {

                            val idArea = idAreasJSON.getJSONObject(i)
                            val id = idArea.getString("id")
                            val displayName = idArea.getString("displayName")

                            val totalConfirmed = try {
                                idArea.getInt("totalConfirmed")
                            } catch (e: JSONException) { 0 }

                            val totalDeaths = try {
                                idArea.getInt("totalDeaths")
                            } catch (e: JSONException) { 0 }

                            val totalRecovered = try {
                                idArea.getInt("totalRecovered")
                            } catch (e: JSONException) { 0 }

                            val lat = idArea.getDouble("lat")
                                .toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble()
                            val lng = idArea.getDouble("long")
                                .toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble()

                            items.add(
                                MapsData(
                                    id,
                                    displayName,
                                    totalConfirmed,
                                    totalDeaths,
                                    totalRecovered,
                                    lat,
                                    lng,
                                    0,
                                    0f
                                )
                            )
                        }
                        setProgress("Have fun!", 0)
                        nextIntent()
                    }

                    catch (e: JSONException) {
                        setProgress(e.toString(), 1)
                        progress_bar.visibility = View.INVISIBLE
                    }
                    catch (e: IOException) {
                        setProgress(e.toString(), 1)
                        progress_bar.visibility = View.INVISIBLE
                    }
                }
            }

            override fun onFailure(c: Call<ResponseBody>, t: Throwable) {
                setProgress(t.toString(), 1)
            }
        })
    }

    private fun nextIntent() {
        Handler().postDelayed({
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("loc", loc)
            intent.putExtra("items", items)
            startActivity(intent)
            finish()
        }, 1000)
    }

    private fun setProgress(text: String, err: Int) {
        text_view.text = "${text_view.text}\n\n$text"
        if (err == 1) {
            progress_bar.visibility = View.INVISIBLE
            ref_btn.visibility = View.VISIBLE
        }
    }

    private val locationListener: LocationListener = object : LocationListener {

        override fun onLocationChanged(l: Location) {
            loc = l
            setProgress("Get data from API...", 0)
            getData()
        }

        override fun onStatusChanged(p: String, s: Int, e: Bundle) {}
        override fun onProviderEnabled(p: String) {}
        override fun onProviderDisabled(p: String) {}
    }
}
