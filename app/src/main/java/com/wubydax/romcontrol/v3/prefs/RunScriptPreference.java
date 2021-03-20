package com.wubydax.romcontrol.v3.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Toast;

import com.stericson.rootshell.exceptions.RootDeniedException;
import com.stericson.rootshell.execution.Command;
import com.stericson.roottools.RootTools;
import com.wubydax.romcontrol.v3.R;
import com.wubydax.romcontrol.v3.utils.Constants;
import com.wubydax.romcontrol.v3.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;


public class RunScriptPreference extends Preference {
    private final String mPackageToKill;
    private final boolean mIsSilent;
    private final String mReverseDependencyKey;
    private final String mFilePath;
    private boolean mIsConfirmRequired;
    private int mRebootType;

    public RunScriptPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.RunScriptPreference);
        String scriptName = typedArray.getString(R.styleable.RunScriptPreference_scriptFileName);
        mFilePath = Constants.FILES_SCRIPTS_FOLDER_PATH + File.separator + scriptName;
        mIsConfirmRequired = typedArray
                .getBoolean(R.styleable.RunScriptPreference_showConfirmDialog, true);
        TypedArray generalTypedArray = context
                .obtainStyledAttributes(attrs, R.styleable.Preference);
        mPackageToKill = generalTypedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = generalTypedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mReverseDependencyKey = generalTypedArray
                .getString(R.styleable.Preference_reverseDependency);
        if (typedArray.hasValue(R.styleable.RunScriptPreference_rebootOptions)) {
            mRebootType = typedArray
                    .getInt(R.styleable.RunScriptPreference_rebootOptions, 0);
            if (mRebootType == 2) {
                mIsConfirmRequired = true;
            }
        }
        typedArray.recycle();
        generalTypedArray.recycle();
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        if (!TextUtils.isEmpty(mReverseDependencyKey)) {
            Preference preference = findPreferenceInHierarchy(mReverseDependencyKey);
            if ((preference instanceof MySwitchPreference || preference
                    instanceof MyCheckBoxPreference)) {
                ReverseDependencyMonitor reverseDependencyMonitor =
                        (ReverseDependencyMonitor) preference;
                reverseDependencyMonitor.registerReverseDependencyPreference(this);
            }
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        if (mIsConfirmRequired) {
            String message = String.format(Locale.getDefault(), getContext()
                    .getString(R.string.confirm_script_dialog_message), getTitle());
            if (mRebootType == 2) {
                message = message + "\n\n" + getContext()
                        .getString(R.string.imminent_reboot_warning);
            }
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.run_script_confirm_dialog_title)
                    .setMessage(message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.run_script_confirm_dialog_positive_button,
                            (dialogInterface, i) -> runScript()).create()
                    .show();
        } else {
            runScript();
        }

    }

    private void runScript() {
        Command command = new Command(0, mFilePath) {
            @Override
            public void commandCompleted(int id, int exitCode) {
                super.commandCompleted(id, exitCode);
                if (exitCode != 0) {
                    Toast.makeText(getContext(), String.valueOf(exitCode), Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(getContext(), R.string.command_executed_success_toast,
                            Toast.LENGTH_SHORT).show();
                    if (mRebootType == 1) {
                        Utils.showRebootRequiredDialog(getContext());
                    } else if (mRebootType == 2) {
                        Utils.putCommand("su -c reboot");
                    } else if (mPackageToKill != null && Utils.isPackageInstalled(mPackageToKill)) {
                        if (mIsSilent) {
                            Utils.killPackage(mPackageToKill);
                        } else {
                            Utils.showKillPackageDialog(mPackageToKill, getContext());
                        }
                    }
                }
            }
        };
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException | TimeoutException | RootDeniedException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), R.string.root_tools_exception_toast,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
