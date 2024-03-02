package de.westnordost.streetcomplete.screens.main.map.components

import android.content.res.Resources
import android.graphics.Color
import androidx.annotation.UiThread
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.ElementKey
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.osm.isOneway
import de.westnordost.streetcomplete.overlays.PointStyle
import de.westnordost.streetcomplete.overlays.PolygonStyle
import de.westnordost.streetcomplete.overlays.PolylineStyle
import de.westnordost.streetcomplete.overlays.Style
import de.westnordost.streetcomplete.screens.main.map.maplibre.clear
import de.westnordost.streetcomplete.screens.main.map.maplibre.toMapLibreGeometry
import de.westnordost.streetcomplete.screens.main.map.maplibre.toPoint
import de.westnordost.streetcomplete.util.ktx.addTransparency
import de.westnordost.streetcomplete.util.ktx.darken
import de.westnordost.streetcomplete.util.ktx.toARGBString

/** Takes care of displaying styled map data */
class StyleableOverlayMapComponent(private val resources: Resources, private val map: MapboxMap) {

    private val overlaySource = GeoJsonSource("overlay-source")

    private val darkenedColors = HashMap<String, String>()
    private val transparentColors = HashMap<String, String>()

    /** Shows/hides the map data */
    var isVisible: Boolean
        // add / remove source
        @UiThread get() = map.style?.sources?.find { it.id == "overlay-source" } != null
        @UiThread set(value) {
            if (isVisible == value) return
            if (value) {
                map.style?.addSource(overlaySource)
            } else {
                map.style?.removeSource(overlaySource)
            }
        }

    init {
        map.style?.addSource(overlaySource)
    }

    /** Show given map data with each the given style */
    @UiThread fun set(features: Collection<StyledElement>) {
        // todo: color.invisible should reproduce original style?
        //  then do after actual style is decided
        val mapLibreFeatures = features.flatMap { (element, geometry, style) ->
            val p = JsonObject()
            p.addProperty(ELEMENT_ID, element.id.toString()) // try avoiding the string?
            p.addProperty(ELEMENT_TYPE, element.type.name)

            when (style) {
                is PointStyle -> {
                    // there is no other style, so we always need a symbol and not a circle
                    if (style.icon != null)
                        p.addProperty("icon", style.icon)
                    if (style.label != null)
                        p.addProperty("label", style.label) // offset and stuff is set for all text in layer
                    listOf(Feature.fromGeometry(geometry.center.toPoint(), p))
                }
                is PolygonStyle -> {
                    if (style.color != de.westnordost.streetcomplete.overlays.Color.INVISIBLE) {
                        p.addProperty("color", style.color)
                        p.addProperty("outline-color", getDarkenedColor(style.color))
                    } else
                        p.addProperty("opacity", 0f)
                    if (getHeight(element.tags) != null)
                        p.addProperty("height", getHeight(element.tags))

                    val f = Feature.fromGeometry(geometry.toMapLibreGeometry(), p)
                    val label = if (style.label != null) {
                        Feature.fromGeometry(
                            geometry.center.toPoint(),
                            JsonObject().apply { addProperty("label", style.label) }
                        )
                    } else null
                    listOfNotNull(f, label)
                }
                is PolylineStyle -> {
                    // there is no strokeColor for lines, so appearance is different and thinner due to missing "line-outline"
                    val line = geometry.toMapLibreGeometry()
                    val width = getLineWidth(element.tags)
                    val left = if (style.strokeLeft != null) {
                        val p2 = p.deepCopy()
                        p2.addProperty("width", 4f)
                        p2.addProperty("offset", -width - 2f)
                        if (style.strokeLeft.color != de.westnordost.streetcomplete.overlays.Color.INVISIBLE)
                            p2.addProperty("color", style.strokeLeft.color)
                        if (style.strokeLeft.dashed)
                            p2.addProperty("dashed", true)
                        Feature.fromGeometry(line, p2)
                    } else null
                    val right = if (style.strokeRight != null) {
                        val p2 = p.deepCopy()
                        p2.addProperty("width", 4f)
                        p2.addProperty("offset", width + 2f)
                        if (style.strokeRight.color != de.westnordost.streetcomplete.overlays.Color.INVISIBLE)
                            p2.addProperty("color", style.strokeRight.color)
                        if (style.strokeRight.dashed)
                            p2.addProperty("dashed", true)
                        Feature.fromGeometry(line, p2)
                    } else null
                    val center = if (style.stroke != null && style.stroke.color != de.westnordost.streetcomplete.overlays.Color.INVISIBLE) {
                        val p2 = p.deepCopy()
                        p2.addProperty("width", width)
                        p2.addProperty("color", style.stroke.color)
                        if (style.stroke.dashed)
                            p2.addProperty("dashed", true)
                        Feature.fromGeometry(line, p2)
                    } else null
                    val label = if (style.label != null) {
                        Feature.fromGeometry(
                            geometry.center.toPoint(),
                            JsonObject().apply { addProperty("label", style.label) }
                        )
                    } else null
                    listOfNotNull(left, right, center, label)
                }
            }
        }
        overlaySource.setGeoJson(FeatureCollection.fromFeatures(mapLibreFeatures))
    }

    /** mimics width of line as seen in StreetComplete map style (or otherwise 3m) */
    private fun getLineWidth(tags: Map<String, String>): Float = when (tags["highway"]) {
        "motorway" -> if (!isOneway(tags)) 15f else 7.5f
        "motorway_link" -> 4.5f
        "trunk", "primary", "secondary", "tertiary" -> if (!isOneway(tags)) 7.5f else 4.5f
        "service", "track" -> 3f
        "path", "cycleway", "footway", "bridleway", "steps" -> 1f
        null -> 3f
        else -> if (!isOneway(tags)) 5.5f else 3f
    }

    /** estimates height of thing, currently not used */
    private fun getHeight(tags: Map<String, String>): Float? {
        val height = tags["height"]?.toFloatOrNull()
        if (height != null) return height
        val buildingLevels = tags["building:levels"]?.toFloatOrNull()
        val roofLevels = tags["roof:levels"]?.toFloatOrNull()
        if (buildingLevels != null) return 3f * (buildingLevels + (roofLevels ?: 0f))
        return null
    }

    // no need to parse, modify and write to string darkening the same colors for every single element
    private fun getDarkenedColor(color: String): String =
        darkenedColors.getOrPut(color) { toARGBString(darken(Color.parseColor(color), 0.67f)) }

    private fun getColorWithSomeTransparency(color: String): String =
        // alpha is actually double of what is specified https://github.com/tangrams/tangram-es/issues/2333
        transparentColors.getOrPut(color) { toARGBString(addTransparency(Color.parseColor(color), 0.6f)) }

    /** Clear map data */
    @UiThread fun clear() {
        overlaySource.clear()
    }

    fun getElementKey(properties: Map<String, String>): ElementKey? {
        val type = properties[ELEMENT_TYPE]?.let { ElementType.valueOf(it) } ?: return null
        val id = properties[ELEMENT_ID]?.toLong() ?: return null
        return ElementKey(type, id)
    }
}

private const val ELEMENT_TYPE = "element_type"
private const val ELEMENT_ID = "element_id"

data class StyledElement(
    val element: Element,
    val geometry: ElementGeometry,
    val style: Style
) {
    // geometries may contain road color, which depends on current theme
    // however, storing is not an issue as styled elements are cleared on theme switch (both automatic and manual)
//    var tangramGeometries: List<Geometry>? = null
}

fun JsonElement.toElementKey(): ElementKey? {
    // todo: what are the values if it doesn't exist? empty strings?
    val id = asJsonObject.getAsJsonPrimitive(ELEMENT_ID)?.asString?.toLongOrNull() ?: return null
    val type = asJsonObject.getAsJsonPrimitive(ELEMENT_TYPE).asString
    return if (type in ElementType.entries.map { it.toString() })
        ElementKey(ElementType.valueOf(type), id)
    else null
}
