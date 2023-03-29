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
import androidx.constraintlayout.widget.Group
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
            if (remainingTime==0) showToast(this, "Incorrect time value")
            else {
                if(isTimerRunning != TIMER_RUNNING) startTimer() else pauseTimer()
            }
        }


        binding.restartIV.setOnClickListener {
            remainingTime = 0
            resetTimer()
            binding.mainButton.visibility = View.VISIBLE
        }

        binding.upButtons.referencedIds.forEach { id ->
            findViewById<View>(id).setOnClickListener{
                var totalTime = remainingTime
                when (it.id){
                    binding.minutesFirstUpButton.id -> totalTime += 600
                    binding.minutesSecondUpButton.id -> totalTime += 60
                    binding.secondsFirstUpButton.id -> totalTime += 10
                    binding.secondsSecondUpButton.id -> totalTime += 1
                }
                if (totalTime<=anHour){
                    remainingTime = totalTime
                    updateTimerUI(remainingTime)
                    updateButtonsUI(isTimerRunning)
                }
            }
        }

        binding.downButtons.referencedIds.forEach { id ->
            findViewById<View>(id).setOnClickListener{
                var totalTime = remainingTime
                when (it.id){
                    binding.minutesFirstDownButton.id -> totalTime -= 600
                    binding.minutesSecondDownButton.id -> totalTime -= 60
                    binding.secondsFirstDownButton.id -> totalTime -= 10
                    binding.secondsSecondDownButton.id -> totalTime -= 1
                }
                if (totalTime>=0){
                    remainingTime = totalTime
                    updateTimerUI(remainingTime)
                    updateButtonsUI(isTimerRunning)
                }
            }
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

    private fun showToast(context: Context, text: String){Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }

    private fun updateTimerUI(remainingTime: Int) {
        val minutes: Int = remainingTime / 60
        val seconds: Int = remainingTime % 60
        binding.timerValue.text =
            "${"%02d".format(minutes)}:${"%02d".format(seconds)}"
    }

    private fun updateButtonsUI(isTimerRunning: String) {
        if (isTimerRunning!= TIMER_ENDED) {
            binding.upButtons.visibility = View.INVISIBLE
            binding.downButtons.visibility = View.INVISIBLE
        }else{
            binding.upButtons.visibility = View.VISIBLE
            binding.downButtons.visibility = View.VISIBLE
        }
        if (isTimerRunning == TIMER_RUNNING) {
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
        timerService.putExtra(TimerService.TIMER_ACTION, TimerService.ENDED)
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