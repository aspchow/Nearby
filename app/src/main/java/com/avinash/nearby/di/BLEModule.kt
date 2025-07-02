package com.avinash.nearby.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by Avinash Munnangi on 01/07/25.
 */
@InstallIn(SingletonComponent::class)
@Module
class BLEModule {

    @Singleton
    @Provides
    fun provideBluetoothManager(@ApplicationContext context : Context): BluetoothManager {
        return context.getSystemService(BluetoothManager::class.java) as BluetoothManager
    }

    @Singleton
    @Provides
    fun provideBluetoothAdapter(bluetoothManager : BluetoothManager): BluetoothAdapter {
        return bluetoothManager.adapter
    }

    @Provides
    fun provideScanner(adapter: BluetoothAdapter): BluetoothLeScanner {
        return adapter.bluetoothLeScanner
    }
}