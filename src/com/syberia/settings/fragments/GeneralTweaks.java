/*
 * Copyright © 2018 Syberia Project
 * Date: 22.08.2018
 * Time: 21:21
 * Author: @alexxxdev <alexxxdev@ya.ru>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.syberia.settings.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.content.ContentResolver;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;

import com.syberia.settings.preference.CustomSeekBarPreference;
import com.syberia.settings.preference.SecureSettingSwitchPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.android.settings.R;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.syberia.SyberiaUtils;
import com.syberia.settings.Utils;

public class GeneralTweaks extends SettingsPreferenceFragment implements OnPreferenceChangeListener, 
                                          DialogInterface.OnDismissListener, Indexable {

    private ListPreference mRecentsLayoutStylePref;
    private CustomSeekBarPreference mCornerRadius;
    private CustomSeekBarPreference mContentPadding;
    private SecureSettingSwitchPreference mRoundedFwvals;

    private static final String RECENTS_LAYOUT_STYLE_PREF = "recents_layout_style";
    private static final String RECENTS_ICON_PACK_KEY = "recents_icon_pack";
    private static final String SYSUI_ROUNDED_SIZE = "sysui_rounded_size";
    private static final String SYSUI_ROUNDED_CONTENT_PADDING = "sysui_rounded_content_padding";
    private static final String SYSUI_ROUNDED_FWVALS = "sysui_rounded_fwvals";

    final String KEY_LAUNCHER3_PACKAGE_NAME = "com.android.launcher3";

    private final static String[] sSupportedActions = new String[] {
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme"
    };

    private static final String[] sSupportedCategories = new String[] {
        "com.fede.launcher.THEME_ICONPACK",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME"
    };

    private AlertDialog mDialog;

    
    private ListPreference mScreenOffAnimation;
    private ListPreference mVelocityFriction;
    private ListPreference mPositionFriction;
    private ListPreference mVelocityAmplitude;
    private static final String SCREEN_OFF_ANIMATION = "screen_off_animation";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.general_tweaks);
		ContentResolver resolver = getActivity().getContentResolver();
	final PreferenceScreen screen = getPreferenceScreen();
        mScreenOffAnimation = (ListPreference) findPreference(SCREEN_OFF_ANIMATION);
        int screenOffStyle = Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_ANIMATION, 0);
        mScreenOffAnimation.setValue(String.valueOf(screenOffStyle));
        mScreenOffAnimation.setSummary(mScreenOffAnimation.getEntry());
        mScreenOffAnimation.setOnPreferenceChangeListener(this);

	if (!Utils.isPackageInstalled(getActivity(), KEY_LAUNCHER3_PACKAGE_NAME)) {
            screen.removePreference(findPreference(RECENTS_ICON_PACK_KEY));
        }

        // recents layout style
        mRecentsLayoutStylePref = (ListPreference) findPreference(RECENTS_LAYOUT_STYLE_PREF);
        int type = Settings.System.getInt(resolver,
                Settings.System.RECENTS_LAYOUT_STYLE, 0);
        mRecentsLayoutStylePref.setValue(String.valueOf(type));
        mRecentsLayoutStylePref.setSummary(mRecentsLayoutStylePref.getEntry());
        mRecentsLayoutStylePref.setOnPreferenceChangeListener(this);
	
	Resources res = null;
        Context ctx = getContext();
        float density = Resources.getSystem().getDisplayMetrics().density;

        try {
            res = ctx.getPackageManager().getResourcesForApplication("com.android.systemui");
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // Rounded Corner Radius
        mCornerRadius = (CustomSeekBarPreference) findPreference(SYSUI_ROUNDED_SIZE);
        mCornerRadius.setOnPreferenceChangeListener(this);
        int resourceIdRadius = res.getIdentifier("com.android.systemui:dimen/rounded_corner_radius", null, null);
        int cornerRadius = Settings.Secure.getInt(ctx.getContentResolver(), Settings.Secure.SYSUI_ROUNDED_SIZE,
                (int) (res.getDimension(resourceIdRadius) / density));
        mCornerRadius.setValue(cornerRadius / 1);

        // Rounded Content Padding
        mContentPadding = (CustomSeekBarPreference) findPreference(SYSUI_ROUNDED_CONTENT_PADDING);
        mContentPadding.setOnPreferenceChangeListener(this);
        int resourceIdPadding = res.getIdentifier("com.android.systemui:dimen/rounded_corner_content_padding", null,
                null);
        int contentPadding = Settings.Secure.getInt(ctx.getContentResolver(),
                Settings.Secure.SYSUI_ROUNDED_CONTENT_PADDING,
                (int) (res.getDimension(resourceIdPadding) / density));
        mContentPadding.setValue(contentPadding / 1);

        // Rounded use Framework Values
        mRoundedFwvals = (SecureSettingSwitchPreference) findPreference(SYSUI_ROUNDED_FWVALS);
        mRoundedFwvals.setOnPreferenceChangeListener(this);

        float velFriction = Settings.System.getFloatForUser(resolver,
                    Settings.System.STABILIZATION_VELOCITY_FRICTION,
                    0.1f,
                    UserHandle.USER_CURRENT);
        mVelocityFriction = (ListPreference) findPreference("stabilization_velocity_friction");
	mVelocityFriction.setValue(Float.toString(velFriction));
	mVelocityFriction.setSummary(mVelocityFriction.getEntry());
	mVelocityFriction.setOnPreferenceChangeListener(this);

	float posFriction = Settings.System.getFloatForUser(resolver,
                    Settings.System.STABILIZATION_POSITION_FRICTION,
                    0.1f,
                    UserHandle.USER_CURRENT);
            mPositionFriction = (ListPreference) findPreference("stabilization_position_friction");
	mPositionFriction.setValue(Float.toString(posFriction));
	mPositionFriction.setSummary(mPositionFriction.getEntry());
	mPositionFriction.setOnPreferenceChangeListener(this);

	int velAmplitude = Settings.System.getIntForUser(resolver,
                    Settings.System.STABILIZATION_VELOCITY_AMPLITUDE,
                    8000,
                    UserHandle.USER_CURRENT);
            mVelocityAmplitude = (ListPreference) findPreference("stabilization_velocity_amplitude");
	mVelocityAmplitude.setValue(Integer.toString(velAmplitude));
	mVelocityAmplitude.setSummary(mVelocityAmplitude.getEntry());
	mVelocityAmplitude.setOnPreferenceChangeListener(this);

    }

    private void restoreCorners() {
        Resources res = null;
        float density = Resources.getSystem().getDisplayMetrics().density;

        try {
            res = getContext().getPackageManager().getResourcesForApplication("com.android.systemui");
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        int resourceIdRadius = res.getIdentifier("com.android.systemui:dimen/rounded_corner_radius", null, null);
        int resourceIdPadding = res.getIdentifier("com.android.systemui:dimen/rounded_corner_content_padding", null,
                null);
        mCornerRadius.setValue((int) (res.getDimension(resourceIdRadius) / density));
        mContentPadding.setValue((int) (res.getDimension(resourceIdPadding) / density));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
	ContentResolver resolver = getActivity().getContentResolver();
	if (preference == mScreenOffAnimation) {
		String value = (String) newValue;
		Settings.System.putInt(resolver,
		Settings.System.SCREEN_OFF_ANIMATION, Integer.valueOf(value));
		int valueIndex = mScreenOffAnimation.findIndexOfValue(value);
		mScreenOffAnimation.setSummary(mScreenOffAnimation.getEntries()[valueIndex]);
	return true;
        } else if (preference == mRecentsLayoutStylePref) {
            int type = Integer.valueOf((String) newValue);
            int index = mRecentsLayoutStylePref.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.RECENTS_LAYOUT_STYLE, type);
            mRecentsLayoutStylePref.setSummary(mRecentsLayoutStylePref.getEntries()[index]);
            if (type != 0) { // Disable swipe up gesture if oreo type selected
               Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.SWIPE_UP_TO_SWITCH_APPS_ENABLED, 0);
	    }
            SyberiaUtils.showSystemUiRestartDialog(getContext());
            return true;
	} else if (preference == mCornerRadius) {
            Settings.Secure.putInt(getContext().getContentResolver(), Settings.Secure.SYSUI_ROUNDED_SIZE,
                    ((int) newValue) * 1);
        } else if (preference == mContentPadding) {
            Settings.Secure.putInt(getContext().getContentResolver(), Settings.Secure.SYSUI_ROUNDED_CONTENT_PADDING,
                    ((int) newValue) * 1);
        } else if (preference == mRoundedFwvals) {
            restoreCorners();
        } else if (preference == mVelocityFriction) {
	    String value = (String) newValue;
	    Settings.System.putFloatForUser(resolver,
	             Settings.System.STABILIZATION_VELOCITY_FRICTION, Float.valueOf(value), UserHandle.USER_CURRENT);
	    int valueIndex = mVelocityFriction.findIndexOfValue(value);
	    mVelocityFriction.setSummary(mVelocityFriction.getEntries()[valueIndex]);
        } else if (preference == mPositionFriction) {
	    String value = (String) newValue;
	    Settings.System.putFloatForUser(resolver,
	             Settings.System.STABILIZATION_POSITION_FRICTION, Float.valueOf(value), UserHandle.USER_CURRENT);
	    int valueIndex = mPositionFriction.findIndexOfValue(value);
	    mPositionFriction.setSummary(mPositionFriction.getEntries()[valueIndex]);
        } else if (preference == mVelocityAmplitude) {
	    String value = (String) newValue;
	    Settings.System.putFloatForUser(resolver,
	             Settings.System.STABILIZATION_VELOCITY_AMPLITUDE, Float.valueOf(value), UserHandle.USER_CURRENT);
	    int valueIndex = mVelocityAmplitude.findIndexOfValue(value);
	    mVelocityAmplitude.setSummary(mVelocityAmplitude.getEntries()[valueIndex]);
       }
    	return true;
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == findPreference("recents_icon_pack")) {
            pickIconPack(getContext());
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

     /** Recents Icon Pack Dialog **/
    private void pickIconPack(final Context context) {
        if (mDialog != null) {
            return;
        }
        Map<String, IconPackInfo> supportedPackages = getSupportedPackages(context);
        if (supportedPackages.isEmpty()) {
            Toast.makeText(context, R.string.no_iconpacks_summary, Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
        .setTitle(R.string.dialog_pick_iconpack_title)
        .setOnDismissListener(this)
        .setNegativeButton(R.string.cancel, null)
        .setView(createDialogView(context, supportedPackages));
        mDialog = builder.show();
    }

    private View createDialogView(final Context context, Map<String, IconPackInfo> supportedPackages) {
        final LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.dialog_iconpack, null);
        final IconAdapter adapter = new IconAdapter(context, supportedPackages);
         ListView mListView = (ListView) view.findViewById(R.id.iconpack_list);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                if (adapter.isCurrentIconPack(position)) {
                    return;
                }
                String selectedPackage = adapter.getItem(position);
                Settings.System.putString(getContext().getContentResolver(),
                        Settings.System.RECENTS_ICON_PACK, selectedPackage);
                mDialog.dismiss();
            }
        });
         return view;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mDialog != null) {
            mDialog = null;
        }
    }

    private static class IconAdapter extends BaseAdapter {
        ArrayList<IconPackInfo> mSupportedPackages;
        LayoutInflater mLayoutInflater;
        String mCurrentIconPack;
        int mCurrentIconPackPosition = -1;
         IconAdapter(Context ctx, Map<String, IconPackInfo> supportedPackages) {
            mLayoutInflater = LayoutInflater.from(ctx);
            mSupportedPackages = new ArrayList<IconPackInfo>(supportedPackages.values());
            Collections.sort(mSupportedPackages, new Comparator<IconPackInfo>() {
                @Override
                public int compare(IconPackInfo lhs, IconPackInfo rhs) {
                    return lhs.label.toString().compareToIgnoreCase(rhs.label.toString());
                }
            });
             Resources res = ctx.getResources();
            String defaultLabel = res.getString(R.string.default_iconpack_title);
            Drawable icon = ContextCompat.getDrawable(ctx, android.R.drawable.sym_def_app_icon);
            mSupportedPackages.add(0, new IconPackInfo(defaultLabel, icon, ""));
            mCurrentIconPack = Settings.System.getString(ctx.getContentResolver(),
                Settings.System.RECENTS_ICON_PACK);
        }
         @Override
        public int getCount() {
            return mSupportedPackages.size();
        }
         @Override
        public String getItem(int position) {
            return (String) mSupportedPackages.get(position).packageName;
        }
         @Override
        public long getItemId(int position) {
            return 0;
        }
         public boolean isCurrentIconPack(int position) {
            return mCurrentIconPackPosition == position;
        }
         @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.iconpack_view_radio, null);
            }
            IconPackInfo info = mSupportedPackages.get(position);
            TextView txtView = (TextView) convertView.findViewById(R.id.title);
            txtView.setText(info.label);
            ImageView imgView = (ImageView) convertView.findViewById(R.id.icon);
            imgView.setImageDrawable(info.icon);
            RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.radio);
            boolean isCurrentIconPack = info.packageName.equals(mCurrentIconPack);
            radioButton.setChecked(isCurrentIconPack);
            if (isCurrentIconPack) {
                mCurrentIconPackPosition = position;
            }
            return convertView;
        }
    }

    private Map<String, IconPackInfo> getSupportedPackages(Context context) {
        Intent i = new Intent();
        Map<String, IconPackInfo> packages = new HashMap<String, IconPackInfo>();
        PackageManager packageManager = context.getPackageManager();
        for (String action : sSupportedActions) {
            i.setAction(action);
            for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
                IconPackInfo info = new IconPackInfo(r, packageManager);
                packages.put(r.activityInfo.packageName, info);
            }
        }
        i = new Intent(Intent.ACTION_MAIN);
        for (String category : sSupportedCategories) {
            i.addCategory(category);
            for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
                IconPackInfo info = new IconPackInfo(r, packageManager);
                packages.put(r.activityInfo.packageName, info);
            }
            i.removeCategory(category);
        }
        return packages;
    }

    static class IconPackInfo {
        String packageName;
        CharSequence label;
        Drawable icon;
         IconPackInfo(ResolveInfo r, PackageManager packageManager) {
            packageName = r.activityInfo.packageName;
            icon = r.loadIcon(packageManager);
            label = r.loadLabel(packageManager);
        }
         IconPackInfo(){
        }
         public IconPackInfo(String label, Drawable icon, String packageName) {
            this.label = label;
            this.icon = icon;
            this.packageName = packageName;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SYBERIA;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.general_tweaks;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
    };
}