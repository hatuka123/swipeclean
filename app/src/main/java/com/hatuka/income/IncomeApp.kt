package com.hatuka.income

import android.app.Application
import com.hatuka.income.data.CrashLog
import com.hatuka.income.data.Store

class IncomeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        Store.init(this)
    }
}
