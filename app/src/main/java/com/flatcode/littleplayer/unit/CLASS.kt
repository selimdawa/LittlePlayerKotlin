package com.flatcode.littleplayer.unit

import com.flatcode.littleplayer.activity.AlbumDetailsActivity
import com.flatcode.littleplayer.activity.MainActivity
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.activity.SplashActivity

object CLASS {
    val MAIN: Class<*> = MainActivity::class.java
    val SPLASH: Class<*> = SplashActivity::class.java
    val PLAYER: Class<*> = PlayerActivity::class.java
    val ALBUM_DETAILS: Class<*> = AlbumDetailsActivity::class.java
}