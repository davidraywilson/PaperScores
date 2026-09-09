package com.paperapps.paperscores.repository

import com.paperapps.paperscores.network.FotMobApiClient
import com.paperapps.paperscores.network.models.MatchDetails
import com.paperapps.paperscores.network.models.Team
import com.paperapps.paperscores.network.models.TeamNextMatch
import com.paperapps.paperscores.network.models.Tournament
import com.paperapps.paperscores.network.models.SearchResult
import com.paperapps.paperscores.network.models.Score
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.paperapps.paperscores.db.SoccerDao
import com.paperapps.paperscores.db.FollowedTeamEntity
import com.paperapps.paperscores.db.FollowedTournamentEntity
import java.util.TimeZone
import java.util.Calendar
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.boolean
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SoccerRepository private constructor(private val context: android.content.Context, private val dao: SoccerDao) {
    private val apiClient = FotMobApiClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch { refreshTodaysGames() }
    }

    private val _rawTodaysGames = MutableStateFlow<List<MatchDetails>>(emptyList())
    
    private val _isLoadingTodaysGames = MutableStateFlow(true)
    val isLoadingTodaysGames: StateFlow<Boolean> = _isLoadingTodaysGames.asStateFlow()

    val followedTeams: StateFlow<List<Team>> = dao.getFollowedTeamsFlow()
        .map { list -> list.map { it.toTeam() } }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    val followedTournaments: StateFlow<List<Tournament>> = dao.getFollowedTournamentsFlow()
        .map { list -> list.map { it.toTournament() } }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    val todaysGames: StateFlow<List<MatchDetails>> = combine(
        _rawTodaysGames,
        followedTeams,
        followedTournaments
    ) { rawGames, teams, tournaments ->
        val tourneyIds = tournaments.map { it.id }
        val teamIds = teams.map { it.id }
        
        val followedGames = rawGames.filter { game ->
            game.tournamentId in tourneyIds ||
            game.homeTeam.id in teamIds ||
            game.awayTeam.id in teamIds
        }
        scheduleNotifications(rawGames, teamIds)
        followedGames
    }.stateIn(scope, SharingStarted.Lazily, emptyList())

    private fun extractAttribute(block: String, attr: String): String? {
        val regex = """$attr="([^"]+)"""".toRegex()
        return regex.find(block)?.groupValues?.get(1)
    }

    suspend fun refreshTodaysGames(date: Date? = null, forceRefresh: Boolean = false) = coroutineScope {
        _isLoadingTodaysGames.value = true
        val format = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        if (date != null) {
            calendar.time = date
        }
        val todayStr = format.format(calendar.time)
        calendar.add(Calendar.DATE, -1)
        val yesterdayStr = format.format(calendar.time)
        calendar.add(Calendar.DATE, 2)
        val tomorrowStr = format.format(calendar.time)

        val deferreds = listOf(
            async { apiClient.getMatchesByDate(yesterdayStr, forceRefresh) },
            async { apiClient.getMatchesByDate(todayStr, forceRefresh) },
            async { apiClient.getMatchesByDate(tomorrowStr, forceRefresh) }
        )
        
        val responses = deferreds.awaitAll().filterNotNull()

        if (responses.isEmpty()) {
            _rawTodaysGames.value = emptyList()
            _isLoadingTodaysGames.value = false
            return@coroutineScope
        }

        val inputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("Europe/Oslo")
        val outputFormat = SimpleDateFormat("HH:mm", Locale.US)
        outputFormat.timeZone = TimeZone.getDefault()
        
        val localDateOnlyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        localDateOnlyFormat.timeZone = TimeZone.getDefault()
        val targetLocalStr = localDateOnlyFormat.format(date ?: Date())

        val parsedMatches = mutableListOf<MatchDetails>()
        val seenMatchIds = mutableSetOf<String>()

        try {
            for (response in responses) {
                val leagues = response.split("<league ")
            for (i in 1 until leagues.size) {
                val leagueBlock = leagues[i]
                val tourneyId = extractAttribute(leagueBlock, "pl") ?: extractAttribute(leagueBlock, "id") ?: continue
                val tourneyName = extractAttribute(leagueBlock, "plName") ?: extractAttribute(leagueBlock, "name") ?: ""
                
                val matches = leagueBlock.split("<match ")
                for (j in 1 until matches.size) {
                    val matchBlock = matches[j].substringBefore("/>")
                    val matchId = extractAttribute(matchBlock, "id") ?: continue
                    val hTeam = extractAttribute(matchBlock, "hTeam") ?: ""
                    val aTeam = extractAttribute(matchBlock, "aTeam") ?: ""
                    val time = extractAttribute(matchBlock, "time") ?: ""
                    val status = extractAttribute(matchBlock, "Status") ?: ""
                    val hScore = if (status == "N") null else extractAttribute(matchBlock, "hScore")?.toIntOrNull()
                    val aScore = if (status == "N") null else extractAttribute(matchBlock, "aScore")?.toIntOrNull()
                    val hId = extractAttribute(matchBlock, "hId") ?: ""
                    val aId = extractAttribute(matchBlock, "aId") ?: ""
                    
                        val isFinished = status == "F" || status == "FT" || status == "AET" || status == "Pen"
                        val statusStr = when (status) {
                            "F", "FT", "AET", "Pen" -> "Finished"
                            "N" -> "Upcoming"
                            "S", "HT" -> "Active"
                            "P", "Post" -> "Postponed"
                            "Del", "Dly" -> "Delayed"
                            "Susp", "Ssp" -> "Suspended"
                            "Canc", "Can" -> "Cancelled"
                            "Abd", "Abn" -> "Abandoned"
                            "Int" -> "Interrupted"
                            else -> if (isFinished) "Finished" else status
                        }
                        var matchTimeStr = time.substringAfter(" ")
                        var matchLocalDateStr = ""
                        var matchStartTimeMs: Long? = null
                        
                        if (time.isNotEmpty() && !isFinished) {
                            try {
                                val parsedDate = inputFormat.parse(time)
                                if (parsedDate != null) {
                                    matchLocalDateStr = localDateOnlyFormat.format(parsedDate)
                                    matchTimeStr = outputFormat.format(parsedDate)
                                    matchStartTimeMs = parsedDate.time
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else if (time.isNotEmpty() && isFinished) {
                            try {
                                val parsedDate = inputFormat.parse(time)
                                if (parsedDate != null) {
                                    matchLocalDateStr = localDateOnlyFormat.format(parsedDate)
                                    matchStartTimeMs = parsedDate.time
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        if (matchLocalDateStr.isNotEmpty() && matchLocalDateStr != targetLocalStr) {
                            continue
                        }
                        
                        if (!seenMatchIds.add(matchId)) continue

                        var liveMinute = ""
                        if (status == "HT") {
                            liveMinute = "HT"
                        } else if (status == "S") {
                            val gs = extractAttribute(matchBlock, "gs")
                            val shs = extractAttribute(matchBlock, "shs")
                            
                            val tzFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.US)
                            tzFormat.timeZone = java.util.TimeZone.getTimeZone("Europe/Oslo")
                            val now = java.util.Date().time
                            
                            try {
                                if (shs != null) {
                                    val parsedShs = tzFormat.parse(shs)
                                    if (parsedShs != null) {
                                        val diff = Math.max(0, (now - parsedShs.time) / 60000)
                                        liveMinute = if (diff >= 45) "90+'" else "${45 + diff}'"
                                    }
                                } else if (gs != null) {
                                    val parsedGs = tzFormat.parse(gs)
                                    if (parsedGs != null) {
                                        val diff = Math.max(0, (now - parsedGs.time) / 60000)
                                        liveMinute = if (diff >= 45) "45+'" else "${diff}'"
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        val matchDetails = MatchDetails(
                            matchId = matchId,
                            homeTeam = Team(hId, hTeam, ""),
                            awayTeam = Team(aId, aTeam, ""),
                            score = Score(hScore, aScore),
                            status = statusStr,
                            matchTime = when (statusStr) {
                                "Finished" -> "FT"
                                "Active" -> if (liveMinute.isNotBlank()) liveMinute else matchTimeStr
                                "Upcoming" -> matchTimeStr
                                else -> statusStr
                            },
                            liveTime = liveMinute,
                            tournamentId = tourneyId,
                            tournamentName = tourneyName,
                            startTimeMs = matchStartTimeMs
                        )
                        parsedMatches.add(matchDetails)
                    }
                }
            }
            
            // Removed mass scraping of live match details for performance

            _rawTodaysGames.value = parsedMatches
        } catch (e: Exception) {
            e.printStackTrace()
            _rawTodaysGames.value = emptyList()
        }
        _isLoadingTodaysGames.value = false
    }

    suspend fun getMatchDetails(matchId: String, forceRefresh: Boolean = false): MatchDetails? {
        val details = apiClient.getMatchDetails(matchId, forceRefresh)
        
        if (details != null) {
            val currentGames = _rawTodaysGames.value.toMutableList()
            val index = currentGames.indexOfFirst { it.matchId == matchId }
            if (index != -1) {
                val oldMatch = currentGames[index]
                val updatedMatch = oldMatch.copy(
                    score = details.score,
                    status = details.status,
                    liveTime = details.liveTime
                )
                currentGames[index] = updatedMatch
                _rawTodaysGames.value = currentGames
            }
        }
        
        return details
    }

    suspend fun searchEntities(query: String): List<SearchResult> {
        val response = apiClient.searchEntities(query) ?: return emptyList()
        val results = mutableListOf<SearchResult>()

        try {
            val teams = response["teamSuggest"]?.jsonArray?.firstOrNull()?.jsonObject?.get("options")?.jsonArray
            teams?.forEach { element ->
                val option = element.jsonObject
                val text = option["text"]?.jsonPrimitive?.content ?: ""
                val payload = option["payload"]?.jsonObject
                val id = payload?.get("id")?.jsonPrimitive?.content ?: ""
                
                val name = text.substringBefore("|")
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    val imageUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/${id}.png"
                    results.add(SearchResult.TeamResult(Team(id, name, imageUrl)))
                }
            }

            val leagues = response["leagueSuggest"]?.jsonArray?.firstOrNull()?.jsonObject?.get("options")?.jsonArray
            leagues?.forEach { element ->
                val option = element.jsonObject
                val text = option["text"]?.jsonPrimitive?.content ?: ""
                val payload = option["payload"]?.jsonObject
                val id = payload?.get("id")?.jsonPrimitive?.content ?: ""
                
                val name = text.substringBefore("|")
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    val imageUrl = "https://images.fotmob.com/image_resources/logo/leaguelogo/${id}.png"
                    results.add(SearchResult.TournamentResult(Tournament(id, name, imageUrl)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    fun followTeam(team: Team) {
        scope.launch {
            dao.insertTeam(FollowedTeamEntity.fromTeam(team))
        }
    }

    fun unfollowTeam(teamId: String) {
        scope.launch {
            dao.deleteTeam(teamId)
        }
    }

    fun followTournament(tournament: Tournament) {
        scope.launch {
            dao.insertTournament(FollowedTournamentEntity.fromTournament(tournament))
        }
    }

    fun unfollowTournament(tournamentId: String) {
        scope.launch {
            dao.deleteTournament(tournamentId)
        }
    }

    suspend fun fetchNextMatchesForTeams(teamIds: List<String>): Map<String, TeamNextMatch> {
        val resultMap = mutableMapOf<String, TeamNextMatch>()
        coroutineScope {
            teamIds.map { teamId ->
                async {
                    val match = apiClient.getTeamNextMatch(teamId)
                    if (match != null) {
                        resultMap[teamId] = match
                    }
                }
            }.awaitAll()
        }
        return resultMap
    }

    suspend fun getLeagueTable(url: String): List<com.paperapps.paperscores.network.models.TableEntry>? {
        return apiClient.getLeagueTable(url)
    }

    suspend fun getPlayoffBracket(leagueId: String): List<com.paperapps.paperscores.network.models.PlayoffRound>? {
        return apiClient.getPlayoffBracket(leagueId)
    }

    suspend fun getTournamentDetails(leagueId: String): com.paperapps.paperscores.network.models.TournamentDetails? {
        return apiClient.getLeagueDetails(leagueId)
    }
    suspend fun getTeamDetails(teamId: String): com.paperapps.paperscores.network.models.TeamDetails? {
        return apiClient.getTeamDetails(teamId)
    }

    private fun scheduleNotifications(games: List<MatchDetails>, teamIds: List<String>) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        val userPrefs = UserPreferences.getInstance(context)
        val remindersEnabled = userPrefs.isMatchRemindersEnabled
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager

        val now = System.currentTimeMillis()
        games.forEach { match ->
            val isTeamFollowed = match.homeTeam.id in teamIds || match.awayTeam.id in teamIds
            val uniqueWorkName = "match_notification_${match.matchId}"
            val serviceIntent = android.content.Intent(context, com.paperapps.paperscores.service.ScoreOverlayService::class.java).apply {
                putExtra("matchId", match.matchId)
            }
            val pendingIntent = android.app.PendingIntent.getForegroundService(
                context,
                match.matchId.hashCode(),
                serviceIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            if (!remindersEnabled || !isTeamFollowed || match.status != "Upcoming" || match.startTimeMs == null) {
                workManager.cancelUniqueWork(uniqueWorkName)
                alarmManager.cancel(pendingIntent)
                return@forEach
            }
            
            val timeToKickoff = match.startTimeMs!! - now
            val fifteenMinsMs = 15 * 60 * 1000L
            val triggerTime = match.startTimeMs!! - fifteenMinsMs

            if (timeToKickoff > 0) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, Math.max(now, triggerTime), pendingIntent)
                            workManager.cancelUniqueWork(uniqueWorkName)
                            return@forEach
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, Math.max(now, triggerTime), pendingIntent)
                        workManager.cancelUniqueWork(uniqueWorkName)
                        return@forEach
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }

                // Fallback to WorkManager
                val delay = Math.max(0L, timeToKickoff - fifteenMinsMs)
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.paperapps.paperscores.worker.MatchNotificationWorker>()
                    .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .setInputData(androidx.work.workDataOf("matchId" to match.matchId))
                    .addTag("match_notification")
                    .build()
                
                workManager.enqueueUniqueWork(
                    uniqueWorkName,
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        }
    }

    fun cancelAllNotifications() {
        val workManager = androidx.work.WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("match_notification")
        
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        _rawTodaysGames.value.forEach { match ->
            val serviceIntent = android.content.Intent(context, com.paperapps.paperscores.service.ScoreOverlayService::class.java)
            val pendingIntent = android.app.PendingIntent.getForegroundService(
                context,
                match.matchId.hashCode(),
                serviceIntent,
                android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SoccerRepository? = null

        fun initialize(context: android.content.Context, dao: SoccerDao) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = SoccerRepository(context.applicationContext, dao)
                    }
                }
            }
        }

        fun getInstance(): SoccerRepository {
            return INSTANCE ?: throw IllegalStateException("SoccerRepository must be initialized")
        }
    }
}
