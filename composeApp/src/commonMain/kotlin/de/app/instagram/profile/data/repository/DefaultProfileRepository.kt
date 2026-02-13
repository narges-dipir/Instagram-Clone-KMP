package de.app.instagram.profile.data.repository

import de.app.instagram.profile.data.remote.ProfileApi
import de.app.instagram.profile.domain.model.Profile
import de.app.instagram.profile.domain.model.PostMediaType
import de.app.instagram.profile.domain.model.ProfilePost
import de.app.instagram.profile.domain.model.ProfileStats
import de.app.instagram.profile.domain.model.StoryHighlight
import de.app.instagram.profile.domain.repository.ProfileRepository

class DefaultProfileRepository(
    private val profileApi: ProfileApi,
) : ProfileRepository {
    override suspend fun getProfile(): Profile {
        val dto = profileApi.getProfile()
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
                StoryHighlight(
                    id = it.id,
                    title = it.title,
                    coverUrl = it.coverUrl,
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
}
