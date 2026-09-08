package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.pharmacy.app.MainActivity as PharmacyMainActivity

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val intent = Intent(this, PharmacyMainActivity::class.java)
    startActivity(intent)
    finish()
  }
}
