package com.flatcode.littleplayer.unit

import com.flatcode.littleplayer.Activity.AlbumDetailsActivity
import com.flatcode.littleplayer.Activity.MainActivity
import com.flatcode.littleplayer.Activity.PlayerActivity
import com.flatcode.littleplayer.Activity.SplashActivity

object CLASS {
    val MAIN: Class<*> = MainActivity::class.java
    val SPLASH: Class<*> = SplashActivity::class.java
    val PLAYER: Class<*> = PlayerActivity::class.java
    val ALBUM_DETAILS: Class<*> = AlbumDetailsActivity::class.java
}