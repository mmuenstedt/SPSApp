package com.example.spstest

class Refresher(private val model: CountersModel) : Runnable {
    private var isRunning = false
    override fun run() {
        isRunning = true
        while (isRunning) {
            Thread.sleep(1000)
            model.refreshVerbrauch()
        }
    }

    fun stop() {
        isRunning = false
    }
}