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

    private var isTimerRunning = TIMER_PAUSED

    private lateinit var statusReceiver: BroadcastReceiver
    private lateinit var timeReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainButton.setOnClickListener {
            if (isTimerRunning == TIMER_RUNNING) pauseTimer() else startTimer()
        }

        binding.restartIV.setOnClickListener {
            resetTimer()
            binding.mainButton.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()

        moveToBackground()
    }

    override fun onResume() {
        super.onResume()

        getTimerStatus()

        val statusFilter = IntentFilter()
        statusFilter.addAction(TimerService.TIMER_STATUS)
        statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                val isRunning = p1?.getStringExtra(TimerService.IS_TIMER_RUNNING)!!
                isTimerRunning = isRunning
                val timeElapsed = p1.getIntExtra(TimerService.REMAINING_TIME, 0)

                updateButtonsUI(isTimerRunning)
                updateTimerUI(timeElapsed)
            }
        }
        registerReceiver(statusReceiver, statusFilter)

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

        moveToForeground()
    }

    private fun updateTimerUI(remainingTime: Int) {
        val minutes: Int = remainingTime / 60
        val seconds: Int = remainingTime % 60
        binding.timerValue.text =
            "${"%02d".format(minutes)}:${"%02d".format(seconds)}"
    }

    private fun updateButtonsUI(isTimerRunning: String) {
        when (isTimerRunning) {
            TIMER_RUNNING -> {
                binding.mainButton.text = getString(R.string.main_button_pause)
                binding.restartIV.visibility = View.INVISIBLE
            }
            TIMER_PAUSED ->{
                binding.mainButton.text = getString(R.string.main_button_start)
                binding.restartIV.visibility = View.VISIBLE
            }
            else -> {
                binding.mainButton.visibility = View.INVISIBLE
                binding.restartIV.visibility = View.VISIBLE
            }
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