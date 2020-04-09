package app.ifnyas.vaquita.data

import android.os.Parcelable
import android.text.BoringLayout
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MapsData(
    var id: String,
    var displayName: String,
    var totalConfirmed: Int,
    var totalDeaths: Int,
    var totalRecovered: Int,
    var lat: Double,
    var lng: Double,
    var insider: Int,
    var distance: Float
) : Parcelable