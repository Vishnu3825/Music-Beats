package com.example.musicplayer.data.remote

import com.example.musicplayer.data.model.JamendoResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Jamendo is a free/open-source music platform with a free API.
 * Register at https://developer.jamendo.com to get a client_id.
 * The free tier allows streaming and search.
 */
interface JamendoApiService {

    @GET("tracks/")
    suspend fun searchTracks(
        @Query("client_id") clientId: String = JAMENDO_CLIENT_ID,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 30,
        @Query("search") query: String,
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("include") include: String = "musicinfo",
        @Query("order") order: String = "popularity_total"
    ): JamendoApiResponse

    @GET("tracks/")
    suspend fun getFeaturedTracks(
        @Query("client_id") clientId: String = JAMENDO_CLIENT_ID,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("order") order: String = "popularity_total",
        @Query("tags") tags: String = "pop"
    ): JamendoApiResponse

    @GET("tracks/")
    suspend fun getTracksByGenre(
        @Query("client_id") clientId: String = JAMENDO_CLIENT_ID,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("tags") genre: String,
        @Query("order") order: String = "popularity_total"
    ): JamendoApiResponse

    companion object {
        const val BASE_URL = "https://api.jamendo.com/v3.0/"
        // Replace with your actual Jamendo client ID from https://developer.jamendo.com
        const val JAMENDO_CLIENT_ID = "YOUR_JAMENDO_CLIENT_ID"
    }
}

data class JamendoApiResponse(
    val headers: JamendoHeaders,
    val results: List<JamendoTrackDto>
)

data class JamendoHeaders(
    val status: String,
    val code: Int,
    val error_message: String?,
    val results_count: Int
)

data class JamendoTrackDto(
    val id: String,
    val name: String,
    val duration: Int,
    val artist_id: String,
    val artist_name: String,
    val artist_idstr: String,
    val album_name: String,
    val album_id: String,
    val album_image: String?,
    val audio: String,
    val audiodownload: String?,
    val image: String,
    val musicinfo: JamendoMusicInfo?
)

data class JamendoMusicInfo(
    val vocalinstrumental: String?,
    val lang: String?,
    val gender: String?,
    val speed: String?,
    val tags: JamendoTags?
)

data class JamendoTags(
    val genres: List<String>?,
    val instruments: List<String>?,
    val vartags: List<String>?
)
