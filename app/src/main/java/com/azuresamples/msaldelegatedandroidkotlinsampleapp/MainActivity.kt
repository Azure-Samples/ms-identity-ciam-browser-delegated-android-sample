package com.azuresamples.msaldelegatedandroidkotlinsampleapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.azuresamples.msaldelegatedandroidkotlinsampleapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AuthClient.initialize(this@MainActivity)


    }
}
