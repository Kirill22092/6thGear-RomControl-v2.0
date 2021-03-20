package com.wubydax.romcontrol.v3.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.stericson.rootshell.exceptions.RootDeniedException;
import com.stericson.rootshell.execution.Command;
import com.stericson.roottools.RootTools;
import com.wubydax.romcontrol.v3.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static com.wubydax.romcontrol.v3.MyApp.getContext;


@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class Utils {


    private static final String LOG_TAG = "RomControlUtils";


    static void copyAssetFolder() {
        try {
            String[] scriptsInAssets = getContext().getAssets().list(Constants.SCRIPTS_FOLDER);
            Log.d(LOG_TAG, "copyAssetFolder " + Objects.requireNonNull(scriptsInAssets)[0]);
            File scriptsFilesDir = new File(Constants.FILES_SCRIPTS_FOLDER_PATH);
            if (!scriptsFilesDir.exists()) {
                new File(Constants.FILES_SCRIPTS_FOLDER_PATH).mkdirs();
            }
            for (String file : scriptsInAssets) {
                Log.d(LOG_TAG, "copyAssetFolder " + file);
                if (file.contains(".")) {
                    copyAsset(scriptsInAssets, Constants.SCRIPTS_FOLDER + File.separator
                            + file, Constants.FILES_SCRIPTS_FOLDER_PATH + File.separator +
                            file);
                } else {
                    copyAssetFolder();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void copyAsset(String[] scriptsInAssets, String from, String to) {
        boolean isCopied = false;
        InputStream in;
        OutputStream out;
        ArrayList<File> scriptsFiles = new ArrayList<>();
        for (String scriptsInAsset : scriptsInAssets) {
            File f = new File(Constants.FILES_SCRIPTS_FOLDER_PATH + File.separator
                    + scriptsInAsset);
            scriptsFiles.add(f);
        }
        for (int j = 0; j < scriptsFiles.size(); j++) {
            if (!scriptsFiles.get(j).exists()) {
                try {
                    in = getContext().getAssets().open(from);
                    new File(to).createNewFile();
                    out = new FileOutputStream(to);
                    copyFile(in, out);
                    in.close();
                    out.flush();
                    out.close();
                    isCopied = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    isCopied = false;
                }
            }
        }
        if (isCopied) {
            Utils.putCommand("chmod -R 755 ");
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static Drawable getIconDrawable(Uri uri) {
        Drawable drawable = null;
        if (uri != null) {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                        bitmap.getWidth() / 5, bitmap.getHeight() / 5,
                        false);
                drawable = new BitmapDrawable(getContext().getResources(), scaledBitmap);
                bitmap.recycle();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return drawable;
    }

    static Drawable getDrawable(View rootView) {
        float scaleFactor = 8;
        int radius = 5;
        Bitmap bitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        rootView.draw(canvas);
        Bitmap overlay = Bitmap.createBitmap((int) (rootView.getMeasuredWidth() / scaleFactor),
                (int) (rootView.getMeasuredHeight() / scaleFactor), Bitmap.Config.ARGB_8888);
        Canvas canvasOverlay = new Canvas(overlay);
        canvasOverlay.translate(-rootView.getLeft() / scaleFactor, -rootView.getTop() /
                scaleFactor);
        canvasOverlay.scale(1 / scaleFactor, 1 / scaleFactor);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvasOverlay.drawBitmap(bitmap, 0, 0, paint);
        overlay = FastBlur.doBlur(overlay, radius, true);
        return new BitmapDrawable(getContext().getResources(), overlay);
    }

    public static void showKillPackageDialog(final String packageName, Context context) {
        try {
            ApplicationInfo applicationInfo = getContext().getPackageManager()
                    .getApplicationInfo(packageName, 0);
            String appLabel = applicationInfo.loadLabel(getContext().getPackageManager())
                    .toString();
            Drawable appIcon = applicationInfo.loadIcon(getContext().getPackageManager());
            new AlertDialog.Builder(context)
                    .setTitle(R.string.app_reboot_required_dialog_title)
                    .setMessage(String.format(Locale.getDefault(), getContext()
                            .getString(R.string.app_reboot_required_dialog_message), appLabel))
                    .setIcon(appIcon)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            killPackage(packageName))
                    .create().show();
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(getContext(), "App not installed", Toast.LENGTH_SHORT).show();
        }
    }

    public static void showRebootRequiredDialog(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.reboot_required_dialog_title)
                .setMessage(R.string.reboot_required_dialog_message)
                .setNegativeButton(R.string.reboot_later_negative_button, null)
                .setPositiveButton(R.string.reboot_now_dialog_button, (dialogInterface, i) ->
                        Utils.putCommand("su -c reboot"))
                .create()
                .show();
    }


    public static boolean isPackageInstalled(String packageName) {
        try {
            getContext().getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void killPackage(String packageNameToKill) {
        Utils.putCommand("pkill -f " + packageNameToKill);
    }

    public static void putKey(String commandPut) {
        Utils.putCommand("settings put system " + commandPut);
    }

    public static void putCommand(String commandPut) {
        Runnable runnable = () -> {
            Command command = new Command(0, commandPut);
            try {
                RootTools.getShell(true).add(command);
            } catch (IOException | TimeoutException | RootDeniedException e) {
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(runnable);
        thread.setPriority(9);
        thread.start();
    }
}
