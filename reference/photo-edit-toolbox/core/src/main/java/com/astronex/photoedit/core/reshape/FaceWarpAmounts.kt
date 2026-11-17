package com.astronex.photoedit.core.reshape

/**
 * How much each face reshape should change a face, all at once, for [FaceWarps]. Each value runs
 * from -1 through 0 (unchanged, the default) to 1, the same way round as Meitu's slider of the same
 * name (measured on the Galaxy A13):
 * @param width Face > Width: positive narrower, negative wider ([FaceWidthWarp]).
 * @param lift Face > Lift: positive raises and tightens the jaw, negative drops it ([FaceLiftWarp]).
 * @param temple Face > Temple: positive fuller temples, negative hollower ([TempleWarp]).
 * @param cheekbones Face > Cheekbones: positive slimmer across the cheekbones, negative wider
 *   ([CheekboneWarp]).
 * @param chinLength Face > Chin length: positive a shorter chin, negative longer ([ChinWarp]).
 * @param hairline Face > Hairline: positive a lower hairline, negative higher ([HairlineWarp]).
 * @param eyeSize Eyes > Size: positive bigger eyes, negative smaller ([EyeSizeWarp]).
 * @param noseSize Nose > Size: positive a narrower nose, negative wider ([NoseSizeWarp]).
 * @param lipSize Lips > Size: positive smaller lips, negative bigger ([LipSizeWarp]).
 * @param headSize Head size: positive a smaller head, negative bigger ([HeadSizeWarp]).
 * @param jawline Face > Jawline: positive a narrower jaw, negative wider ([JawlineWarp]).
 * @param chinShape Face > Chin shape: positive a narrower chin, negative wider ([ChinShapeWarp]).
 * @param smooth Face > Smooth: positive a more even outline, negative the opposite ([FaceSmoothWarp]).
 * @param chinTip Face > Chin tip: positive a more pointed chin, negative rounder ([ChinTipWarp]).
 * @param jawAngle Face > Jaw angle: positive the jaw's corners raised and drawn in, negative squarer
 *   ([JawAngleWarp]).
 * @param doubleChin Face > Double chin: positive the jaw's lower edge lifted, negative lowered
 *   ([DoubleChinWarp]).
 *
 * It is one object, not loose numbers, because the face tools keep growing and many numbers of the
 * same type in a row are easy to swap by mistake; each is named here.
 */
internal data class FaceWarpAmounts(
    val width: Float = 0f,
    val lift: Float = 0f,
    val temple: Float = 0f,
    val cheekbones: Float = 0f,
    val chinLength: Float = 0f,
    val hairline: Float = 0f,
    val eyeSize: Float = 0f,
    val noseSize: Float = 0f,
    val lipSize: Float = 0f,
    val headSize: Float = 0f,
    val jawline: Float = 0f,
    val chinShape: Float = 0f,
    val smooth: Float = 0f,
    val chinTip: Float = 0f,
    val jawAngle: Float = 0f,
    val doubleChin: Float = 0f,
) {

    /** True when every value is 0 (or NaN): the face is left as it is and no landmarks are needed. */
    val isNone: Boolean
        get() = listOf(width, lift, temple, cheekbones, chinLength, hairline, eyeSize, noseSize, lipSize, headSize, jawline, chinShape, smooth, chinTip, jawAngle, doubleChin)
            .all(predicate = { it == 0f || it.isNaN() })
}
