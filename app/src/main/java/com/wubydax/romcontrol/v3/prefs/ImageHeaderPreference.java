package com.wubydax.romcontrol.v3.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.wubydax.romcontrol.v3.R;


public class ImageHeaderPreference extends Preference {
    private final int mResId;

    public ImageHeaderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context
                .obtainStyledAttributes(attrs, R.styleable.ImageHeaderPreference);
        mResId = typedArray
                .getResourceId(R.styleable.ImageHeaderPreference_imageSource, -1);
        typedArray.recycle();
        setLayoutResource(R.layout.preference_header_general);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        ((ImageView) view).setImageDrawable(ContextCompat.getDrawable(getContext(), mResId));
        return view;
    }
}
