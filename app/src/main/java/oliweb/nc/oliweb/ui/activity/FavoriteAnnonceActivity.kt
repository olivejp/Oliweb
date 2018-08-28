package oliweb.nc.oliweb.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.util.Pair
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import oliweb.nc.oliweb.R
import oliweb.nc.oliweb.service.sharing.DynamicLynksGenerator
import oliweb.nc.oliweb.ui.activity.MainActivity.RC_SIGN_IN
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment
import oliweb.nc.oliweb.ui.fragment.ListFavoritesFragment
import oliweb.nc.oliweb.utility.Utility

class FavoriteAnnonceActivity : AppCompatActivity() {

    private val TAG = FavoriteAnnonceActivity::class.java.name

    companion object {
        const val ARG_USER_UID = "ARG_USER_UID"
    }

    /**
     * OnClickListener that should open AnnonceDetailActivity
     */
    private val onClickListener = { v: View ->
        val viewHolderBeauty = v.tag as AnnonceBeautyAdapter.ViewHolderBeauty
        val intent = Intent(appCompatActivity, AnnonceDetailActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable(ARG_ANNONCE, viewHolderBeauty.annoncePhotos)
        intent.putExtras(bundle)
        val pairImage = Pair<View, String>(viewHolderBeauty.imageView, getString(R.string.image_detail_transition))
        val options = makeSceneTransitionAnimation(appCompatActivity, pairImage)
        startActivity(intent, options.toBundle())
    }

    /**
     * OnClickListener that share an annonce with a DynamicLink
     */
    private val onClickListenerShare = { v ->
        if (uidUser != null && !uidUser.isEmpty()) {
            val viewHolder = v.getTag() as AnnonceBeautyAdapter.ViewHolderBeauty
            val annoncePhotos = viewHolder.annoncePhotos
            val annonceEntity = annoncePhotos.getAnnonce()

            // Display a loading spinner
            loadingDialogFragment = LoadingDialogFragment()
            loadingDialogFragment.setText(getString(R.string.dynamic_link_creation))
            loadingDialogFragment.show(appCompatActivity.getSupportFragmentManager(), LOADING_DIALOG)

            DynamicLynksGenerator.generateShortLink(uidUser, annonceEntity, annoncePhotos.photos, object : DynamicLynksGenerator.DynamicLinkListener {
                override fun getLink(shortLink: Uri, flowchartLink: Uri) {
                    loadingDialogFragment.dismiss()
                    val sendIntent = Intent()
                    val msg = getString(R.string.default_text_share_link) + shortLink
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(Intent.EXTRA_TEXT, msg)
                    sendIntent.type = "text/plain"
                    startActivity(sendIntent)
                }

                override fun getLinkError() {
                    loadingDialogFragment.dismiss()
                    Snackbar.make(coordinatorLayout, R.string.link_share_error, Snackbar.LENGTH_LONG).show()
                }
            })
        } else {
            Snackbar.make(coordinatorLayout, R.string.sign_in_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_in) { v1 -> Utility.signIn(appCompatActivity, RC_SIGN_IN) }
                    .show()
        }
    }

    private val onClickListenerFavorite = { v: View ->
        if (uidUser == null || uidUser.isEmpty()) {
            Snackbar.make(coordinatorLayout, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.sign_in)) { v1 -> Utility.signIn(appCompatActivity, RC_SIGN_IN) }
                    .show()
        } else {
            val viewHolder = v.tag as AnnonceBeautyAdapter.ViewHolderBeauty
            viewModel.removeFromFavorite(FirebaseAuth.getInstance().uid, viewHolder.annoncePhotos)
                    .observeOnce({ isRemoved ->
                        if (isRemoved != null) {
                            when (isRemoved) {
                                SearchActivityViewModel.AddRemoveFromFavorite.REMOVE_SUCCESSFUL -> Snackbar.make(recyclerView, R.string.annonce_remove_from_favorite, Snackbar.LENGTH_LONG).show()
                                SearchActivityViewModel.AddRemoveFromFavorite.REMOVE_FAILED -> Toast.makeText(appCompatActivity, R.string.remove_from_favorite_failed, Toast.LENGTH_LONG).show()
                                else -> {
                                }
                            }
                        }
                    })
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_annonce)
        if (intent.extras != null && intent.extras!!.containsKey(ARG_USER_UID) && intent.extras[ARG_USER_UID] != null) {
            val userUid = intent.extras!!.getString(ARG_USER_UID)
            val listFavoritesFragment = ListFavoritesFragment.getInstance(userUid)
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_annonce_favorite_activity, listFavoritesFragment)
                    .addToBackStack(null)
                    .commit()
        } else {
            Log.e(TAG, "FavoriteAnnonceActivity needs ARG_USER_UID to launch", RuntimeException("Missing argument"))
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
