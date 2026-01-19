package kaist.iclab.mobiletracker.db.mapper

/**
 * Mapper interface for converting Room entities to Supabase data format.
 *
 * @param TEntity The Room entity type
 * @param TSupabase The Supabase data type
 */
interface EntityToSupabaseMapper<TEntity, TSupabase> {
    /**
     * Convert a Room entity to Supabase data format.
     *
     * @param entity The Room entity to convert
     * @param userUuid The user UUID from Supabase session (can be null)
     * @return The Supabase data object
     */
    fun map(entity: TEntity, userUuid: String?): TSupabase
}

