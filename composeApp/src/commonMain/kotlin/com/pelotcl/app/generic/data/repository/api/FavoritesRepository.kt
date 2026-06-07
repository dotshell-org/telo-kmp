package com.pelotcl.app.generic.data.repository.api

import com.pelotcl.app.generic.data.models.stops.Favorite

interface FavoritesRepository {
    fun getFavoriteStops(): Set<String>
    fun getUserFavorites(): List<Favorite>
    fun generateFavoriteId(): String
    fun addFavorite(favorite: Favorite): Boolean
    fun removeFavorite(favoriteId: String): Boolean
    fun toggleFavoriteStop(stopName: String, desserte: String? = null): Boolean
}
