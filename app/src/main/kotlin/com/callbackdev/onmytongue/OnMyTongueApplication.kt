package com.callbackdev.onmytongue

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The application: Hilt's graph root. Nothing ever runs on a timer: the game has no service and
 * no background work of its own (PLANNING.md §9).
 */
@HiltAndroidApp
class OnMyTongueApplication : Application()
