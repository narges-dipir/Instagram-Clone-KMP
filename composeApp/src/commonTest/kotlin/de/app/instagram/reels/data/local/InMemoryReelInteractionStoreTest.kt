package de.app.instagram.reels.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class InMemoryReelInteractionStoreTest {

    @Test
    fun save_andReadAll_roundTripsInteraction() = runTest {
        val store = InMemoryReelInteractionStore()

        store.save(
            reelId = "r_001",
            interaction = LocalReelInteraction(
                isLikedByMe = true,
                isSavedByMe = true,
                isFollowingCreator = true,
                comments = listOf("nice", "great"),
                localShares = 2,
            ),
        )

        val all = store.readAll()
        val interaction = all.getValue("r_001")

        assertEquals(true, interaction.isLikedByMe)
        assertEquals(true, interaction.isSavedByMe)
        assertEquals(true, interaction.isFollowingCreator)
        assertEquals(listOf("nice", "great"), interaction.comments)
        assertEquals(2, interaction.localShares)
    }
}
