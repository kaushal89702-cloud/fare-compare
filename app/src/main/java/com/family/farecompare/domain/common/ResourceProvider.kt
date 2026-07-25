package com.family.farecompare.domain.common

/**
 * Resolves Android string resources without coupling the presentation layer
 * (ViewModels) directly to an Android [android.content.Context].
 */
interface ResourceProvider {
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg formatArgs: Any): String
}
