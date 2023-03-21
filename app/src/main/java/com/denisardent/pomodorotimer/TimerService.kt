package com.denisardent.pomodorotimer

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.*

class TimerService : Service() {
    private var remainingTime: Int = 60
    private var isTimerRunning = false

    private var updateTimer = Timer()
    private var pomoTimer = Timer()
    //получение доступа к notificationManager
    private lateinit var notificationManager: NotificationManager

    /*
    Переопределяю OnStartCommand, создаю канал уведомлений для андроид 8.0+
    Получаю доступ к NotificationManager для работы с уведомлениями;
    задаю функции для взаимодействия с таймером
    Возвращаю flag: START_STICKY, так как мне надо, чтобы сервис
    сразу начинал работать, как только его убьет система
     */

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        getNotificationManager()
        // получаю значение действия от интента
        val action = intent?.getStringExtra(TIMER_ACTION)!!

        when (action){
            START -> startTimer()
            PAUSE -> pauseTimer()
            RESET -> resetTimer()
            ENDED -> timerEnded()
            GET_STATUS -> sendStatus()
            MOVE_TO_FOREGROUND -> moveToForeground()
            MOVE_TO_BACKGROUND -> moveToBackground()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun sendStatus(){
        val statusIntent = Intent()
        statusIntent.action = TIMER_STATUS
        statusIntent.putExtra(IS_TIMER_RUNNING, isTimerRunning)
        statusIntent.putExtra(REMAINING_TIME, remainingTime)
        sendBroadcast(statusIntent)

    }

    private fun startTimer(){
        isTimerRunning = true
        sendStatus()

        pomoTimer = Timer()
        val mainTask = object: TimerTask(){
            override fun run() {
                val timerIntent = Intent()
                timerIntent.action = TIMER_TICK

                remainingTime--
                if (remainingTime == 0) timerEnded()
                timerIntent.putExtra(REMAINING_TIME, remainingTime)
                sendBroadcast(timerIntent)
            }

        }
        pomoTimer.scheduleAtFixedRate(mainTask, 0, 1000)
    }

    private fun pauseTimer(){
        pomoTimer.cancel()
        isTimerRunning = false
        sendStatus()
    }

    private fun resetTimer(){
        pauseTimer()
        remainingTime = 60
        sendStatus()
    }

    private fun timerEnded(){
        pauseTimer()
        remainingTime = 0
        sendStatus()
    }

    // Создание канала для создания уведомлений
    private fun createChannel(){
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(
                CHANNEL_NOTIFICATION_ID,
                "PomoTimer",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.setSound(null,null)
            notificationChannel.setShowBadge(true)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun getNotificationManager() {
        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager
    }

    private fun buildNotification(): Notification{
        val title = if (isTimerRunning){
            "PomoTimer is running"
        } else {
            "Pomotimer is paused"
        }

        val minutes = remainingTime/60
        val seconds = remainingTime%60

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)

        return NotificationCompat.Builder(this, CHANNEL_NOTIFICATION_ID)
            .setContentTitle(title)
            .setOngoing(true)
            .setContentText(
                "${"%02d".format(minutes)}:${"%02d".format(seconds)}"
            )
            .setColorized(true)
            .setColor(Color.parseColor("#A7DBFF"))
            .setSmallIcon(R.drawable.ic_timer_logo)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun updateNotification(){
        notificationManager.notify(1, buildNotification())
    }

    private fun moveToForeground(){

        if (isTimerRunning){
            startForeground(1,buildNotification())

            updateTimer = Timer()

            updateTimer.schedule(object : TimerTask() {
                override fun run() {
                    updateNotification() } }, 0, 1000)
        }
    }

    private fun moveToBackground(){
        updateTimer.cancel()
        stopForeground(true)
    }

    companion object{
        //constants for timer and intent
        const val CHANNEL_NOTIFICATION_ID = "TIMER_NOTIFICATION"

        const val START ="START"
        const val PAUSE = "PAUSE"
        const val RESET = "RESET"
        const val ENDED = "ENDED"
        const val GET_STATUS = "GET_STATUS"
        const val MOVE_TO_FOREGROUND = "MOVE_TO_FOREGROUND"
        const val MOVE_TO_BACKGROUND = "MOVE_TO_BACKGROUND"

        const val TIMER_ACTION = "TIMER_ACTION"
        const val REMAINING_TIME = "REMAINING_TIME"
        const val IS_TIMER_RUNNING = "IS_TIMER_RUNNING"


        const val TIMER_TICK = "TIMER_TICK"
        const val TIMER_STATUS = "TIMER_STATUS"
    }
}