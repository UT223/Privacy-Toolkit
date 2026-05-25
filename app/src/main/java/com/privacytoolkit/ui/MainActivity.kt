package com.privacytoolkit.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.privacytoolkit.R
import com.privacytoolkit.databinding.ActivityMainBinding

/**
 * Single-Activity host that owns the NavHostFragment and BottomNavigationView.
 * All screens are Fragments navigated via the Navigation Component.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        // Top-level destinations (no Up button shown)
        appBarConfig = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.appsFragment,
                R.id.wifiFragment,
                R.id.qrFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfig)
        binding.bottomNav.setupWithNavController(navController)

        // Keep the bottom nav visible only on top-level screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevel = appBarConfig.topLevelDestinations.contains(destination.id)
            binding.bottomNav.visibility =
                if (topLevel) BottomNavigationView.VISIBLE else BottomNavigationView.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                navController.navigate(R.id.aboutFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
