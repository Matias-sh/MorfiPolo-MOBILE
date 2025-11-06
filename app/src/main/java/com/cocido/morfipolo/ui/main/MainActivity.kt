package com.cocido.morfipolo.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.setupWithNavController
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.ActivityMainBinding
import com.cocido.morfipolo.ui.login.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificar si está logueado
        if (!(application as MorfipoloApplication).userRepository.isLoggedIn()) {
            val intent = Intent(this, com.cocido.morfipolo.ui.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        val navView: BottomNavigationView = binding.navView
        
        // Esperar a que el FragmentContainerView esté completamente inicializado
        binding.root.post {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment
            val navController = navHostFragment?.navController ?: return@post

            // Configurar BottomNavigationView con NavController
            navView.setupWithNavController(navController)
        }
    }

}

