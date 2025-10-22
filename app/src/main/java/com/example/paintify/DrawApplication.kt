package com.example.paintify
import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import com.example.paintify.data.AppDatabase
import com.example.paintify.data.DrawingRepository
/**

Assignment 3
Author: Nolan Mai - u1492442
Date: Oct 15, 2025*
Application class for initializing global app dependencies.*/
class DrawApplication : Application() {
    val scope = CoroutineScope(SupervisorJob())

    //get a reference to the DB singleton
    val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "drawing_database"
        ).build()
    }
    val drawingRepository by lazy { DrawingRepository(this, scope, db.drawingDao()) }
}