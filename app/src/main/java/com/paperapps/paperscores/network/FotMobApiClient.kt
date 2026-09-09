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
                    val liveTimeStr = when (reasonShort) {
                        "HT", "Half-Time" -> "HT"
                        "Postp", "Postponed", "Del", "Delayed", "Dly", "Canc", "Cancelled", "Int", "Interrupted" -> ""
                        else -> liveTimeShort ?: reasonShort ?: ""
                    }
                    
                    val matchTimeUTC = general["matchTimeUTC"]?.toString()?.trim('"') ?: ""
                    
                    val leagueName = general["leagueName"]?.toString()?.trim('"') ?: general["parentLeagueName"]?.toString()?.trim('"') ?: ""
                    val leagueIdStr = general["parentLeagueId"]?.toString()?.trim('"') ?: general["leagueId"]?.toString()?.trim('"')
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
                    
                    val tableObj = content?.get("table")?.jsonObjectOrNull
                    val tableUrl = tableObj?.get("url")?.toString()?.trim('"')?.takeIf { it != "null" && it.isNotBlank() }

                    val seoObj = pageProps?.get("seo")?.jsonObjectOrNull
                    val broadcastEvent = seoObj?.get("eventJSONLD")?.jsonObjectOrNull?.get("broadcastEvent")?.jsonObjectOrNull
                    val publishedOn = broadcastEvent?.get("publishedOn")?.jsonArrayOrNull
                    val tvNetworks = publishedOn?.mapNotNull { el ->
                        val obj = el.jsonObjectOrNull ?: return@mapNotNull null
                        val country = obj["areaServed"]?.jsonObjectOrNull?.get("name")?.jsonPrimitive?.content
                        if (country == "USA") {
                            obj["name"]?.jsonPrimitive?.content
                        } else {
                            null
                        }
                    }?.distinct() ?: emptyList()

                    val details = MatchDetails(
                        matchId = matchId,
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        score = if (!isStarted) com.paperapps.paperscores.network.models.Score(null, null) else score,
                        status = if (isFinished) "Finished" else if (!isStarted) "Upcoming" else if (!reasonShort.isNullOrBlank() && reasonShort !in listOf("HT", "FT", "Pen", "AET", "Half-Time", "Full-Time")) reasonShort else "Active",
                        matchTime = matchTimeUTC,
                        liveTime = liveTimeStr,
                        tvNetworks = tvNetworks,
                        tournamentName = leagueName,
                        stadiumName = stadiumName,
                        events = eventsList,
                        stats = statsList,
                        homeLineup = homeLineup,
                        awayLineup = awayLineup,
                        tableUrl = tableUrl,
                        leagueId = leagueIdStr
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

    suspend fun getTeamDetails(teamId: String): com.paperapps.paperscores.network.models.TeamDetails? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = client.get("https://www.fotmob.com/teams/$teamId") {
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
                    val teamData = fallback?.get("team-$teamId")?.jsonObjectOrNull ?: return@withContext null

                    val detailsObj = teamData["details"]?.jsonObjectOrNull
                    val name = detailsObj?.get("name")?.toString()?.trim('"') ?: ""
                    val country = detailsObj?.get("country")?.toString()?.trim('"') ?: ""
                    val primaryLeagueName = detailsObj?.get("primaryLeagueName")?.toString()?.trim('"') ?: ""

                    val fixturesObj = teamData["fixtures"]?.jsonObjectOrNull
                    val allFixturesObj = fixturesObj?.get("allFixtures")?.jsonObjectOrNull
                    val fixturesArray = allFixturesObj?.get("fixtures")?.jsonArrayOrNull ?: kotlinx.serialization.json.JsonArray(emptyList())

                    val fixturesList = mutableListOf<MatchDetails>()
                    fixturesArray.forEach { fixtureEl ->
                        val fixtureObj = fixtureEl.jsonObjectOrNull
                        if (fixtureObj != null) {
                            val matchId = fixtureObj["id"]?.toString() ?: ""
                            val homeObj = fixtureObj["home"]?.jsonObjectOrNull
                            val homeTeam = Team(
                                id = homeObj?.get("id")?.toString() ?: "",
                                name = homeObj?.get("name")?.toString()?.trim('"') ?: "",
                                imageUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/${homeObj?.get("id")?.toString()}.png"
                            )
                            val awayObj = fixtureObj["away"]?.jsonObjectOrNull
                            val awayTeam = Team(
                                id = awayObj?.get("id")?.toString() ?: "",
                                name = awayObj?.get("name")?.toString()?.trim('"') ?: "",
                                imageUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/${awayObj?.get("id")?.toString()}.png"
                            )

                            val statusObj = fixtureObj["status"]?.jsonObjectOrNull
                            val scoreStr = statusObj?.get("scoreStr")?.toString()?.trim('"') ?: ""
                            val isFinished = statusObj?.get("finished")?.toString()?.toBoolean() ?: false
                            val isStarted = statusObj?.get("started")?.toString()?.toBoolean() ?: false
                            val utcTime = statusObj?.get("utcTime")?.toString()?.trim('"') ?: ""
                            val reasonShort = statusObj?.get("reason")?.jsonObjectOrNull?.get("short")?.toString()?.trim('"')
                            
                            val scores = scoreStr.split(" - ")
                            val homeScore = scores.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
                            val awayScore = scores.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                            val score = com.paperapps.paperscores.network.models.Score(homeScore, awayScore)

                            val tournamentObj = fixtureObj["tournament"]?.jsonObjectOrNull
                            val tournamentName = tournamentObj?.get("name")?.toString()?.trim('"') ?: ""
                            val tournamentLeagueId = tournamentObj?.get("leagueId")?.toString()?.trim('"')

                            val matchDetails = MatchDetails(
                                matchId = matchId,
                                homeTeam = homeTeam,
                                awayTeam = awayTeam,
                                score = if (!isStarted) com.paperapps.paperscores.network.models.Score(null, null) else score,
                                status = if (isFinished) "Finished" else if (!isStarted) "Upcoming" else if (!reasonShort.isNullOrBlank() && reasonShort !in listOf("HT", "FT", "Pen", "AET", "Half-Time", "Full-Time")) reasonShort else "Active",
                                matchTime = utcTime,
                                liveTime = reasonShort ?: "",
                                tournamentName = tournamentName,
                                stadiumName = "",
                                events = emptyList(),
                                stats = emptyList(),
                                homeLineup = null,
                                awayLineup = null,
                                tableUrl = null,
                                leagueId = tournamentLeagueId
                            )
                            fixturesList.add(matchDetails)
                        }
                    }
                    
                    val overviewObj = teamData["overview"]?.jsonObjectOrNull
                    val teamFormList = mutableListOf<com.paperapps.paperscores.network.models.TeamForm>()
                    overviewObj?.get("teamForm")?.jsonArrayOrNull?.forEach { formEl ->
                        val formObj = formEl.jsonObjectOrNull
                        if (formObj != null) {
                            teamFormList.add(
                                com.paperapps.paperscores.network.models.TeamForm(
                                    result = formObj["result"]?.toString()?.trim('"') ?: "",
                                    resultString = formObj["resultString"]?.toString()?.trim('"') ?: "",
                                    score = formObj["score"]?.toString()?.trim('"') ?: "",
                                    tooltipText = formObj["tooltipText"]?.toString()?.trim('"') ?: "",
                                    imageUrl = formObj["imageUrl"]?.toString()?.trim('"') ?: ""
                                )
                            )
                        }
                    }

                    val nextMatchObj = overviewObj?.get("nextMatch")?.jsonObjectOrNull
                    val nextMatch = nextMatchObj?.let {
                        val opponentObj = it["opponent"]?.jsonObjectOrNull
                        com.paperapps.paperscores.network.models.NextMatch(
                            id = it["id"]?.toString() ?: "",
                            opponentName = opponentObj?.get("name")?.toString()?.trim('"') ?: "",
                            isHome = it["home"]?.toString()?.toBoolean() ?: false,
                            date = it["status"]?.jsonObjectOrNull?.get("utcTime")?.toString()?.trim('"') 
                                ?: it["status"]?.jsonObjectOrNull?.get("startDateStr")?.toString()?.trim('"') 
                                ?: "",
                            tournamentName = it["tournament"]?.jsonObjectOrNull?.get("name")?.toString()?.trim('"') ?: ""
                        )
                    }

                    val tableList = mutableListOf<com.paperapps.paperscores.network.models.TableEntry>()
                    val tableArray = teamData["table"]?.jsonArrayOrNull ?: overviewObj?.get("table")?.jsonArrayOrNull
                    if (tableArray != null && tableArray.isNotEmpty()) {
                        val tableData = tableArray[0].jsonObjectOrNull?.get("data")?.jsonObjectOrNull
                        
                        val allTables = mutableListOf<kotlinx.serialization.json.JsonArray>()
                        val isComposite = tableData?.get("composite")?.jsonPrimitive?.booleanOrNull == true
                        
                        if (isComposite) {
                            tableData?.get("tables")?.jsonArrayOrNull?.forEach { t ->
                                val all = t.jsonObjectOrNull?.get("table")?.jsonObjectOrNull?.get("all")?.jsonArrayOrNull
                                if (all != null) allTables.add(all)
                            }
                        } else {
                            val all = tableData?.get("table")?.jsonObjectOrNull?.get("all")?.jsonArrayOrNull
                            if (all != null) allTables.add(all)
                        }

                        val targetTable = allTables.find { jsonArray ->
                            jsonArray.any { entryEl ->
                                val idStr = entryEl.jsonObjectOrNull?.get("id")?.toString()?.trim('"')
                                idStr == teamId
                            }
                        } ?: allTables.firstOrNull()

                        targetTable?.forEach { entryEl ->
                            val entryObj = entryEl.jsonObjectOrNull
                            if (entryObj != null) {
                                val scoresStr = entryObj["scoresStr"]?.toString()?.trim('"') ?: ""
                                val scores = scoresStr.split("-")
                                val goalsFor = scores.getOrNull(0)?.toIntOrNull() ?: 0
                                val goalsAgainst = scores.getOrNull(1)?.toIntOrNull() ?: 0
                                tableList.add(
                                    com.paperapps.paperscores.network.models.TableEntry(
                                        id = entryObj["id"]?.toString()?.trim('"') ?: "",
                                        name = entryObj["name"]?.toString()?.trim('"') ?: "",
                                        played = entryObj["played"]?.toString()?.toIntOrNull() ?: 0,
                                        wins = entryObj["wins"]?.toString()?.toIntOrNull() ?: 0,
                                        draws = entryObj["draws"]?.toString()?.toIntOrNull() ?: 0,
                                        losses = entryObj["losses"]?.toString()?.toIntOrNull() ?: 0,
                                        goalsFor = goalsFor,
                                        goalsAgainst = goalsAgainst,
                                        points = entryObj["pts"]?.toString()?.toIntOrNull() ?: 0
                                    )
                                )
                            }
                        }
                    }

                    val squadSections = mutableListOf<com.paperapps.paperscores.network.models.SquadSection>()
                    val squadObj = teamData["squad"]?.jsonObjectOrNull
                    val squadList = squadObj?.get("squad")?.jsonArrayOrNull
                    squadList?.forEach { sectionEl ->
                        val sectionObj = sectionEl.jsonObjectOrNull
                        if (sectionObj != null) {
                            val title = sectionObj["title"]?.toString()?.trim('"') ?: ""
                            val membersList = mutableListOf<com.paperapps.paperscores.network.models.SquadMember>()
                            sectionObj["members"]?.jsonArrayOrNull?.forEach { memberEl ->
                                val memberObj = memberEl.jsonObjectOrNull
                                if (memberObj != null) {
                                    membersList.add(
                                        com.paperapps.paperscores.network.models.SquadMember(
                                            id = memberObj["id"]?.toString() ?: "",
                                            name = memberObj["name"]?.toString()?.trim('"') ?: "",
                                            shirtNumber = memberObj["shirtNumber"]?.toString()?.toIntOrNull(),
                                            role = memberObj["role"]?.toString()?.trim('"') ?: "",
                                            ccode = memberObj["ccode"]?.toString()?.trim('"')
                                        )
                                    )
                                }
                            }
                            squadSections.add(
                                com.paperapps.paperscores.network.models.SquadSection(
                                    title = title,
                                    members = membersList
                                )
                            )
                        }
                    }

                    return@withContext com.paperapps.paperscores.network.models.TeamDetails(
                        id = teamId,
                        name = name,
                        country = country,
                        primaryLeagueName = primaryLeagueName,
                        fixtures = fixturesList,
                        teamForm = teamFormList,
                        nextMatch = nextMatch,
                        table = tableList.ifEmpty { null },
                        squad = squadSections
                    )
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getLeagueDetails(leagueId: String): com.paperapps.paperscores.network.models.TournamentDetails? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = client.get("https://www.fotmob.com/leagues/$leagueId") {
                    header(io.ktor.http.HttpHeaders.CacheControl, "no-cache")
                }
                val text = response.bodyAsText()
                val regex = """<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""".toRegex(RegexOption.DOT_MATCHES_ALL)
                val match = regex.find(text)
                if (match != null) {
                    val jsonString = match.groupValues[1]
                    val jsonObject = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObjectOrNull
                    val pageProps = jsonObject?.get("props")?.jsonObjectOrNull?.get("pageProps")?.jsonObjectOrNull ?: return@withContext null

                    val detailsObj = pageProps["details"]?.jsonObjectOrNull
                    val name = detailsObj?.get("name")?.toString()?.trim('"') ?: ""

                    // Parse Table
                    val tableArray = pageProps["table"]?.jsonArrayOrNull
                    val tableEntries = mutableListOf<com.paperapps.paperscores.network.models.TableEntry>()
                    if (tableArray != null && tableArray.isNotEmpty()) {
                        val firstTableObj = tableArray.firstOrNull()?.jsonObjectOrNull
                        val tableDataObj = firstTableObj?.get("data")?.jsonObjectOrNull
                        val tableAllObj = tableDataObj?.get("table")?.jsonObjectOrNull
                        val allArray = tableAllObj?.get("all")?.jsonArrayOrNull

                        allArray?.forEach { el ->
                            val entryObj = el.jsonObjectOrNull ?: return@forEach
                            val id = entryObj["id"]?.toString()?.trim('"') ?: ""
                            val tName = entryObj["name"]?.toString()?.trim('"') ?: ""
                            val played = entryObj["played"]?.jsonPrimitive?.intOrNull ?: 0
                            val wins = entryObj["wins"]?.jsonPrimitive?.intOrNull ?: 0
                            val draws = entryObj["draws"]?.jsonPrimitive?.intOrNull ?: 0
                            val losses = entryObj["losses"]?.jsonPrimitive?.intOrNull ?: 0
                            val pts = entryObj["pts"]?.jsonPrimitive?.intOrNull ?: 0
                            val scoresStr = entryObj["scoresStr"]?.toString()?.trim('"') ?: "0-0"
                            val parts = scoresStr.split("-")
                            val goalsFor = parts.getOrNull(0)?.toIntOrNull() ?: 0
                            val goalsAgainst = parts.getOrNull(1)?.toIntOrNull() ?: 0

                            tableEntries.add(
                                com.paperapps.paperscores.network.models.TableEntry(
                                    id = id,
                                    name = tName,
                                    played = played,
                                    wins = wins,
                                    draws = draws,
                                    losses = losses,
                                    goalsFor = goalsFor,
                                    goalsAgainst = goalsAgainst,
                                    points = pts
                                )
                            )
                        }
                    }

                    // Parse Fixtures
                    val fixturesArray = pageProps["fixtures"]?.jsonObjectOrNull?.get("allMatches")?.jsonArrayOrNull
                    val fixtures = mutableListOf<com.paperapps.paperscores.network.models.MatchDetails>()
                    fixturesArray?.forEach { fEl ->
                        val fObj = fEl.jsonObjectOrNull ?: return@forEach
                        val matchId = fObj["id"]?.toString()?.trim('"') ?: return@forEach
                        
                        val homeObj = fObj["home"]?.jsonObjectOrNull
                        val awayObj = fObj["away"]?.jsonObjectOrNull
                        val hId = homeObj?.get("id")?.toString()?.trim('"') ?: ""
                        val hName = homeObj?.get("name")?.toString()?.trim('"') ?: ""
                        val aId = awayObj?.get("id")?.toString()?.trim('"') ?: ""
                        val aName = awayObj?.get("name")?.toString()?.trim('"') ?: ""

                        val statusObj = fObj["status"]?.jsonObjectOrNull
                        val started = statusObj?.get("started")?.jsonPrimitive?.booleanOrNull ?: false
                        val finished = statusObj?.get("finished")?.jsonPrimitive?.booleanOrNull ?: false
                        val cancelled = statusObj?.get("cancelled")?.jsonPrimitive?.booleanOrNull ?: false
                        val utcTime = statusObj?.get("utcTime")?.toString()?.trim('"') ?: ""
                        val reasonObj = statusObj?.get("reason")?.jsonObjectOrNull
                        val reasonShort = reasonObj?.get("short")?.toString()?.trim('"')
                        
                        val scoreStr = statusObj?.get("scoreStr")?.toString()?.trim('"')
                        val scoreParts = scoreStr?.split(" - ")
                        val hScore = scoreParts?.getOrNull(0)?.trim()?.toIntOrNull()
                        val aScore = scoreParts?.getOrNull(1)?.trim()?.toIntOrNull()

                        val statusStr = if (finished) "Finished" else if (!started) "Upcoming" else if (cancelled) "Cancelled" else if (!reasonShort.isNullOrBlank() && reasonShort !in listOf("HT", "FT", "Pen", "AET", "Half-Time", "Full-Time")) reasonShort else "Active"
                        
                        fixtures.add(
                            com.paperapps.paperscores.network.models.MatchDetails(
                                matchId = matchId,
                                homeTeam = com.paperapps.paperscores.network.models.Team(hId, hName, ""),
                                awayTeam = com.paperapps.paperscores.network.models.Team(aId, aName, ""),
                                score = if (!started) com.paperapps.paperscores.network.models.Score(null, null) else com.paperapps.paperscores.network.models.Score(hScore, aScore),
                                status = statusStr,
                                matchTime = utcTime,
                                liveTime = reasonShort ?: "",
                                tournamentId = leagueId,
                                tournamentName = name
                            )
                        )
                    }

                    return@withContext com.paperapps.paperscores.network.models.TournamentDetails(
                        id = leagueId,
                        name = name,
                        table = tableEntries,
                        fixtures = fixtures.filter { it.status == "Upcoming" },
                        overviewMatches = fixtures.filter { it.status == "Finished" || it.status == "Active" }.takeLast(5).reversed() + fixtures.filter { it.status == "Upcoming" }.take(5)
                    )
                }
                return@withContext null
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
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

    suspend fun getLeagueTable(url: String): List<com.paperapps.paperscores.network.models.TableEntry>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val absoluteUrl = if (url.startsWith("/")) "https://www.fotmob.com$url" else url
                android.util.Log.d("FotMobApiClient", "Fetching table from $absoluteUrl")
                val response = client.get(absoluteUrl) {
                    header(io.ktor.http.HttpHeaders.CacheControl, "no-cache")
                }
                val bytes = response.body<ByteArray>()
                android.util.Log.d("FotMobApiClient", "Fetched ${bytes.size} bytes")
                val text = try {
                    java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(bytes)).bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    String(bytes)
                }
                
                val regex = """<t name="([^"]+)" id="([^"]+)" p="([^"]+)" w="([^"]+)" d="([^"]+)" l="([^"]+)" g="([^"]+)" c="([^"]+)"(.*?)/>""".toRegex()
                val matchResults = regex.findAll(text)
                val tableList = matchResults.map {
                    val wins = it.groupValues[4].toIntOrNull() ?: 0
                    val draws = it.groupValues[5].toIntOrNull() ?: 0
                    val losses = it.groupValues[6].toIntOrNull() ?: 0
                    com.paperapps.paperscores.network.models.TableEntry(
                        name = it.groupValues[1],
                        id = it.groupValues[2],
                        points = it.groupValues[3].toIntOrNull() ?: 0,
                        wins = wins,
                        draws = draws,
                        losses = losses,
                        goalsFor = it.groupValues[7].toIntOrNull() ?: 0,
                        goalsAgainst = it.groupValues[8].toIntOrNull() ?: 0,
                        played = wins + draws + losses
                    )
                }.toList()
                android.util.Log.d("FotMobApiClient", "Parsed ${tableList.size} table entries")
                tableList
            } catch (e: Exception) {
                android.util.Log.e("FotMobApiClient", "Failed to fetch or parse table", e)
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getPlayoffBracket(leagueId: String): List<com.paperapps.paperscores.network.models.PlayoffRound>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = client.get("https://www.fotmob.com/leagues/$leagueId?_=${System.currentTimeMillis()}") {
                    header(io.ktor.http.HttpHeaders.CacheControl, "no-cache")
                }
                val text = response.bodyAsText()
                val regex = """<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""".toRegex(RegexOption.DOT_MATCHES_ALL)
                val match = regex.find(text)
                if (match != null) {
                    val jsonString = match.groupValues[1]
                    val jsonObject = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObjectOrNull
                    val pageProps = jsonObject?.get("props")?.jsonObjectOrNull?.get("pageProps")?.jsonObjectOrNull
                    
                    val playoffObj = pageProps?.get("playoff")?.jsonObjectOrNull ?: return@withContext null
                    val roundsArray = playoffObj["rounds"]?.jsonArrayOrNull ?: return@withContext null
                    
                    val playoffRounds = mutableListOf<com.paperapps.paperscores.network.models.PlayoffRound>()
                    roundsArray.forEach { roundEl ->
                        val roundObj = roundEl.jsonObjectOrNull ?: return@forEach
                        val stageName = roundObj["stage"]?.toString()?.trim('"') ?: ""
                        val matchupsArray = roundObj["matchups"]?.jsonArrayOrNull ?: return@forEach
                        
                        val matchups = mutableListOf<com.paperapps.paperscores.network.models.PlayoffMatchup>()
                        matchupsArray.forEach { matchupEl ->
                            val matchupObj = matchupEl.jsonObjectOrNull ?: return@forEach
                            val homeTeam = matchupObj["homeTeam"]?.toString()?.trim('"') ?: ""
                            val awayTeam = matchupObj["awayTeam"]?.toString()?.trim('"') ?: ""
                            val homeScore = matchupObj["homeScore"]?.toString()?.trim('"') ?: ""
                            val awayScore = matchupObj["awayScore"]?.toString()?.trim('"') ?: ""
                            val winner = matchupObj["winner"]?.toString()?.trim('"')?.takeIf { it != "null" }
                            
                            matchups.add(
                                com.paperapps.paperscores.network.models.PlayoffMatchup(
                                    homeTeam = homeTeam,
                                    awayTeam = awayTeam,
                                    homeScore = homeScore,
                                    awayScore = awayScore,
                                    winner = winner
                                )
                            )
                        }
                        playoffRounds.add(
                            com.paperapps.paperscores.network.models.PlayoffRound(
                                roundName = stageName,
                                matchups = matchups
                            )
                        )
                    }
                    return@withContext playoffRounds
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
