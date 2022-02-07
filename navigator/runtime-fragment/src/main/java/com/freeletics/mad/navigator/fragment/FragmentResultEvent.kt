package com.freeletics.mad.navigator.fragment

import android.os.Parcelable
import com.freeletics.mad.navigator.NavEvent
import com.freeletics.mad.navigator.NavEventNavigator

/**
 * Delivers a `Fragment` result to a requester that started this `Fragment` with
 * [NavEventNavigator.navigateForResult].
 */
public data class FragmentResultEvent(
    internal val requestKey: String,
    internal val result: Parcelable
)
