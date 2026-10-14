package com.phongkaster.kanso.data.edit.edit

/**
 * The named colour filters of the Edit tool, each built from one look-up image that the photo
 * engine ships.
 *
 * The images are not Kanso's own: they come inside two of the engine's prebuilt libraries,
 * `libs/imageprocessor.aar` (folder `assets/lookup/`) and `libs/editimage.aar` (folder
 * `assets/filter_materials/map/`). When the app is built, the assets of every library are merged
 * into the app's own, so each image is read at run time with `context.assets.open(assetPath)`;
 * nothing is downloaded. How an image becomes a filter (a 3D colour table, or three per-channel
 * curves) is explained on [FilterLut].
 *
 * [assetPath] is the image's path inside the app's assets, or null for [NONE], which leaves the
 * photo as it is.
 */
enum class FilterPreset(val assetPath: String?) {

    /** No filter: the photo is given back unchanged. */
    NONE(null),

    /** A bright, airy look: the engine's 512 x 512 colour table `lookup_light`. */
    LIGHT("lookup/lookup_light.png"),

    /** A faded, rosy late-seventies look, from the curve strip `1977map`. */
    SEVENTY_SEVEN("filter_materials/map/1977map.png"),

    /** A warm, golden look, from the curve strip `kelvinMap`. */
    KELVIN("filter_materials/map/kelvinMap.png"),

    /** A pink-and-navy look, from the curve strip `nashvilleMap`. */
    NASHVILLE("filter_materials/map/nashvilleMap.png"),

    /** A cool, washed-out look, from the curve strip `waldenMap`. */
    WALDEN("filter_materials/map/waldenMap.png"),

    /** A cold, high-key look, from the curve strip `hudsonMap`. */
    HUDSON("filter_materials/map/hudsonMap.png"),

    /** A soft look with lifted shadows, from the curve strip `amaroMap`. */
    AMARO("filter_materials/map/amaroMap.png"),
}
