package com.freeletics.mad.whetstone.internal

import android.content.Context
import com.freeletics.mad.navigator.internal.NavigationExecutor
import kotlin.reflect.KClass

/**
 * Looks up [T] by searching for a fitting [NavEntryComponentGetter] or finding the [scope] in
 * [Context.getSystemService].
 */
@InternalWhetstoneApi
public fun <T : Any> Context.findComponentByScope(
    scope: KClass<*>,
    destinationScope: KClass<*>,
    executor: NavigationExecutor,
): T {
    if (scope != destinationScope) {
        val destinationComponent = find(destinationScope) as? DestinationComponent
        val getter = destinationComponent?.navEntryComponentGetters?.get(scope.java)
        if (getter != null) {
            @Suppress("UNCHECKED_CAST")
            return getter.retrieve(executor, this) as T
        }
    }
    val dependency = find(scope)
    checkNotNull(dependency) {
        "Could not find scope ${scope.qualifiedName} through getSystemService"
    }
    @Suppress("UNCHECKED_CAST")
    return dependency as T
}
