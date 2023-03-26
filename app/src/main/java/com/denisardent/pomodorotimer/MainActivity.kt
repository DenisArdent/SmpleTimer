package com.denisardent.pomodorotimer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.denisardent.pomodorotimer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val anHour = 3600

    private lateinit var binding: ActivityMainBinding
    private var remainingTime = 0
    private var isTimerRunning = TIMER_PAUSED

    private lateinit var statusReceiver: BroadcastReceiver
    private lateinit var timeReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainButton.setOnClickListener {
            if (remainingTime==0) showToast(this)
            else {
                if(isTimerRunning != TIMER_RUNNING) startTimer() else pauseTimer()
            }
        }

        binding.restartIV.setOnClickListener {
            remainingTime = 0
            resetTimer()
            binding.mainButton.visibility = View.VISIBLE
        }

        binding.minutesFirstUpButton.setOnClickListener{
            if (remainingTime+600<anHour) {
                remainingTime += 600
                updateTimerUI(remainingTime)
                updateButtonsUI(isTimerRunning)
            }
            else showToast(this)
        }

        binding.minutesSecondUpButton.setOnClickListener{
            if (remainingTime+60<anHour) {
                remainingTime += 60
                updateTimerUI(remainingTime)
                updateButtonsUI(isTimerRunning)
            }
            else showToast(this)
        }

        binding.secondsFirstUpButton.setOnClickListener{
            if (remainingTime+10<anHour) {
                remainingTime += 10
                updateTimerUI(remainingTime)
                updateButtonsUI(isTimerRunning)
            }
            else showToast(this)
        }

        binding.secondsSecondUpButton.setOnClickListener{
            if (remainingTime+1<anHour) {
                remainingTime += 1
                updateTimerUI(remainingTime)
                updateButtonsUI(isTimerRunning)
            }
            else showToast(this)
        }

        binding.minutesFirstDownButton.setOnClickListener{
            if (remainingTime-600>0) {
                remainingTime -= 600
                updateTimerUI(remainingTime)
            }
            else showToast(this)
        }

        binding.minutesSecondDownButton.setOnClickListener{
            if (remainingTime-60>0) {
                remainingTime -= 60
                updateTimerUI(remainingTime)
            }
            else showToast(this)
        }

        binding.secondsFirstDownButton.setOnClickListener{
            if (remainingTime-10>0) {
                remainingTime -= 10
                updateTimerUI(remainingTime)
            }
            else showToast(this)
        }

        binding.secondsSecondDownButton.setOnClickListener{
            if (remainingTime-1>0) {
                remainingTime -= 1
                updateTimerUI(remainingTime)
            }
            else showToast(this)
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
                remainingTime = p1.getIntExtra(TimerService.REMAINING_TIME, 0)

                updateButtonsUI(isTimerRunning)
                updateTimerUI(remainingTime)
            }
        }
        registerReceiver(statusReceiver, statusFilter)

        val timeFilter = IntentFilter()
        timeFilter.addAction(TimerService.TIMER_TICK)
        timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                remainingTime = p1?.getIntExtra(TimerService.REMAINING_TIME, 0)!!
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

    private fun showToast(context: Context){Toast.makeText(context, "Incorrect Value", Toast.LENGTH_SHORT).show() }

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
                binding.upButtons.visibility = View.INVISIBLE
                binding.downButtons.visibility = View.INVISIBLE
            }
            TIMER_PAUSED ->{
                binding.mainButton.text = getString(R.string.main_button_start)
                binding.restartIV.visibility = View.VISIBLE
                binding.upButtons.visibility = View.VISIBLE
                binding.downButtons.visibility = View.VISIBLE
            }
            else -> {
                binding.mainButton.text = getString(R.string.main_button_start)
                binding.restartIV.visibility = View.VISIBLE
                binding.upButtons.visibility = View.VISIBLE
                binding.downButtons.visibility = View.VISIBLE
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
        timerService.putExtra(TimerService.REMAINING_TIME, remainingTime)
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
        }
}