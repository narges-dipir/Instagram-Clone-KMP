package de.app.instagram.profile.data.repository

import de.app.instagram.db.RemoteContentCache
import de.app.instagram.profile.data.remote.ProfileApi
import de.app.instagram.profile.data.remote.ProfileDto
import de.app.instagram.profile.domain.model.Profile
import de.app.instagram.profile.domain.model.PostMediaType
import de.app.instagram.profile.domain.model.ProfilePost
import de.app.instagram.profile.domain.model.ProfileStats
import de.app.instagram.profile.domain.model.StoryHighlight
import de.app.instagram.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class DefaultProfileRepository(
    private val profileApi: ProfileApi,
    private val remoteContentCache: RemoteContentCache,
    private val json: Json,
) : ProfileRepository {
    override suspend fun getProfile(): Profile {
        val cacheKey = cacheKey()
        val networkFailure = runCatching {
            val remoteDto = profileApi.getProfile()
            remoteContentCache.write(cacheKey, json.encodeToString(ProfileDto.serializer(), remoteDto))
        }.exceptionOrNull()

        val cachedDto = remoteContentCache.observe(cacheKey).first()?.let { payload ->
            json.decodeFromString(ProfileDto.serializer(), payload)
        }
        val dto = cachedDto ?: throw (networkFailure ?: IllegalStateException("Profile cache is empty"))
        return Profile(
            id = dto.id,
            username = dto.username,
            fullName = dto.fullName,
            bio = dto.bio,
            isVerified = dto.isVerified,
            avatarUrl = dto.avatarUrl,
            stats = ProfileStats(
                posts = dto.stats.posts,
                followers = dto.stats.followers,
                following = dto.stats.following,
            ),
            website = dto.website,
            storyHighlights = dto.storyHighlights.map {
                val fallbackMediaUrls = FALLBACK_HIGHLIGHT_MEDIA[it.id].orEmpty()
                val resolvedMediaUrls = if (it.mediaUrls.isNotEmpty()) it.mediaUrls else fallbackMediaUrls
                StoryHighlight(
                    id = it.id,
                    title = it.title,
                    coverUrl = it.coverUrl,
                    mediaUrls = resolvedMediaUrls,
                )
            },
            posts = dto.posts.map {
                ProfilePost(
                    id = it.id,
                    imageUrl = it.imageUrl,
                    mediaType = if (it.mediaType.equals("video", ignoreCase = true)) {
                        PostMediaType.VIDEO
                    } else {
                        PostMediaType.IMAGE
                    },
                    videoUrl = it.videoUrl,
                    likes = it.likes,
                    comments = it.comments,
                )
            },
        )
    }

    private fun cacheKey(): String = "content_cache.profile"

    private companion object {
        val FALLBACK_HIGHLIGHT_MEDIA = mapOf(
            "h_001" to listOf(
                "https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=900&q=80",
                "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=900&q=80",
                "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=900&q=80",
            ),
            "h_002" to listOf(
                "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=900&q=80",
                "https://images.unsplash.com/photo-1521791136064-7986c2920216?auto=format&fit=crop&w=900&q=80",
                "https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=900&q=80",
            ),
            "h_003" to listOf(
                "https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&w=900&q=80",
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80",
                "https://images.unsplash.com/photo-1482049016688-2d3e1b311543?auto=format&fit=crop&w=900&q=80",
            ),
        )
    }
}
