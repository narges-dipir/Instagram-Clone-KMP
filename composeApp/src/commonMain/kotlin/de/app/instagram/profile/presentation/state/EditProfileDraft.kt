package de.app.instagram.profile.presentation.state

import de.app.instagram.profile.domain.model.Profile

data class EditProfileDraft(
    val username: String,
    val fullName: String,
    val bio: String,
    val website: String,
) {
    companion object {
        fun fromProfile(profile: Profile): EditProfileDraft = EditProfileDraft(
            username = profile.username,
            fullName = profile.fullName,
            bio = profile.bio,
            website = profile.website,
        )
    }
}
