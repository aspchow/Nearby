package com.avinash.nearby.di

import android.app.Activity
import androidx.activity.ComponentActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

/**
 * Created by Avinash Munnangi on 02/07/25.
 */

@Module
@InstallIn(ActivityComponent::class)
class PermissionModule {

    @Provides
    @ActivityScoped
    fun provideComponentActivity(activity: Activity): ComponentActivity {
        return activity as ComponentActivity
    }
}