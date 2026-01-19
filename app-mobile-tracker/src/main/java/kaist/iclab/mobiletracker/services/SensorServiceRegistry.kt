package kaist.iclab.mobiletracker.services

import kaist.iclab.mobiletracker.services.supabase.BaseSupabaseService

/**
 * Registry for managing Supabase sensor services.
 * Reduces dependency injection complexity by providing a single point of access.
 */
interface SensorServiceRegistry {
    /**
     * Get a Supabase service for a given sensor ID.
     *
     * @param sensorId The sensor ID
     * @return The Supabase service, or null if not found
     */
    fun getService(sensorId: String): BaseSupabaseService<*>?

    /**
     * Check if a service exists for the given sensor ID.
     */
    fun hasService(sensorId: String): Boolean
}

/**
 * Implementation of SensorServiceRegistry using a Map.
 */
class SensorServiceRegistryImpl(
    private val services: Map<String, BaseSupabaseService<*>>
) : SensorServiceRegistry {

    override fun getService(sensorId: String): BaseSupabaseService<*>? {
        return services[sensorId]
    }

    override fun hasService(sensorId: String): Boolean {
        return services.containsKey(sensorId)
    }
}

