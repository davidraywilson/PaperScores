import java.net.URL
import java.util.Scanner

fun fetch() {
    try {
        val url = URL("https://www.fotmob.com/api/matchDetails?matchId=4667807")
        val scanner = Scanner(url.openStream(), "UTF-8").useDelimiter("\\A")
        if (scanner.hasNext()) {
            val res = scanner.next()
            println("Success: " + res.substring(0, 50))
        }
    } catch (e: Exception) {
        println("Error: " + e.message)
    }
}
fetch()
