package com.wubydax.romcontrol.v3;

import android.Manifest;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;

import com.google.android.material.navigation.NavigationView;

import android.preference.PreferenceManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.stericson.roottools.RootTools;
import com.wubydax.romcontrol.v3.utils.BackupRestoreIntentService;
import com.wubydax.romcontrol.v3.utils.Constants;
import com.wubydax.romcontrol.v3.utils.MyDialogFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        MyDialogFragment.OnDialogFragmentListener {

    private Runnable rootCheck;
    private Handler mHandler;
    private FragmentManager mFragmentManager;
    private SharedPreferences mSharedPreferences;
    private ArrayList<Integer> mNavMenuItemsIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int myTheme = mSharedPreferences.getInt(Constants.THEME_PREF_KEY, getResources().getInteger(R.integer.default_theme));
        int currentNightMode = getResources()
                .getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (myTheme) {
            case 0:
                switch (currentNightMode) {
                    case Configuration.UI_MODE_NIGHT_NO:
                        setTheme(R.style.AppThemeLight);
                        break;
                    case Configuration.UI_MODE_NIGHT_YES:
                        setTheme(R.style.AppThemeDark);
                        break;
                }
                break;
            case 1:
                setTheme(R.style.AppThemeLight);
                break;
            case 2:
                setTheme(R.style.AppThemeDark);
                break;
        }
        Window window = getWindow();
        mHandler = new Handler(Looper.getMainLooper());
        rootCheck = () -> {
            if (RootTools.isAccessGiven() || BuildConfig.DEBUG) {
                window.setNavigationBarColor(getResources().getColor(R.color.darkBgColor));
                setContentView(R.layout.activity_main);
                mFragmentManager = getFragmentManager();
                int lastFragmentIndex = mSharedPreferences
                        .getInt(Constants.LAST_FRAGMENT, 0);
                String[] titles = getResources()
                        .getStringArray(R.array.nav_menu_prefs_titles);
                if (savedInstanceState == null) {
                    String[] xmlNames = getResources()
                            .getStringArray(R.array.nav_menu_xml_file_names);
                    loadPrefsFragment(xmlNames[lastFragmentIndex]);
                }
                setTitle(titles[lastFragmentIndex]);
                initViews();
            } else {
                setContentView(R.layout.root_rq);
                window.setNavigationBarColor(ContextCompat
                        .getColor(this, R.color.colorPrimary));
            }
        };
        mHandler.post(rootCheck);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
    }

    public void retryRootCheck(View view) {
        mHandler.post(rootCheck);
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer,
                toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        assert drawer != null;
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);
        Menu navigationMenu = navigationView.getMenu();
        setUpPrefsMenu(navigationMenu);
    }

    private void setUpPrefsMenu(Menu navigationMenu) {
        String[] titles = getResources().getStringArray(R.array.nav_menu_prefs_titles);
        TypedArray iconIds = getResources().obtainTypedArray(R.array.nav_menu_prefs_drawables);
        mNavMenuItemsIds = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            int id = View.generateViewId();
            mNavMenuItemsIds.add(id);
            MenuItem item = navigationMenu.add(Menu.NONE, id, 0, titles[i])
                    .setIcon(iconIds.getResourceId(i, -1));
            if (titles[i] != null && titles[i].equals(getTitle().toString())) {
                item.setChecked(true);
            }
        }
        iconIds.recycle();
        navigationMenu.setGroupCheckable(Menu.NONE, true, true);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.rebootMenu) {
            mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance
                    (Constants.REBOOT_MENU_DIALOG_REQUEST_CODE), "reboot_dialog")
                    .commit();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (mNavMenuItemsIds.contains(id)) {
            int index = mNavMenuItemsIds.indexOf(id);
            loadPrefsFragment(getResources()
                    .getStringArray(R.array.nav_menu_xml_file_names)[index]);
            setTitle(item.getTitle().toString());
            mSharedPreferences.edit().putInt(Constants.LAST_FRAGMENT, index).apply();
        } else {
            if (id == R.id.themes) {
                mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance
                        (Constants.THEME_DIALOG_REQUEST_CODE), "theme_dialog")
                        .commit();
            } else if (id == R.id.changeLog) {
                mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance
                        (Constants.CHANGELOG_DIALOG_REQUEST_CODE), "changelog")
                        .commit();
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (getStorage()) {
                        mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance
                                        (Constants.BACKUP_OR_RESTORE_DIALOG_REQUEST_CODE),
                                "backup_restore")
                                .commit();
                    } else {
                        Toast toast = Toast.makeText(this, getResources()
                                .getString(R.string.permission_storage), Toast.LENGTH_LONG);
                        toast.show();
                    }
                } else mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance
                        (Constants.BACKUP_OR_RESTORE_DIALOG_REQUEST_CODE), "backup_restore")
                        .commit();
            }
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean getStorage() {
        int permissionStatus = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    0);
            return false;
        } else return true;
    }

    private void launchBackupRestoreService(int which, String filePath) {
        String action;
        Intent intent = new Intent(this, BackupRestoreIntentService.class);
        switch (which) {
            case 0:
                action = Constants.SERVICE_INTENT_ACTION_BACKUP;
                break;
            case 1:
                action = Constants.SERVICE_INTENT_ACTION_RESTORE;
                intent.putExtra(Constants.BACKUP_FILE_PATH_EXTRA_KEY, filePath);
                break;
            default:
                action = null;
        }
        if (action != null) {
            intent.setAction(action);
            startService(intent);
            if (which == 1) {
                finish();
            }
        }
    }

    private void loadPrefsFragment(String prefName) {
        mFragmentManager.beginTransaction().replace(R.id.fragment_container, PrefsFragment
                .newInstance(prefName)).commit();
    }


    @Override
    public void onDialogResult(int requestCode) {
        if (requestCode == Constants.THEME_DIALOG_REQUEST_CODE) {
            finish();
            this.overridePendingTransition(0, R.anim.fade_out);
            startActivity(new Intent(this, MainActivity.class));
            this.overridePendingTransition(R.anim.fade_in, 0);
        }
    }

    @Override
    public void onBackupRestoreResult(int which) {
        switch (which) {
            case 0:
                launchBackupRestoreService(which, null);
                break;
            case 1:
                mFragmentManager.beginTransaction().add(MyDialogFragment
                                .newInstance(Constants.RESTORE_FILE_SELECTOR_DIALOG_REQUEST_CODE),
                        "restore_selector").commit();
                break;
        }
    }

    @Override
    protected void onPause() {
        Runtime.getRuntime().gc();
        super.onPause();
    }

    @Override
    public void onRestoreRequested(String filePath, boolean isConfirmed) {
        if (isConfirmed) {
            launchBackupRestoreService(1, filePath);
        } else {
            mFragmentManager.beginTransaction().add(MyDialogFragment
                    .backupRestoreInstance(Constants.RESTORE_FILE_SELECTOR_DIALOG_REQUEST_CODE,
                            true, filePath), "restore_confirm").commit();
        }
    }

    @Override
    public View getDecorView() {
        return getWindow().getDecorView();
    }
}