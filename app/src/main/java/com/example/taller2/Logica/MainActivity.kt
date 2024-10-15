package com.example.taller2.Logica

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taller2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.contactsButton.setOnClickListener{
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        binding.cameraButton.setOnClickListener{
            startActivity(Intent(this, CameraActivity::class.java))
        }

        binding.mapsButton.setOnClickListener{
            startActivity(Intent(this, MapsActivity::class.java))
        }
    }
}