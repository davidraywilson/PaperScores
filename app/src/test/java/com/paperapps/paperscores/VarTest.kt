package com.paperapps.paperscores

import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.File

class VarTest {
    @Test
    fun fetchMatches() = runBlocking {
        val client = com.paperapps.paperscores.network.FotMobApiClient()
        val data = client.getMatchesByDate("20260623")
        File("/tmp/fotmob_data.txt").writeText(data ?: "null")
    }
}
