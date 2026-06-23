package com.paperapps.paperscores

import org.junit.Test
import kotlinx.coroutines.runBlocking

class FotMobApiClientTest {
    @Test
    fun testGetMatchDetails() = runBlocking {
        val client = com.paperapps.paperscores.network.FotMobApiClient()
        val details = client.getMatchDetails("4667806")
        if (details != null) {
            println("Got Details! Match: ${details.homeTeam.name} vs ${details.awayTeam.name}")
        } else {
            println("Details is null!")
        }
    }
}
