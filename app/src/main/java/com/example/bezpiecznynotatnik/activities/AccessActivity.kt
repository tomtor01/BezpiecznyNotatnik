package com.example.bezpiecznynotatnik.activities

import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.utils.LocaleHelper
import com.example.bezpiecznynotatnik.utils.NavigationController
import com.example.bezpiecznynotatnik.utils.PreferenceHelper

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

import java.util.Locale

class AccessActivity : NavigationController() {

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(null)
            return
        }
        val language = PreferenceHelper.getLanguage(newBase) ?: Locale.getDefault().language
        val context = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access)
        setupNavigationView()

        val topAppBar: AppBarLayout = findViewById(R.id.appBarLayout)
        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Link BottomNavigationView with NavController
        bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, _, _ ->
            topAppBar.setExpanded(true, true)
        }
    }

    override fun onStop() {
        super.onStop()
        (application as SecureNotesApp).scheduleLogoutWork()
    }
    override fun onStart() {
        super.onStart()
        (application as SecureNotesApp).cancelLogoutWork()
    }
    override fun onRestart() {
        super.onRestart()
        (application as SecureNotesApp).cancelLogoutWork()
    }
    override fun onResume() {
        super.onResume()
        (application as SecureNotesApp).cancelLogoutWork()
    }
    override fun onDestroy() {
        super.onDestroy()
        (application as SecureNotesApp).cancelLogoutWork()
    }
    override fun onPause() {
        super.onPause()
        (application as SecureNotesApp).cancelLogoutWork()
    }
}
