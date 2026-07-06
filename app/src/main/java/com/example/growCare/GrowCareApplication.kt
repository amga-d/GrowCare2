package com.example.growCare

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for GrowCare
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 */
@HiltAndroidApp
class GrowCareApplication : Application() {

}
