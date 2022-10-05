package ru.dartx.wordcards.activities

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import ru.dartx.wordcards.BuildConfig
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityMainBinding
import ru.dartx.wordcards.databinding.NavHeaderBinding
import ru.dartx.wordcards.db.CardAdapter
import ru.dartx.wordcards.db.MainViewModel
import ru.dartx.wordcards.dialogs.AboutAppDialog
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.settings.SettingsActivity
import ru.dartx.wordcards.utils.BitmapManager
import ru.dartx.wordcards.utils.LanguagesManager
import ru.dartx.wordcards.utils.ThemeManager
import ru.dartx.wordcards.workers.NotificationsWorker
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(), CardAdapter.Listener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var nvBinding: NavHeaderBinding
    private var edSearch: EditText? = null
    private var adapter: CardAdapter? = null
    private var textWatcher: TextWatcher? = null
    private var singInLauncher: ActivityResultLauncher<Intent>? = null
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((applicationContext as MainApp).database)
    }
    private lateinit var defPreference: SharedPreferences
    private var currentTheme = ""
    private var currentHideSignButtonState = false


    override fun onCreate(savedInstanceState: Bundle?) {
        defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        currentTheme = defPreference.getString("theme", "blue").toString()
        currentHideSignButtonState = defPreference.getBoolean("hide_login_button", false)
        setTheme(ThemeManager.getSelectedThemeNoBar(this))
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        nvBinding = NavHeaderBinding.bind(binding.navView.getHeaderView(0))
        setContentView(binding.root)
        startWorker()
        init()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            Log.d("DArtX", "First account: ${account.account}")
            nvBinding.btSignIn.text = getString(R.string.sign_out)
            googleDriveClient(account)
        }
        if (!defPreference.getBoolean(
                "hide_login_button",
                false
            ) || (defPreference.getBoolean(
                "sign_in_state",
                false
            ) && account == null)
        ) {
            nvBinding.btSignIn.visibility = View.VISIBLE
            googleSignIn()
            signInLauncher()
        } else {
            nvBinding.btSignIn.visibility = View.GONE
        }
        showHTU()
        cardListObserver()

        binding.btFab.setOnClickListener {
            val i = Intent(this, CardActivity::class.java)
            startActivity(i)
        }
    }

    override fun onResume() {
        super.onResume()
        LanguagesManager.getUsedLanguages(applicationContext)
        if (defPreference.getString(
                "theme", "blue"
            ) != currentTheme ||
            defPreference.getBoolean(
                "hide_login_button", false
            ) != currentHideSignButtonState
        ) recreate()
    }

    private fun expandActionView(): MenuItem.OnActionExpandListener {
        return object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                edSearch?.addTextChangedListener(textWatcher)
                searchListObserver()
                mainViewModel.allCards.removeObservers(this@MainActivity)
                mainViewModel.searchCard("%%")
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                edSearch?.removeTextChangedListener(textWatcher)
                edSearch?.setText("")
                invalidateOptionsMenu()
                mainViewModel.foundCards.removeObservers(this@MainActivity)
                cardListObserver()
                return true
            }
        }
    }

    private fun init() = with(binding) {
        rcViewCardList.layoutManager = LinearLayoutManager(this@MainActivity)
        adapter = CardAdapter(this@MainActivity)
        rcViewCardList.adapter = adapter
        toolbar.setNavigationOnClickListener {
            val name = defPreference.getString("user_name", "")
            if (!name.isNullOrEmpty()) {
                val statsHeaderText: String = name + getString(R.string.stats_header_with_name)
                nvBinding.tvStatsHeader.text = statsHeaderText
            }
            nvBinding.tvStats.text = statsCount()
            val imageS = defPreference.getString("avatar", "")
            if (!imageS.isNullOrEmpty()) {
                val imageB = BitmapManager.decodeToBase64(imageS)
                nvBinding.ivAvatar.setImageBitmap(imageB)
            } else nvBinding.ivAvatar.setImageResource(R.drawable.ic_avatar)
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.settings -> startActivity(
                    Intent(
                        this@MainActivity,
                        SettingsActivity::class.java
                    )
                )
                R.id.faq -> startActivity(
                    Intent(
                        this@MainActivity,
                        HowToUseActivity::class.java
                    )
                )
                R.id.donate -> {
                    val uriUrl = Uri.parse(getString(R.string.donate_url))
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            uriUrl
                        )
                    )
                }
                R.id.about -> {
                    val message = BuildConfig.VERSION_NAME
                    AboutAppDialog.showDialog(this@MainActivity, message)
                }
            }
            drawerLayout.close()
            true
        }
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                if (binding.drawerLayout.isDrawerVisible(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(
                this@MainActivity,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (binding.drawerLayout.isDrawerVisible(GravityCompat.START)) {
                            binding.drawerLayout.closeDrawer(GravityCompat.START)
                        } else finish()
                    }

                })
        }
        toolbar.inflateMenu(R.menu.top_menu)
        val search = toolbar.menu.findItem(R.id.search)
        edSearch = search.actionView?.findViewById(R.id.edSearch) as EditText
        search.setOnActionExpandListener(expandActionView())
        textWatcher = textWatcher()
    }

    private fun signInLauncher() {
        singInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                val acc = GoogleSignIn.getLastSignedInAccount(this)
                if (acc != null) {
                    thread {
                        val editor = defPreference.edit()
                        editor.putString("user_name", acc.displayName)
                        editor.putBoolean("hide_login_button", true)
                        editor.putBoolean("sign_in_state", true)
                        if (acc.photoUrl != null) {
                            try {
                                val stream =
                                    java.net.URL(acc.photoUrl!!.toString()).openStream()
                                val realImage: Bitmap = BitmapFactory.decodeStream(stream)
                                editor.putString("avatar", BitmapManager.encodeToBase64(realImage))
                            } catch (e: FileNotFoundException) {
                                e.printStackTrace()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                        editor.apply()
                        currentHideSignButtonState = true
                        googleDriveClient(acc)
                    }
                    nvBinding.btSignIn.text = getString(R.string.sign_out)
                    nvBinding.btSignIn.visibility = View.GONE
                    binding.drawerLayout.close()
                }
            }
        }
    }

    private fun googleSignIn() {
        nvBinding.btSignIn.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
                .build()
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            val tempAccount = GoogleSignIn.getLastSignedInAccount(this)
            if (tempAccount == null && singInLauncher != null) {
                val signInIntent = mGoogleSignInClient.signInIntent
                singInLauncher!!.launch(signInIntent)
            } else if (tempAccount != null) {
                mGoogleSignInClient.signOut()
                val editor = defPreference.edit()
                editor.putString("user_name", "")
                editor.putString("avatar", "")
                editor.putBoolean("hide_login_button", false)
                editor.putBoolean("sign_in_state", false)
                editor.apply()
                nvBinding.btSignIn.text = getString(R.string.sign_in)
                nvBinding.btSignIn.visibility = View.VISIBLE
                currentHideSignButtonState = false
                binding.drawerLayout.close()
            }
        }
    }

    private fun googleDriveClient(account: GoogleSignInAccount) {
        Log.d("DArtX", "Before Click")
        val credentials = GoogleAccountCredential.usingOAuth2(this, listOf(DriveScopes.DRIVE_FILE))
        credentials.selectedAccount = account.account
        Log.d("DArtX", "Account: ${account.account}")
        val googleDriveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credentials
        )
            .setApplicationName(getString(R.string.app_name))
            .build()
        if (googleDriveService != null) {
            nvBinding.btUpload.setOnClickListener {
                Log.d("DArtX", "Click")
                upload(googleDriveService)
            }
        }
    }

    private fun upload(googleDriveService: Drive) {
        Log.d("DArtX", "Upload")
        val dbPath = getString(R.string.db_path)
        val dbPathShm = getString(R.string.db_path_shm)
        val dbPathWal = getString(R.string.db_path_wal)

        val storageFile = com.google.api.services.drive.model.File()
        storageFile.parents = Collections.singletonList("appDataFolder")
        storageFile.name = "wordcards.db"
        val storageFileShm = com.google.api.services.drive.model.File()
        storageFileShm.parents = Collections.singletonList("appDataFolder")
        storageFileShm.name = "wordcards.db-shm"
        val storageFileWal = com.google.api.services.drive.model.File()
        storageFileWal.parents = Collections.singletonList("appDataFolder")
        storageFileWal.name = "wordcards.db-wal"

        val filePath = java.io.File(dbPath)
        val filePathShm = java.io.File(dbPathShm)
        val filePathWal = java.io.File(dbPathWal)
        val mediaContent = FileContent("", filePath)
        val mediaContentShm = FileContent("", filePathShm)
        val mediaContentWal = FileContent("", filePathWal)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DArtX", "Try upload")
                val file =
                    googleDriveService.files().create(storageFile, mediaContent).setFields("id")
                        .execute()
                println("Filename: " + file.id)
                val fileShm =
                    googleDriveService.files().create(storageFileShm, mediaContentShm).setFields("id")
                        .execute()
                println("Filename: " + fileShm.id)
                val fileWal =
                    googleDriveService.files().create(storageFileWal, mediaContentWal).setFields("id")
                        .execute()
                println("Filename: " + fileWal.id)
            } catch (e: GoogleJsonResponseException) {
                Log.d("DArtX", "Try e1")
                println("Unable upload: " + e.details)
                throw e
            }
        }
    }

    private fun cardListObserver() {
        mainViewModel.allCards.observe(this) {
            adapter?.submitList(it)
        }
    }

    private fun searchListObserver() {
        mainViewModel.foundCards.observe(this) {
            val tempCardList = ArrayList<Card>()
            it.forEach { card ->
                tempCardList.add(card)
            }
            adapter?.submitList(tempCardList)
        }
    }

    private fun statsCount(): String {
        val tempCardList = mainViewModel.allCards.value
        var count = 0
        tempCardList?.forEach { card ->
            if (card.step == 9) count++
        }
        return if (tempCardList != null) {
            "$count ${getString(R.string.of)} ${
                resources.getQuantityString(
                    R.plurals.words,
                    tempCardList.size,
                    tempCardList.size
                )
            }"
        } else ""
    }

    override fun onClickCard(card: Card) {
        val i = Intent(this, CardActivity::class.java)
        i.putExtra(CARD_DATA, card)
        startActivity(i)
    }

    private fun textWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mainViewModel.searchCard("%$s%")
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }
    }

    private fun showHTU() {
        if (!defPreference.getBoolean("not_show_htu", false)) {
            startActivity(
                Intent(
                    this@MainActivity,
                    HowToUseActivity::class.java
                )
            )
        }
    }

    private fun startWorker() {
        val notificationsRequest =
            PeriodicWorkRequestBuilder<NotificationsWorker>(
                defPreference.getString(
                    "notifications_repeat_time",
                    "15"
                )!!.toLong(), TimeUnit.MINUTES
            )
                .addTag("notifications")
                .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationsRequest
        )
    }

    companion object {
        const val CARD_DATA = "card"
        const val CHANNEL_ID = "wordCH"
    }
}