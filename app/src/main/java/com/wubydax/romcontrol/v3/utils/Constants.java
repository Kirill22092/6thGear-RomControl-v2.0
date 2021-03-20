package com.wubydax.romcontrol.v3.utils;

import com.wubydax.romcontrol.v3.MyApp;

import java.io.File;
import java.util.Objects;

import static com.wubydax.romcontrol.v3.MyApp.getContext;

public class Constants {

    static final String BACKUP_FOLDER_PATH = Objects.requireNonNull(getContext().
            getExternalFilesDir(null)).getAbsolutePath() + File.separator +
            "RomControl" + File.separator + "Backups";
    static final String DIALOG_REQUEST_CODE_KEY = "dialog_request_code";
    static final String DIALOG_RESTORE_IS_CONFIRM_REQUIRED = "is_confirm";
    static final String SCRIPTS_FOLDER = "scripts";
    static final String SHARED_PREFS_FOLDER_PATH = MyApp.getContext().getFilesDir().getParent() +
            File.separator + "shared_prefs";

    public static final String BACKUP_FILE_PATH_EXTRA_KEY = "file_path";
    public static final String FILES_SCRIPTS_FOLDER_PATH = MyApp.getContext().getFilesDir()
            .getPath() + File.separator + SCRIPTS_FOLDER;
    public static final String LAST_FRAGMENT = "last_fragment_used";
    public static final String PREF_NAME_KEY = "pref_key";
    public static final String SERVICE_INTENT_ACTION_BACKUP = "com.wubydax.action.BACKUP";
    public static final String SERVICE_INTENT_ACTION_RESTORE = "com.wubydax.action.RESTORE";
    public static final String THEME_PREF_KEY = "theme_pref";

    public static final int BACKUP_OR_RESTORE_DIALOG_REQUEST_CODE = 26;
    public static final int CHANGELOG_DIALOG_REQUEST_CODE = 25;
    public static final int REBOOT_MENU_DIALOG_REQUEST_CODE = 58;
    public static final int RESTORE_FILE_SELECTOR_DIALOG_REQUEST_CODE = 65;
    public static final int THEME_DIALOG_REQUEST_CODE = 29;

}

