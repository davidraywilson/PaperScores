package com.paperapps.paperscores

import org.junit.Test
import kotlinx.coroutines.runBlocking
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File

class FotMobApiClientTest {
    @Test
    fun testGetLeagueTable() = runBlocking {
        val client = HttpClient(Android)
        try {
            val res1 = client.get("https://www.fotmob.com/api/tltable?leagueId=47")
            File("test_tltable.json").writeText(res1.bodyAsText())
        } catch (e: Exception) { println(e) }
        try {
            val res2 = client.get("https://www.fotmob.com/api/leagues?id=47")
            File("test_leagues.json").writeText(res2.bodyAsText())
        } catch (e: Exception) { println(e) }
    }
}
