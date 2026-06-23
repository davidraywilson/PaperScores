import java.net.URL
import java.net.HttpURLConnection

fun fetchMatch(matchId: String) {
    val url = URL("https://www.fotmob.com/match/$matchId")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
    val text = connection.inputStream.bufferedReader().use { it.readText() }
    
    val regex = """"buildId":"([^"]+)"""".toRegex()
    val match = regex.find(text)
    if (match != null) {
        println("buildId: " + match.groupValues[1])
    } else {
        println("Not found")
    }
}
fetchMatch("5225665")
