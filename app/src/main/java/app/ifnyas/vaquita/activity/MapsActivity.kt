package app.ifnyas.vaquita.activity

import android.animation.LayoutTransition
import android.content.Intent
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.ifnyas.vaquita.R
import app.ifnyas.vaquita.Splash
import app.ifnyas.vaquita.adapter.MapsAdapter
import app.ifnyas.vaquita.adapter.OnItemClickListener
import app.ifnyas.vaquita.adapter.addOnItemClickListener
import app.ifnyas.vaquita.data.MapsData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import java.math.RoundingMode
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val mTag: String = "MA"
    private var expanded = 0

    private lateinit var mMap: GoogleMap
    private lateinit var mLoc: Location
    private lateinit var cLoc: LatLng
    private lateinit var markers: ArrayList<Marker>

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var itemInit: ArrayList<*>
    private lateinit var items: ArrayList<MapsData>
    private lateinit var shown: ArrayList<MapsData>

    // please change havocEnabled to false if you're a serious person
    private val havocEnabled = true
    private var havoc = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // enable layout transition
        maps_layout.layoutTransition
            .enableTransitionType(LayoutTransition.CHANGING)

        // init fun starter pack
        initFun()

        // init havoc!
        if (havocEnabled == true) {
            Handler().postDelayed({
                if (expanded == 1) expandList(0)
                Handler().postDelayed({
                    Toast.makeText(this, "RUN", Toast.LENGTH_LONG).show()
                    initHavoc()
                }, 500)
            }, 10000)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // init map
        mMap = googleMap
        afterMapReady()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Close app?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No") { _, _ -> }
            .create().show()
    }

    private fun initFun() {
        // set btn listener
        setBtn(0)

        // get intent extras
        getIntX()

        // get map fragment
        getMap()
    }

    private fun getMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun getIntX() {
        val intentExtra = intent.extras
        mLoc = intentExtra?.get("loc") as Location
        itemInit = intentExtra.get("items") as ArrayList<*>
        items = itemInit.filterIsInstance<MapsData>() as ArrayList<MapsData>
        shown = items.filter { it.insider == 1 } as ArrayList<MapsData>
        initRecycler()
    }

    private fun initRecycler() {
        viewManager = LinearLayoutManager(this)
        viewAdapter = MapsAdapter(shown)
        recycler_view.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    private fun afterMapReady() {
        // setting map
        setMapToggle()

        // apply clickable list item
        setBtn(1)

        // customise the styling of the map
        setMapStyle()

        // set map markers
        setMapMarker()
    }

    private fun setMapStyle() {
        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.style_json
                )
            )
            if (!success) {
                Log.e(mTag, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(mTag, "Can't find style. Error: ", e)
        }
    }

    private fun setMapToggle() {
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
    }

    private fun setMapMarker() {
        // init current location
        var mLatLng = LatLng(mLoc.latitude, mLoc.longitude)
        var mRad = mMap.addCircle(CircleOptions().center(mLatLng).visible(false))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 6f))

        // add user marker
        val mMark = mMap.addMarker((MarkerOptions()
            .position(mLatLng)).draggable(false).icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
        )

        // create items markers
        markers = arrayListOf()
        for (i in 0 until items.size) {
            val lat = items[i].lat
            val lng = items[i].lng
            val loc = LatLng(lat, lng)
            val marker = mMap.addMarker(
                MarkerOptions().position(loc)
                    .draggable(false)
                    .icon(
                        BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
            )
            markers.add(marker)
        }

        // create marker camera idle
        mMap.setOnCameraIdleListener {
            // make sure radius removed
            mRad.remove()

            // save current loc
            cLoc = mLatLng

            // hide view
            scroll_view.visibility = View.VISIBLE
            progress_bar.visibility = View.INVISIBLE

            // add radius circle
            mRad = mMap.addCircle(
                CircleOptions()
                    .center(mLatLng)
                    .radius(161803.3)
                    .strokeWidth(0f)
                    .fillColor(ContextCompat.getColor(this, R.color.colorRadius))
            )

            updateList()
        }

        // move marker when camera move
        mMap.setOnCameraMoveListener {
            // remove last radius
            mRad.remove()

            // show view progress
            progress_bar.visibility = View.VISIBLE
            scroll_view.visibility = View.INVISIBLE

            mLatLng = mMap.cameraPosition.target
            mMark.position = mLatLng
            cLoc = mLatLng
        }
    }

    private fun updateList() {
        // check inside radius
        for (i in 0 until items.size) {
            // init val
            val lat = items[i].lat
            val lng = items[i].lng
            val loc = LatLng(lat, lng)
            val distance = FloatArray(1)

            // calculate distance
            Location.distanceBetween(
                loc.latitude, loc.longitude,
                cLoc.latitude, cLoc.longitude, distance
            )
            items[i].distance = distance[0]

            // set item insider value
            if (distance[0] < 161803.3) {
                items[i].insider = 1
            } else {
                items[i].insider = 0
            }

            // update adapter
            if (items[i].insider == 1) {
                if (!shown.contains(items[i])) {
                    shown.add(items[i])
                }
            } else {
                if (shown.contains(items[i])) {
                    shown.remove((items[i]))
                }
            }
        }

        // sort and update list
        shown.sortBy { it.distance }
        viewAdapter.notifyDataSetChanged()

        // set info text
        info_view.text = when (viewAdapter.itemCount) {
            1 -> "1 Province near me"
            0 -> "No province near me"
            else -> "${viewAdapter.itemCount} Provinces near me"
        }

        // set end list text
        end_view.text = when (viewAdapter.itemCount) {
            0 -> "Empty list"
            else -> "End of list"
        }
    }

    private fun setBtn(before: Int) {
        // before onMapReady
        if (before == 0) {
            // card view
            card_view.setOnTouchListener { _, event ->
                val y = event.y.toInt()
                if (event.action == MotionEvent.ACTION_UP) {
                    if (y < -162) expandList(1)
                    if (y > 162) expandList(0)
                }
                return@setOnTouchListener true
            }

            // expand button
            exp_btn.setOnClickListener {
                if (expanded == 0) expandList(1) else expandList(0)
            }
        }

        // after onMapReady
        else {
            // item list
            recycler_view.apply {
                addOnItemClickListener(object: OnItemClickListener {
                    override fun onItemClicked(position: Int, view: View) {
                        val loc = LatLng(shown[position].lat, shown[position].lng)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 6f))
                    }
                })
            }

            // current location button
            loc_btn.setOnClickListener {
                val loc = LatLng(mLoc.latitude, mLoc.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 6f))
            }
        }
    }

    private fun expandList(exp: Int) {
        // set variables
        val cardParams = card_view.layoutParams
                as ConstraintLayout.LayoutParams
        val scrollParams = scroll_view.layoutParams
                as ConstraintLayout.LayoutParams

        if (exp == 1) {
            // set view as expanded
            expanded = 1

            // change button view
            exp_btn.animate().rotation(180f).start()

            // change layout params
            cardParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            scrollParams.matchConstraintPercentHeight = 0.95f
        }

        else {
            // set view as expanded
            expanded = 0

            // change button view
            exp_btn.animate().rotation(360f).start()

            // change layout params
            cardParams.topToTop = scroll_view.id
            scrollParams.matchConstraintPercentHeight = 0.5f
        }

        // delay animation
        Handler().postDelayed({
            // apply layout changes
            maps_layout.updateViewLayout(card_view, cardParams)
            maps_layout.updateViewLayout(scroll_view, scrollParams)
        },400)
    }

    private fun initHavoc() {
        // set view
        loc_btn.visibility = View.GONE
        exp_btn.visibility = View.GONE
        end_view.visibility = View.GONE

        // for your safety
        card_view.setOnTouchListener { _, _ ->
            return@setOnTouchListener true
        }
        mMap.setOnMarkerClickListener {
            return@setOnMarkerClickListener true
        }
        recycler_view.apply {
            addOnItemClickListener(object : OnItemClickListener {
                override fun onItemClicked(position: Int, view: View) {
                    return
                }
            })
        }

        // you only live once
        var live = 1

        // please stand away from zombies
        if (havoc == 0) {
            for (i in 0 until shown.size) {
                val distance = FloatArray(1)
                Location.distanceBetween(
                    shown[i].lat, shown[i].lng,
                    cLoc.latitude, cLoc.longitude, distance
                )

                if (distance[0] < 32360) {
                    cLoc = LatLng(-2.600029, 118.015776)
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(cLoc))
                }
            }
        }

        // start havoc
        havoc = 1
        havoc_view.visibility = View.VISIBLE
        havoc_view.text = "RUN, DON'T GET CAUGHT"

        // move zombies per sec
        Handler().postDelayed({
            // calculating movement
            for (i in 0 until markers.size) {
                val difX = markers[i].position.latitude - cLoc.latitude
                val newLat =
                    if (difX < 0) markers[i].position.latitude + 0.08
                    else markers[i].position.latitude - 0.08

                val difY = markers[i].position.longitude - cLoc.longitude
                val newLng =
                    if (difY < 0) markers[i].position.longitude + 0.08
                    else markers[i].position.longitude - 0.08

                val newLoc = LatLng(newLat, newLng)
                markers[i].position = newLoc

                // calculate distance
                val distance = FloatArray(1)
                Location.distanceBetween(
                    newLoc.latitude, newLoc.longitude,
                    cLoc.latitude, cLoc.longitude, distance
                )

                // set new values
                items[i].lat = newLoc.latitude.toBigDecimal()
                    .setScale(6, RoundingMode.HALF_UP).toDouble()

                items[i].lng = newLoc.longitude.toBigDecimal()
                    .setScale(6, RoundingMode.HALF_UP).toDouble()

                items[i].distance = distance[0]

                // if you get caught
                if (distance[0] < 16180) {
                    live = 0
                    // freeze map
                    mMap.uiSettings.setAllGesturesEnabled(false)
                }
            }

            // if still alive
            if (live == 1) {
                // run again
                viewAdapter.notifyDataSetChanged()
                updateList()
                initHavoc()
            } else {
                // run ends
                val geo = Geocoder(this, Locale.getDefault())
                val addresses: List<Address> = geo
                    .getFromLocation(cLoc.latitude, cLoc.longitude, 1)
                val place = addresses[0].getAddressLine(0)

                // set caught view
                recycler_view.visibility = View.GONE
                info_view.visibility = View.INVISIBLE
                caught_view.visibility = View.VISIBLE
                havoc_view.text = "You get caught in $place"

                // show retry btn
                retry_btn.visibility = View.VISIBLE
                retry_btn.setOnClickListener {
                    startActivity(Intent(this, Splash::class.java))
                    finish()
                }
            }
        }, 1000)
    }
}
