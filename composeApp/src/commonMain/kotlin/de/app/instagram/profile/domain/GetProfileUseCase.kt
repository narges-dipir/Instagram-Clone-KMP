package de.app.instagram.profile.domain

class GetProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): Profile = profileRepository.getProfile()
}
