package com.denisardent.pomodorotimer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.denisardent.pomodorotimer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isTimerRunning = false

    private lateinit var statusReceiver: BroadcastReceiver
    private lateinit var timeReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainButton.setOnClickListener {
            if (isTimerRunning) pauseTimer() else startTimer()
        }

        binding.restartIV.setOnClickListener {
            resetTimer()
        }
    }

    override fun onStart() {
        super.onStart()

        // Moving the service to background when the app is visible
        moveToBackground()
    }

    override fun onResume() {
        super.onResume()

        getTimerStatus()

        // Receiving stopwatch status from service
        val statusFilter = IntentFilter()
        statusFilter.addAction(TimerService.TIMER_STATUS)
        statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                val isRunning = p1?.getBooleanExtra(TimerService.IS_TIMER_RUNNING, false)!!
                isTimerRunning = isRunning
                val timeElapsed = p1.getIntExtra(TimerService.REMAINING_TIME, 0)

                updateButtonsUI(isTimerRunning)
                updateTimerUI(timeElapsed)
            }
        }
        registerReceiver(statusReceiver, statusFilter)

        // Receiving time values from service
        val timeFilter = IntentFilter()
        timeFilter.addAction(TimerService.TIMER_TICK)
        timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                val remainingTime = p1?.getIntExtra(TimerService.REMAINING_TIME, 0)!!
                updateTimerUI(remainingTime)
            }
        }
        registerReceiver(timeReceiver, timeFilter)
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(statusReceiver)
        unregisterReceiver(timeReceiver)

        // Moving the service to foreground when the app is in background / not visible
        moveToForeground()
    }

    private fun updateTimerUI(remainingTime: Int) {
        val minutes: Int = remainingTime / 60
        val seconds: Int = remainingTime % 60
        binding.timerValue.text =
            "${"%02d".format(minutes)}:${"%02d".format(seconds)}"
    }

    private fun updateButtonsUI(isStopwatchRunning: Boolean) {
        if (isStopwatchRunning) {
            binding.mainButton.text = getString(R.string.main_button_pause)
            binding.restartIV.visibility = View.INVISIBLE
        } else {
            binding.mainButton.text = getString(R.string.main_button_start)
            binding.restartIV.visibility = View.VISIBLE
        }
    }

    private fun getTimerStatus() {
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.GET_STATUS)
        startService(timerService)
    }

    private fun startTimer() {
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.START)
        startService(timerService)
    }

    private fun pauseTimer() {
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.PAUSE)
        startService(timerService)
    }

    private fun resetTimer() {
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.RESET)
        startService(timerService)
    }

    private fun moveToForeground() {
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(
            TimerService.TIMER_ACTION,
            TimerService.MOVE_TO_FOREGROUND
        )
        startService(timerService)
    }

    private fun moveToBackground() {
        val timerService = Intent(this, TimerService::class.java)
        timerService.putExtra(
            TimerService.TIMER_ACTION,
            TimerService.MOVE_TO_BACKGROUND
        )
        startService(timerService)
    }}