import java.net.URL
import java.net.HttpURLConnection

fun fetchLeagueTable(leagueId: String) {
    val url = URL("https://www.fotmob.com/api/tltable?leagueId=" + leagueId)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
    
    if (connection.responseCode == 200) {
        val text = connection.inputStream.bufferedReader().use { it.readText() }
        println(text.take(300))
    } else {
        println("Error: " + connection.responseCode)
    }
}
fetchLeagueTable("47")
