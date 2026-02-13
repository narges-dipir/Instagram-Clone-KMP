package de.app.instagram.profile.domain.usecase

import de.app.instagram.profile.domain.model.Profile
import de.app.instagram.profile.domain.repository.ProfileRepository

class GetProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): Profile = profileRepository.getProfile()
}
