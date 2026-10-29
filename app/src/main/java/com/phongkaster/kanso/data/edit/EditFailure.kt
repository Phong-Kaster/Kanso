package com.phongkaster.kanso.data.edit

/**
 * Why an edit could not be made. A tool processor reports one of these instead of throwing, so
 * the tool screen can tell the person what happened in plain words while the photo on screen
 * stays exactly as it was.
 *
 * It is a sealed interface: every possible reason is listed here, and a `when` over it must
 * handle each one, so adding a reason later makes the compiler point at every place that has to
 * show it.
 */
sealed interface EditFailure {

    /** The phone ran out of memory while working on the photo. */
    data object OutOfMemory : EditFailure

    /** The tool looked for a subject to cut out (a person, for example) and found none. */
    data object NoSubject : EditFailure

    /** The tool needs a face to work on and found none in the photo. */
    data object NoFace : EditFailure

    /** The tool needs a person's body (a Body reshape such as Waist) and found none in the photo. */
    data object NoPerson : EditFailure

    /** The machine-learning model the tool needs would not load on this phone. */
    data object ModelUnavailable : EditFailure

    /** Something else went wrong that the tool cannot name more precisely. */
    data object Other : EditFailure
}
