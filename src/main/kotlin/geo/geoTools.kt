package com.confinitum.common.redis.geo

inline fun geoTools(callback: GeoDistance.Companion.() -> Unit) {
    callback(GeoDistance)
}