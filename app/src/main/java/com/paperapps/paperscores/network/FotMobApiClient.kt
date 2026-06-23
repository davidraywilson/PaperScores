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

val JsonElement.jsonObjectOrNull: JsonObject? get() = if (this is JsonObject) this else null
val JsonElement.jsonArrayOrNull: kotlinx.serialization.json.JsonArray? get() = if (this is kotlinx.serialization.json.JsonArray) this else null
class FotMobApiClient {
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

    suspend fun getMatchDetails(matchId: String): MatchDetails? {
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
                            val timeStr = ev["timeStr"]?.toString()?.toIntOrNull() ?: 0
                            val type = ev["type"]?.toString()?.trim('"') ?: ""
                            val nameStr = ev["nameStr"]?.toString()?.trim('"') ?: ""
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
                    
                    return MatchDetails(
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

    suspend fun getMatchesByDate(date: String): String? {
        return try {
            client.get("https://apigw.fotmob.com/matches?date=$date&_=${System.currentTimeMillis()}") {
                header(io.ktor.http.HttpHeaders.CacheControl, "no-cache")
            }.body()
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
}
