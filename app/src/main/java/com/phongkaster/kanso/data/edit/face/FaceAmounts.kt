package com.phongkaster.kanso.data.edit.face

/**
 * How much each face reshape should change a face, all at once, for [FaceShape]. Each value runs
 * from -1 through 0 (unchanged, the default) to 1, the same way round as Meitu's slider of the same
 * name (measured on the Galaxy A13):
 * @param width Face > Width: positive narrower, negative wider ([FaceWidth]).
 * @param lift Face > Lift: positive raises and tightens the jaw, negative drops it ([FaceLift]).
 * @param temple Face > Temple: positive fuller temples, negative hollower ([FaceTemple]).
 * @param cheekbones Face > Cheekbones: positive slimmer across the cheekbones, negative wider
 *   ([FaceCheekbones]).
 * @param chinLength Face > Chin length: positive a shorter chin, negative longer ([FaceChinLength]).
 * @param hairline Face > Hairline: positive a lower hairline, negative higher ([FaceHairline]).
 *
 * It is one object, not loose numbers, because the face tools keep growing and many numbers of the
 * same type in a row are easy to swap by mistake; each is named here.
 */
data class FaceAmounts(
    val width: Float = 0f,
    val lift: Float = 0f,
    val temple: Float = 0f,
    val cheekbones: Float = 0f,
    val chinLength: Float = 0f,
    val hairline: Float = 0f,
) {

    /** True when every value is 0 (or NaN): the face is left as it is and no landmarks are needed. */
    val isNone: Boolean
        get() = listOf(width, lift, temple, cheekbones, chinLength, hairline).all(predicate = { it == 0f || it.isNaN() })
}
