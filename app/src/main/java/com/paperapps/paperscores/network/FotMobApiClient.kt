package com.paperapps.paperscores.network

import com.paperapps.paperscores.network.models.LeagueScoreboard
import com.paperapps.paperscores.network.models.MatchDetails
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import io.ktor.client.statement.bodyAsText
import com.paperapps.paperscores.network.models.Team
import com.paperapps.paperscores.network.models.MatchEvent
import com.paperapps.paperscores.network.models.MatchStat
import com.paperapps.paperscores.network.models.LineupPlayer
import com.paperapps.paperscores.network.models.TeamLineup
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.JsonElement
import com.paperapps.paperscores.network.models.TeamNextMatch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar
val JsonElement.jsonObjectOrNull: JsonObject? get() = if (this is JsonObject) this else null
val JsonElement.jsonArrayOrNull: kotlinx.serialization.json.JsonArray? get() = if (this is kotlinx.serialization.json.JsonArray) this else null
class FotMobApiClient {
    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long
    )
    
    private val matchesByDateCache = mutableMapOf<String, CacheEntry<String>>()
    private val matchDetailsCache = mutableMapOf<String, CacheEntry<MatchDetails>>()

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val baseUrl = "https://www.fotmob.com/api"

    suspend fun getMatchDetails(matchId: String, forceRefresh: Boolean = false): MatchDetails? {
        if (!forceRefresh) {
            val cached = matchDetailsCache[matchId]
            if (cached != null) {
                val now = System.currentTimeMillis()
                val age = now - cached.timestamp
                val status = cached.data.status
                val isFinished = status == "Finished" || status == "FT" || status == "AET" || status == "Pen"
                val isUpcoming = status == "Upcoming"
                if (isFinished && age < 24 * 60 * 60 * 1000L) {
                    return cached.data
                } else if (isUpcoming && age < 5 * 60 * 1000L) {
                    return cached.data
                } else if (!isFinished && !isUpcoming && age < 60 * 1000L) {
                    return cached.data
                }
            }
        }
        return try {
            val response = client.get("https://www.fotmob.com/match/$matchId?_=${System.currentTimeMillis()}") {
                header(io.ktor.http.HttpHeaders.CacheControl, "no-cache")
            }
            val text = response.bodyAsText()
            
            val regex = """<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(text)
            if (match != null) {
                val jsonString = match.groupValues[1]
                val jsonObject = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObjectOrNull
                val pageProps = jsonObject?.get("props")?.jsonObjectOrNull?.get("pageProps")?.jsonObjectOrNull
                
                val general = pageProps?.get("general")?.jsonObjectOrNull
                val header = pageProps?.get("header")?.jsonObjectOrNull
                
                if (general != null && header != null) {
                    val homeTeamObj = general["homeTeam"]?.jsonObjectOrNull
                    val awayTeamObj = general["awayTeam"]?.jsonObjectOrNull
                    
                    val homeTeamId = homeTeamObj?.get("id")?.toString() ?: ""
                    val homeTeam = Team(
                        id = homeTeamId,
                        name = homeTeamObj?.get("name")?.toString()?.trim('"') ?: "",
                        imageUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/${homeTeamId}.png"
                    )
                    
                    val awayTeamId = awayTeamObj?.get("id")?.toString() ?: ""
                    val awayTeam = Team(
                        id = awayTeamId,
                        name = awayTeamObj?.get("name")?.toString()?.trim('"') ?: "",
                        imageUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/${awayTeamId}.png"
                    )
                    
                    val statusObj = header["status"]?.jsonObjectOrNull
                    val scoreStr = statusObj?.get("scoreStr")?.toString()?.trim('"') ?: ""
                    val scores = scoreStr.split(" - ")
                    val homeScore = scores.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
                    val awayScore = scores.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                    val score = com.paperapps.paperscores.network.models.Score(homeScore, awayScore)
                    
                    val isFinished = statusObj?.get("finished")?.toString()?.toBoolean() ?: false
                    val isStarted = statusObj?.get("started")?.toString()?.toBoolean() ?: true
                    
                    val liveTimeObj = statusObj?.get("liveTime")?.jsonObjectOrNull
                    val liveTimeShort = liveTimeObj?.get("short")?.toString()?.trim('"')
                    
                    val reasonObj = statusObj?.get("reason")?.jsonObjectOrNull
                    val reasonShort = reasonObj?.get("short")?.toString()?.trim('"')
                    
                    val liveTimeStr = liveTimeShort ?: reasonShort ?: ""
                    
                    val matchTimeUTC = general["matchTimeUTC"]?.toString()?.trim('"') ?: ""
                    
                    val leagueName = general["leagueName"]?.toString()?.trim('"') ?: general["parentLeagueName"]?.toString()?.trim('"') ?: ""
                    val stadiumObj = general["stadium"]?.jsonObjectOrNull
                    val stadiumName = stadiumObj?.get("name")?.toString()?.trim('"') ?: ""
                    
                    val content = pageProps["content"]?.jsonObjectOrNull
                    
                    // Parse Events
                    val eventsList = mutableListOf<MatchEvent>()
                    val matchFacts = content?.get("matchFacts")?.jsonObjectOrNull
                    val eventsObj = matchFacts?.get("events")?.jsonObjectOrNull
                    val eventsArray = eventsObj?.get("events")?.jsonArrayOrNull
                    eventsArray?.forEach { eventEl ->
                        val ev = eventEl.jsonObjectOrNull
                        if (ev != null) {
                            val rawTime = ev["time"]?.toString()?.toIntOrNull() ?: 0
                            val parsedTimeStr = ev["timeStr"]?.toString()?.trim('"')
                            val timeStr = if (!parsedTimeStr.isNullOrBlank() && parsedTimeStr != "null") {
                                parsedTimeStr.replace(" ", "")
                            } else {
                                rawTime.toString()
                            }
                            val type = ev["type"]?.toString()?.trim('"') ?: ""
                            var nameStr = ev["nameStr"]?.toString()?.trim('"') ?: ""

                            if (type == "AddedTime") {
                                val minutesAddedStr = ev["minutesAddedStr"]?.toString()?.trim('"')
                                if (!minutesAddedStr.isNullOrBlank() && minutesAddedStr != "null") {
                                    nameStr = minutesAddedStr
                                }
                            }

                            if (nameStr.isBlank()) {
                                val swapArray = ev["swap"]?.jsonArrayOrNull
                                if (swapArray != null && swapArray.size >= 2) {
                                    val playerIn = swapArray[0].jsonObjectOrNull?.get("name")?.toString()?.trim('"') ?: ""
                                    val playerOut = swapArray[1].jsonObjectOrNull?.get("name")?.toString()?.trim('"') ?: ""
                                    nameStr = "In: $playerIn\nOut: $playerOut"
                                } else {
                                    val playerObj = ev["player"]?.jsonObjectOrNull
                                    val playerName = playerObj?.get("name")?.toString()?.trim('"')
                                    if (!playerName.isNullOrBlank() && playerName != "null") {
                                        nameStr = playerName
                                    }
                                }
                            }

                            val assistStr = ev["assistStr"]?.toString()?.trim('"')
                            if (!assistStr.isNullOrBlank() && assistStr != "null") {
                                nameStr += "\n$assistStr"
                            }

                            val goalDescription = ev["goalDescription"]?.toString()?.trim('"')
                            if (!goalDescription.isNullOrBlank() && goalDescription != "null") {
                                nameStr += "\n$goalDescription"
                            }
                            
                            if (type == "Card") {
                                val card = ev["card"]?.toString()?.trim('"')
                                if (!card.isNullOrBlank() && card != "null") {
                                    nameStr += "\n$card"
                                }
                            }

                            val suffix = ev["suffix"]?.toString()?.trim('"')
                            if (!suffix.isNullOrBlank() && suffix != "null") {
                                nameStr += "\n$suffix"
                            }

                            if (type == "VAR") {
                                val ignoredKeys = setOf("nameStr", "timeStr", "type", "reactKey", "profileUrl", "playerId", "firstName", "lastName", "fullName", "isHome", "eventId", "homeScore", "awayScore", "time", "overloadTime", "newScore", "shotmapEvent")
                                val extraInfo = StringBuilder()
                                ev.entries.forEach { (k, v) ->
                                    if (k !in ignoredKeys) {
                                        val strVal = v.toString().trim('"')
                                        if (strVal != "null" && strVal.isNotBlank()) {
                                            if (strVal.startsWith("{")) {
                                                val obj = v.jsonObjectOrNull
                                                val defaultText = obj?.get("defaultText")?.toString()?.trim('"')
                                                val fallbackText = obj?.get("fallbackText")?.toString()?.trim('"')
                                                val translation = obj?.get("translation")?.toString()?.trim('"')
                                                if (!defaultText.isNullOrBlank() && defaultText != "null") extraInfo.append("\n$defaultText")
                                                else if (!fallbackText.isNullOrBlank() && fallbackText != "null") extraInfo.append("\n$fallbackText")
                                                else if (!translation.isNullOrBlank() && translation != "null") extraInfo.append("\n$translation")
                                            } else if (!strVal.startsWith("[")) {
                                                if (strVal != "false" && strVal != "true" && strVal.toIntOrNull() == null && !strVal.contains("VAR")) {
                                                    extraInfo.append("\n$strVal")
                                                } else if (strVal.contains("VAR", ignoreCase = true) || strVal.contains("Goal", ignoreCase = true) || strVal.contains("Penalty", ignoreCase = true)) {
                                                    extraInfo.append("\n$strVal")
                                                }
                                            }
                                        }
                                    }
                                }
                                if (extraInfo.isNotBlank()) {
                                    nameStr += extraInfo.toString()
                                }
                            }

                            eventsList.add(MatchEvent(timeStr, type, nameStr))
                        }
                    }
                    
                    // Parse Stats
                    val statsList = mutableListOf<MatchStat>()
                    val statsObj = content?.get("stats")?.jsonObjectOrNull
                    val periods = statsObj?.get("Periods")?.jsonObjectOrNull
                    val all = periods?.get("All")?.jsonObjectOrNull
                    val statsArray = all?.get("stats")?.jsonArrayOrNull
                    statsArray?.forEach { statGroupEl ->
                        val statGroup = statGroupEl.jsonObjectOrNull
                        if (statGroup != null) {
                            val groupStats = statGroup["stats"]?.jsonArrayOrNull
                            groupStats?.forEach { statItemEl ->
                                val statItem = statItemEl.jsonObjectOrNull
                                if (statItem != null) {
                                    val title = statItem["title"]?.toString()?.trim('"') ?: ""
                                    val vals = statItem["stats"]?.jsonArrayOrNull
                                    val homeVal = vals?.getOrNull(0)?.toString()?.trim('"') ?: ""
                                    val awayVal = vals?.getOrNull(1)?.toString()?.trim('"') ?: ""
                                    statsList.add(MatchStat(title, homeVal, awayVal))
                                }
                            }
                        }
                    }
                    
                    // Parse Lineups
                    fun parseLineup(teamObj: JsonObject?): TeamLineup? {
                        if (teamObj == null) return null
                        val formation = teamObj["formation"]?.toString()?.trim('"') ?: ""
                        val startersArray = teamObj["starters"]?.jsonArrayOrNull ?: return null
                        val starters = startersArray.mapNotNull {
                            val player = it.jsonObjectOrNull
                            if (player != null) {
                                val name = player["name"]?.toString()?.trim('"') ?: ""
                                val shirt = player["shirtNumber"]?.toString()?.trim('"') ?: ""
                                LineupPlayer(name, shirt)
                            } else null
                        }
                        return TeamLineup(formation, starters)
                    }
                    
                    val lineupObj = content?.get("lineup")?.jsonObjectOrNull
                    val homeLineup = parseLineup(lineupObj?.get("homeTeam")?.jsonObjectOrNull)
                    val awayLineup = parseLineup(lineupObj?.get("awayTeam")?.jsonObjectOrNull)
                    
                    val details = MatchDetails(
                        matchId = matchId,
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        score = if (!isStarted) com.paperapps.paperscores.network.models.Score(null, null) else score,
                        status = if (isFinished) "Finished" else if (!isStarted) "Upcoming" else if (!reasonShort.isNullOrBlank() && reasonShort !in listOf("HT", "FT", "Pen", "AET", "Half-Time", "Full-Time")) reasonShort else "Active",
                        matchTime = matchTimeUTC,
                        liveTime = liveTimeStr,
                        tournamentName = leagueName,
                        stadiumName = stadiumName,
                        events = eventsList,
                        stats = statsList,
                        homeLineup = homeLineup,
                        awayLineup = awayLineup
                    )
                    matchDetailsCache[matchId] = CacheEntry(details, System.currentTimeMillis())
                    return details
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getLeagueScoreboard(leagueId: String): LeagueScoreboard? {
        return try {
            client.get("$baseUrl/leagueScoreboard?leagueId=$leagueId").body()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getMatchesByDate(date: String, forceRefresh: Boolean = false): String? {
        if (!forceRefresh) {
            val cached = matchesByDateCache[date]
            if (cached != null) {
                val format = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                format.timeZone = java.util.TimeZone.getDefault()
                val todayStr = format.format(java.util.Date())
                val age = System.currentTimeMillis() - cached.timestamp
                val dateInt = date.toIntOrNull() ?: 0
                val todayInt = todayStr.toIntOrNull() ?: 0
                if (dateInt < todayInt && age < 24 * 60 * 60 * 1000L) {
                    return cached.data
                } else if (dateInt > todayInt && age < 2 * 60 * 60 * 1000L) {
                    return cached.data
                } else if (dateInt == todayInt && age < 60 * 1000L) {
                    return cached.data
                }
            }
        }
        return try {
            val result: String = client.get("https://apigw.fotmob.com/matches?date=$date&_=${System.currentTimeMillis()}") {
                header(io.ktor.http.HttpHeaders.CacheControl, "no-cache")
            }.body()
            matchesByDateCache[date] = CacheEntry(result, System.currentTimeMillis())
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun searchEntities(query: String): JsonObject? {
        return try {
            client.get("https://apigw.fotmob.com/searchapi/suggest?term=$query&lang=en").body()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getTeamNextMatch(teamId: String): TeamNextMatch? {
        return try {
            val response = client.get("https://www.fotmob.com/teams/$teamId/overview/team") {
                header(io.ktor.http.HttpHeaders.CacheControl, "no-cache")
            }
            val text = response.bodyAsText()
            val regex = """<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(text)
            if (match != null) {
                val jsonString = match.groupValues[1]
                val jsonObject = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObjectOrNull
                val pageProps = jsonObject?.get("props")?.jsonObjectOrNull?.get("pageProps")?.jsonObjectOrNull
                val fallback = pageProps?.get("fallback")?.jsonObjectOrNull
                val teamData = fallback?.get("team-$teamId")?.jsonObjectOrNull
                val fixtures = teamData?.get("fixtures")?.jsonObjectOrNull
                val allFixtures = fixtures?.get("allFixtures")?.jsonObjectOrNull
                val fixturesArray = allFixtures?.get("fixtures")?.jsonArrayOrNull
                
                if (fixturesArray != null) {
                    val nextMatchObj = fixturesArray.firstOrNull { 
                        val notStarted = it.jsonObjectOrNull?.get("notStarted")?.jsonPrimitive?.booleanOrNull
                        notStarted == true
                    }?.jsonObjectOrNull
                    
                    if (nextMatchObj != null) {
                        val opponentObj = nextMatchObj["opponent"]?.jsonObjectOrNull
                        val opponentName = opponentObj?.get("name")?.toString()?.trim('"') ?: ""
                        
                        val homeObj = nextMatchObj["home"]?.jsonObjectOrNull
                        val homeId = homeObj?.get("id")?.toString() ?: ""
                        val isHome = homeId == teamId
                        
                        val statusObj = nextMatchObj["status"]?.jsonObjectOrNull
                        val utcTime = statusObj?.get("utcTime")?.toString()?.trim('"')
                        
                        var matchDateStr = ""
                        var matchTimeStr = ""
                        
                        if (utcTime != null) {
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                                val date = sdf.parse(utcTime)
                                if (date != null) {
                                    val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
                                    dateFmt.timeZone = TimeZone.getDefault()
                                    val timeFmt = SimpleDateFormat("h:mm a", Locale.US)
                                    timeFmt.timeZone = TimeZone.getDefault()
                                    
                                    val calMatch = Calendar.getInstance()
                                    calMatch.time = date
                                    val calToday = Calendar.getInstance()
                                    
                                    if (calMatch.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                                        calMatch.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)) {
                                        matchDateStr = "Today"
                                    } else {
                                        matchDateStr = dateFmt.format(date)
                                    }
                                    matchTimeStr = timeFmt.format(date)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        return TeamNextMatch(
                            opponentName = opponentName,
                            isHome = isHome,
                            matchDate = matchDateStr,
                            matchTime = matchTimeStr
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
