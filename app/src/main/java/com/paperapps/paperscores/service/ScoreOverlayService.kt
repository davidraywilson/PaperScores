package com.paperapps.paperscores.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import com.paperapps.paperscores.MainActivity
import com.paperapps.paperscores.R
import com.paperapps.paperscores.network.models.MatchDetails
import com.paperapps.paperscores.repository.SoccerRepository
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class ScoreOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    private var windowManager: WindowManager? = null
    
    private val overlayViews = mutableMapOf<String, ComposeView>()
    private val layoutParamsMap = mutableMapOf<String, WindowManager.LayoutParams>()
    
    private var dismissView: ComposeView? = null
    private val repository = SoccerRepository.getInstance()

    private val activeMatchIds = mutableSetOf<String>()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isPolling = false
    private val isHoveringDismiss = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        setupDismissView()
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun setupDismissView() {
        dismissView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ScoreOverlayService)
            setViewTreeViewModelStoreOwner(this@ScoreOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ScoreOverlayService)
            setContent {
                val hovering by isHoveringDismiss.collectAsState()
                DismissZone(isHovering = hovering)
            }
            visibility = View.GONE
        }

        val dismissParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 150
        }
        
        windowManager?.addView(dismissView, dismissParams)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intentMatchId = intent?.getStringExtra("matchId")
        
        if (intentMatchId == null && overlayViews.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        startForegroundService()
        
        if (intentMatchId != null) {
            if (!activeMatchIds.contains(intentMatchId)) {
                activeMatchIds.add(intentMatchId)
                showOverlay(intentMatchId)
            }
            startPolling()
        }
        
        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "score_overlay_channel")
            .setContentTitle("Live Score Pinned")
            .setContentText("Match scores are floating on your screen")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun startPolling() {
        if (isPolling) return
        isPolling = true
        scope.launch {
            while (isActive) {
                if (activeMatchIds.isEmpty()) {
                    isPolling = false
                    break
                }
                activeMatchIds.forEach { id ->
                    repository.getMatchDetails(id)
                }
                delay(60_000)
            }
        }
    }

    private fun getNextYPosition(): Int {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val clearancePx = (120 * displayMetrics.density).toInt()
        var yPos = (100 * displayMetrics.density).toInt()
        
        while (layoutParamsMap.values.any { Math.abs(it.y - yPos) < clearancePx }) {
            yPos += clearancePx
            if (yPos > screenHeight - clearancePx * 2) {
                yPos = (100 * displayMetrics.density).toInt() // Wrap around if too many
                break
            }
        }
        return yPos
    }

    private fun showOverlay(matchId: String) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ScoreOverlayService)
            setViewTreeViewModelStoreOwner(this@ScoreOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ScoreOverlayService)
            
            setContent {
                var currentMatch by remember { mutableStateOf<MatchDetails?>(null) }
                
                LaunchedEffect(matchId) {
                    while(isActive) {
                        currentMatch = repository.getMatchDetails(matchId)
                        delay(60000)
                    }
                }

                currentMatch?.let { match ->
                    ScoreBubble(match)
                }
            }
        }
        
        overlayViews[matchId] = composeView

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = screenWidth // Start on right edge
        layoutParams.y = getNextYPosition()
        layoutParamsMap[matchId] = layoutParams

        composeView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        dismissView?.visibility = View.VISIBLE
                        isHoveringDismiss.value = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        layoutParams.x = initialX + dx.toInt()
                        layoutParams.y = initialY + dy.toInt()
                        windowManager?.updateViewLayout(composeView, layoutParams)

                        dismissView?.let { dv ->
                            val location = IntArray(2)
                            dv.getLocationOnScreen(location)
                            val dismissX = location[0]
                            val dismissY = location[1]
                            val centerX = dismissX + dv.width / 2
                            val centerY = dismissY + dv.height / 2

                            val distance = Math.hypot((event.rawX - centerX).toDouble(), (event.rawY - centerY).toDouble())
                            isHoveringDismiss.value = distance < 250f
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        dismissView?.visibility = View.GONE
                        if (isHoveringDismiss.value) {
                            removeOverlay(matchId)
                        } else if (!isClick) {
                            // Snap to nearest edge
                            val screenMid = screenWidth / 2
                            if (layoutParams.x > screenMid) {
                                layoutParams.x = screenWidth
                            } else {
                                layoutParams.x = 0
                            }
                            windowManager?.updateViewLayout(composeView, layoutParams)
                        } else {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse("paperscores://game/$matchId"),
                                this@ScoreOverlayService,
                                MainActivity::class.java
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(composeView, layoutParams)
    }

    private fun removeOverlay(matchId: String) {
        activeMatchIds.remove(matchId)
        val view = overlayViews.remove(matchId)
        layoutParamsMap.remove(matchId)
        if (view != null) {
            windowManager?.removeView(view)
        }
        
        if (activeMatchIds.isEmpty()) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        scope.cancel()
        overlayViews.values.forEach { view ->
            windowManager?.removeView(view)
        }
        overlayViews.clear()
        layoutParamsMap.clear()
        if (dismissView != null) {
            windowManager?.removeView(dismissView)
            dismissView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
}

fun formatKickoffTime(timeStr: String): String {
    if (timeStr.isEmpty()) return timeStr
    try {
        if (timeStr.contains("UTC")) {
            val sdf = java.text.SimpleDateFormat("EEE, MMM d, yyyy, HH:mm 'UTC'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(timeStr)
            if (date != null) {
                val outFmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                outFmt.timeZone = java.util.TimeZone.getDefault()
                return outFmt.format(date)
            }
        }
        
        if (timeStr.contains("T")) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(timeStr)
            if (date != null) {
                val outFmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                outFmt.timeZone = java.util.TimeZone.getDefault()
                return outFmt.format(date)
            }
        }
        
        if (timeStr.contains(":")) {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
            val date = sdf.parse(timeStr)
            if (date != null) {
                val outFmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                return outFmt.format(date)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return timeStr
}

@Composable
fun ScoreBubble(match: MatchDetails) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(PureWhite)
            .border(2.dp, PureBlack, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        val isUpcoming = match.status == "Upcoming"
        
        if (isUpcoming) {
            // Display like the screenshot: Flag | Starts at [Time] | Flag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = match.homeTeam.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(CircleShape)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Starts at",
                        color = androidx.compose.ui.graphics.Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatKickoffTime(match.matchTime),
                        color = PureBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                AsyncImage(
                    model = match.awayTeam.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(CircleShape)
                )
            }
        } else {
            // Display active/finished score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .background(PureBlack, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (match.status == "Active") match.liveTime else match.status,
                        color = PureWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = match.homeTeam.name.take(3).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PureBlack
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "${match.score.home ?: 0} - ${match.score.away ?: 0}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PureBlack
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = match.awayTeam.name.take(3).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PureBlack
                    )
                }
            }
        }
    }
}

@Composable
fun DismissZone(isHovering: Boolean) {
    val size by animateDpAsState(targetValue = if (isHovering) 72.dp else 56.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isHovering) PureBlack else PureWhite)
            .border(2.dp, PureBlack, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Dismiss",
            tint = if (isHovering) PureWhite else PureBlack,
            modifier = Modifier.size(32.dp)
        )
    }
}
