package app.ifnyas.vaquita.activity

import android.animation.LayoutTransition
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.ifnyas.vaquita.R
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val mTag: String = "MA"
    private var expanded = 0

    private lateinit var mMap: GoogleMap
    private lateinit var mLoc: Location

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var itemInit: ArrayList<*>
    private lateinit var items: ArrayList<MapsData>
    private lateinit var shown: ArrayList<MapsData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // enable layout transition
        maps_layout.layoutTransition
            .enableTransitionType(LayoutTransition.CHANGING)

        // init fun starter pack
        initFun()
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
        for (i in 0 until items.size) {
            val lat = items[i].lat
            val lng = items[i].lng
            val loc = LatLng(lat, lng)
            mMap.addMarker(MarkerOptions().position(loc)
                .draggable(false)
                .icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }

        // create marker camera idle
        mMap.setOnCameraIdleListener {
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
                    mRad.center.latitude, mRad.center.longitude, distance
                )
                items[i].distance = distance[0]

                // set item insider value
                if (distance[0] < mRad.radius) {
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
                shown.sortBy { it.distance }
                viewAdapter.notifyDataSetChanged()
            }

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

        // move marker when camera move
        mMap.setOnCameraMoveListener {
            // remove last radius and marker
            mRad.remove()

            // show view progress
            progress_bar.visibility = View.VISIBLE
            scroll_view.visibility = View.INVISIBLE

            mLatLng = mMap.cameraPosition.target
            mMark.position = mLatLng
        }

        // move marker when POI clicked
        mMap.setOnPoiClickListener {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, 16f))
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
}

//marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("zombie100s", 64, 64)))
//    private fun resizeMapIcons(iconName: String?, width: Int, height: Int): Bitmap? {
//        val imageBitmap = BitmapFactory.decodeResource(
//            resources,
//            resources.getIdentifier(iconName, "drawable", packageName)
//        )
//        return Bitmap.createScaledBitmap(imageBitmap, width, height, false)
//    }
