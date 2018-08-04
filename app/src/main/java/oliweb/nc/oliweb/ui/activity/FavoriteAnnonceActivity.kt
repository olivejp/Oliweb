package oliweb.nc.oliweb.ui.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import oliweb.nc.oliweb.R
import oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment
import oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ACTION_FAVORITE

class FavoriteAnnonceActivity : AppCompatActivity() {

    private val TAG = FavoriteAnnonceActivity::class.java.name

    companion object {
        const val ARG_USER_UID = "ARG_USER_UID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_annonce)
        if (intent.extras != null && intent.extras!!.containsKey(ARG_USER_UID) && intent.extras[ARG_USER_UID] != null) {
            val userUid = intent.extras!!.getString(ARG_USER_UID)
            val listAnnonceFragment = ListAnnonceFragment.getInstance(userUid, ACTION_FAVORITE)
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_annonce_favorite_activity, listAnnonceFragment)
                    .addToBackStack(null)
                    .commit()
        } else {
            Log.e(TAG, "FavoriteAnnonceActivity needs ARG_USER_UID to launch", RuntimeException("Missing argument"))
            finish()
        }
    }
}
