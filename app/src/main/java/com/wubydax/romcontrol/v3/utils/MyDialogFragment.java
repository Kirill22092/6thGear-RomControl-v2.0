package com.wubydax.romcontrol.v3.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import android.preference.PreferenceManager;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wubydax.romcontrol.v3.BuildConfig;
import com.wubydax.romcontrol.v3.MyApp;
import com.wubydax.romcontrol.v3.R;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

public class MyDialogFragment extends DialogFragment implements View.OnClickListener {
    private int mRequestCode;
    private OnDialogFragmentListener mOnDialogFragmentListener;

    public static MyDialogFragment newInstance(int requestCode) {
        MyDialogFragment myDialogFragment = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.DIALOG_REQUEST_CODE_KEY, requestCode);
        myDialogFragment.setArguments(args);
        return myDialogFragment;
    }

    public static MyDialogFragment backupRestoreInstance(int requestCode,
                                                         boolean isConfirm, String filePath) {
        MyDialogFragment myDialogFragment = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.DIALOG_REQUEST_CODE_KEY, requestCode);
        args.putBoolean(Constants.DIALOG_RESTORE_IS_CONFIRM_REQUIRED, isConfirm);
        args.putString(Constants.BACKUP_FILE_PATH_EXTRA_KEY, filePath);
        myDialogFragment.setArguments(args);
        return myDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mRequestCode = getArguments().getInt(Constants.DIALOG_REQUEST_CODE_KEY);
        Context mContext = MyApp.getContext();
        switch (mRequestCode) {
            case Constants.REBOOT_MENU_DIALOG_REQUEST_CODE:
                setRetainInstance(false);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(mContext)) {
                        Toast toast = Toast.makeText(mContext, mContext.getResources()
                                .getString(R.string.permission_window), Toast.LENGTH_LONG);
                        toast.show();
                        Intent intent;
                        intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + mContext.getPackageName()));
                        Intent chooserIntent = Intent.createChooser(intent, "Open Settings");
                        startActivity(chooserIntent);
                        return getEmptyDialog();
                    } else return getRebootMenuDialog();
                } else return getRebootMenuDialog();
            case Constants.THEME_DIALOG_REQUEST_CODE:
                return getThemeChooserDialog();
            case Constants.CHANGELOG_DIALOG_REQUEST_CODE:
                return getChangelogDialog();
            case Constants.BACKUP_OR_RESTORE_DIALOG_REQUEST_CODE:
                return getBackupRestoreChooserDialog();
            case Constants.RESTORE_FILE_SELECTOR_DIALOG_REQUEST_CODE:
                if (!getArguments().getBoolean(Constants.DIALOG_RESTORE_IS_CONFIRM_REQUIRED)) {
                    return getRestoreFileSelectorDialog();
                } else {
                    return getRestoreConfirmDialog(getArguments()
                            .getString(Constants.BACKUP_FILE_PATH_EXTRA_KEY));
                }
            default:
                return super.onCreateDialog(savedInstanceState);
        }
    }

    private Dialog getRestoreConfirmDialog(final String filePath) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.restore_confirm_dialog_title)
                .setMessage(getText(R.string.restore_confirm_message))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (mOnDialogFragmentListener != null) {
                        mOnDialogFragmentListener.onRestoreRequested(filePath, true);
                    }
                })
                .create();
    }

    private Dialog getRestoreFileSelectorDialog() {
        File backupFolder = new File(Constants.BACKUP_FOLDER_PATH);
        final File[] backupFiles = backupFolder.listFiles();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        boolean isBackup = backupFolder.exists() && Objects.requireNonNull(backupFiles).length > 0;
        if (isBackup) {
            String[] items = new String[backupFiles.length];
            for (int i = 0; i < backupFiles.length; i++) {
                items[i] = backupFiles[i].getName();
            }
            builder.setTitle(R.string.choose_backup_dialog_title)
                    .setSingleChoiceItems(items, 0, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (mOnDialogFragmentListener != null) {
                            int checked = ((AlertDialog) dialog).getListView()
                                    .getCheckedItemPosition();
                            mOnDialogFragmentListener.onRestoreRequested(backupFiles[checked]
                                    .getAbsolutePath(), false);
                        }
                    });
        } else {
            builder.setTitle(R.string.no_backup_dialog_title)
                    .setMessage(R.string.no_backup_dialog_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        if (mOnDialogFragmentListener != null) {
                            mOnDialogFragmentListener.onBackupRestoreResult(0);
                        }
                    });
        }
        return builder.create();
    }

    private Dialog getBackupRestoreChooserDialog() {
        String[] singleChoiceItems = getActivity().getResources()
                .getStringArray(R.array.backup_restore_items);
        Drawable icon = ResourcesCompat.getDrawable(getActivity()
                .getResources(), R.drawable.ic_backup_restore, null);
        assert icon != null;
        icon.setColorFilter(getActivity().getResources().getColor(R.color.colorAccent),
                PorterDuff.Mode.SRC_ATOP);
        return new AlertDialog.Builder(getActivity())
                .setIcon(icon)
                .setTitle(R.string.backup_restore_dialog_title)
                .setSingleChoiceItems(singleChoiceItems, 0, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int checked = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (mOnDialogFragmentListener != null) {
                        mOnDialogFragmentListener.onBackupRestoreResult(checked);
                    }
                })
                .create();
    }

    private Dialog getEmptyDialog() {
        return new AlertDialog.Builder(getActivity())
                .create();
    }

    private Dialog getChangelogDialog() {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new MyAdapter(getActivity()));
        return new AlertDialog.Builder(getActivity())
                .setTitle(String.format(Locale.getDefault(), getString(R.string.changelog_version_title),
                        BuildConfig.VERSION_NAME))
                .setView(recyclerView)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private Dialog getThemeChooserDialog() {
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(MyApp.getContext());
        final int previouslySelected = sharedPreferences
                .getInt(Constants.THEME_PREF_KEY, getResources().getInteger(R.integer.default_theme));
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.theme_dialog_title)
                .setSingleChoiceItems(getActivity().getResources()
                        .getStringArray(R.array.theme_items), previouslySelected, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int checked = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (mOnDialogFragmentListener != null && previouslySelected != checked) {
                        sharedPreferences.edit().putInt(Constants.THEME_PREF_KEY, checked).apply();
                        mOnDialogFragmentListener.onDialogResult(mRequestCode);
                    }
                })
                .create();
    }

    private Dialog getRebootMenuDialog() {
        View view = LayoutInflater.from(new ContextThemeWrapper(MyApp.getContext(),
                R.style.AppThemeDark)).inflate(R.layout.reboot_layout, null);
        view.findViewById(R.id.rebootDevice).setOnClickListener(this);
        view.findViewById(R.id.rebootRecovery).setOnClickListener(this);
        view.findViewById(R.id.rebootUI).setOnClickListener(this);
        view.findViewById(R.id.protectiveView).setOnClickListener(this);
        Dialog dialog = new AlertDialog.Builder(getActivity(), R.style.RebootDialogTheme)
                .setView(view)
                .create();
        Objects.requireNonNull(dialog.getWindow()).setLayout(WindowManager
                .LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        Window window = dialog.getWindow();
        View decorView = window.getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
        window.setBackgroundDrawable(Utils.getDrawable(mOnDialogFragmentListener.getDecorView()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        else window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mOnDialogFragmentListener = (OnDialogFragmentListener) context;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rebootDevice) Utils.putCommand("su -c reboot");
        else if (id == R.id.rebootRecovery) Utils.putCommand("su -c reboot recovery");
        else if (id == R.id.rebootUI) Utils.killPackage("com.android.systemui");
        getDialog().dismiss();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mOnDialogFragmentListener = (OnDialogFragmentListener) activity;
    }

    @Override
    public void onPause() {
        this.dismiss();
        super.onPause();
    }

    public interface OnDialogFragmentListener {
        void onDialogResult(int requestCode);

        void onBackupRestoreResult(int which);

        void onRestoreRequested(String filePath, boolean isConfirmed);

        View getDecorView();
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        private final Context mContext;
        private final String[] mChangelogItems;

        MyAdapter(Context context) {
            mContext = context;
            mChangelogItems = mContext.getResources().getStringArray(R.array.changelog_items);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View mainView = LayoutInflater.from(mContext).inflate(R.layout.changelog_item, parent,
                    false);
            return new MyViewHolder(mainView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.mTextView.setText(mChangelogItems[position]);
        }

        @Override
        public int getItemCount() {
            return mChangelogItems != null ? mChangelogItems.length : 0;
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {
            final TextView mTextView;

            MyViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.changelogText);
            }
        }
    }
}