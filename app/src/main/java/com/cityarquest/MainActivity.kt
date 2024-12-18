package com.cityarquest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.cityarquest.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val mapFragment = MapFragment.newInstance()
    private val completedFragment = CompletedQuestsFragment.newInstance()
    private val profileFragment = ProfileFragment.newInstance()

    private var currentMainFragment: Fragment = mapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(binding.mainFragmentContainer.id, mapFragment, "MapFragment")
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_map -> {
                    showMainFragment(mapFragment)
                    true
                }
                R.id.nav_completed -> {
                    showMainFragment(completedFragment)
                    true
                }
                R.id.nav_profile -> {
                    showMainFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun showMainFragment(fragment: Fragment) {
        if (fragment != currentMainFragment) {
            supportFragmentManager.commit {
                replace(binding.mainFragmentContainer.id, fragment)
            }
            currentMainFragment = fragment
        }
        // Сброс BackStack при переходе между главными вкладками можно добавить при необходимости.
    }

    fun navigateToQuestDetail(questId: String) {
        openFragment(QuestDetailFragment.newInstance(questId))
    }

    fun navigateToARView(questId: String) {
        openFragment(ARViewFragment.newInstance(questId))
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(binding.mainFragmentContainer.id, fragment)
            addToBackStack(null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_help) {
            // Открываем HelpFragment
            openFragment(HelpFragment())
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
