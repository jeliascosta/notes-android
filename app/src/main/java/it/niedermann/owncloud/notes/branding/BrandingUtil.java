package it.niedermann.owncloud.notes.branding;

import static it.niedermann.owncloud.notes.NotesApplication.isDarkThemeActive;
import static it.niedermann.owncloud.notes.shared.util.NotesColorUtil.contrastRatioIsSufficient;
import static it.niedermann.owncloud.notes.shared.util.NotesColorUtil.contrastRatioIsSufficientBigAreas;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputLayout;

import it.niedermann.android.sharedpreferences.SharedPreferenceIntLiveData;
import it.niedermann.android.util.ColorUtil;
import it.niedermann.owncloud.notes.NotesApplication;
import it.niedermann.owncloud.notes.R;
import it.niedermann.owncloud.notes.shared.util.NotesColorUtil;

public class BrandingUtil {

    private static final String TAG = BrandingUtil.class.getSimpleName();
    private static final String pref_key_branding_main = "branding_main";
    private static final String pref_key_branding_text = "branding_text";

    private BrandingUtil() {

    }

    public static LiveData<Pair<Integer, Integer>> readBrandColors(@NonNull Context context) {
        return new BrandingLiveData(context);
    }

    private static class BrandingLiveData extends MediatorLiveData<Pair<Integer, Integer>> {
        @ColorInt
        Integer lastMainColor = null;
        @ColorInt
        Integer lastTextColor = null;

        public BrandingLiveData(@NonNull Context context) {
            addSource(readBrandMainColorLiveData(context), (nextMainColor) -> {
                lastMainColor = nextMainColor;
                if (lastTextColor != null) {
                    postValue(new Pair<>(lastMainColor, lastTextColor));
                }
            });
            addSource(readBrandTextColorLiveData(context), (nextTextColor) -> {
                lastTextColor = nextTextColor;
                if (lastMainColor != null) {
                    postValue(new Pair<>(lastMainColor, lastTextColor));
                }
            });
        }
    }

    public static LiveData<Integer> readBrandMainColorLiveData(@NonNull Context context) {
        final var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Log.v(TAG, "--- Read: shared_preference_theme_main");
        return new SharedPreferenceIntLiveData(sharedPreferences, pref_key_branding_main, context.getApplicationContext().getResources().getColor(R.color.defaultBrand));
    }

    public static LiveData<Integer> readBrandTextColorLiveData(@NonNull Context context) {
        final var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Log.v(TAG, "--- Read: shared_preference_theme_text");
        return new SharedPreferenceIntLiveData(sharedPreferences, pref_key_branding_text, Color.WHITE);
    }

    @ColorInt
    public static int readBrandMainColor(@NonNull Context context) {
        final var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Log.v(TAG, "--- Read: shared_preference_theme_main");
        return sharedPreferences.getInt(pref_key_branding_main, context.getApplicationContext().getResources().getColor(R.color.defaultBrand));
    }

    @ColorInt
    public static int readBrandTextColor(@NonNull Context context) {
        final var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Log.v(TAG, "--- Read: shared_preference_theme_text");
        return sharedPreferences.getInt(pref_key_branding_text, Color.WHITE);
    }

    public static void saveBrandColors(@NonNull Context context, @ColorInt int mainColor, @ColorInt int textColor) {
        final int previousMainColor = readBrandMainColor(context);
        final int previousTextColor = readBrandTextColor(context);
        final var editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        Log.v(TAG, "--- Write: shared_preference_theme_main" + " | " + mainColor);
        Log.v(TAG, "--- Write: shared_preference_theme_text" + " | " + textColor);
        editor.putInt(pref_key_branding_main, mainColor);
        editor.putInt(pref_key_branding_text, textColor);
        editor.apply();
        if (context instanceof BrandedActivity) {
            if (mainColor != previousMainColor || textColor != previousTextColor) {
                final var activity = (BrandedActivity) context;
                activity.runOnUiThread(() -> ActivityCompat.recreate(activity));
            }
        }
    }

    /**
     * Since we may collide with dark theme in this area, we have to make sure that the color is visible depending on the background
     */
    @ColorInt
    public static int getSecondaryForegroundColorDependingOnTheme(@NonNull Context context, @ColorInt int mainColor) {
        final int primaryColor = ContextCompat.getColor(context, R.color.primary);
        final boolean isDarkTheme = NotesApplication.isDarkThemeActive(context);
        if (isDarkTheme && !contrastRatioIsSufficient(mainColor, primaryColor)) {
            Log.v(TAG, "Contrast ratio between brand color " + String.format("#%06X", (0xFFFFFF & mainColor)) + " and dark theme is too low. Falling back to WHITE as brand color.");
            return Color.WHITE;
        } else if (!isDarkTheme && !contrastRatioIsSufficient(mainColor, primaryColor)) {
            Log.v(TAG, "Contrast ratio between brand color " + String.format("#%06X", (0xFFFFFF & mainColor)) + " and light theme is too low. Falling back to BLACK as brand color.");
            return Color.BLACK;
        } else {
            return mainColor;
        }
    }

    public static void applyBrandToEditText(@ColorInt int mainColor, @ColorInt int textColor, @NonNull EditText editText) {
        @ColorInt final int finalMainColor = getSecondaryForegroundColorDependingOnTheme(editText.getContext(), mainColor);
        DrawableCompat.setTintList(editText.getBackground(), new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_active},
                        new int[]{android.R.attr.state_activated},
                        new int[]{android.R.attr.state_focused},
                        new int[]{android.R.attr.state_pressed},
                        new int[]{}
                },
                new int[]{
                        finalMainColor,
                        finalMainColor,
                        finalMainColor,
                        finalMainColor,
                        editText.getContext().getResources().getColor(R.color.fg_default_low)
                }
        ));
    }

    public static void applyBrandToEditTextInputLayout(@ColorInt int color, @NonNull TextInputLayout til) {
        final int colorPrimary = ContextCompat.getColor(til.getContext(), R.color.primary);
        final int colorAccent = ContextCompat.getColor(til.getContext(), R.color.accent);
        final var colorDanger = ColorStateList.valueOf(ContextCompat.getColor(til.getContext(), R.color.danger));
        til.setBoxStrokeColor(contrastRatioIsSufficientBigAreas(color, colorPrimary) ? color : colorAccent);
        til.setHintTextColor(ColorStateList.valueOf(contrastRatioIsSufficient(color, colorPrimary) ? color : colorAccent));
        til.setErrorTextColor(colorDanger);
        til.setBoxStrokeErrorColor(colorDanger);
        til.setErrorIconTintList(colorDanger);
    }

    public static void tintMenuIcon(@NonNull MenuItem menuItem, @ColorInt int color) {
        var drawable = menuItem.getIcon();
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, color);
            menuItem.setIcon(drawable);
        }
    }

    public static void applyBrandToLayerDrawable(@NonNull LayerDrawable check, @IdRes int areaToColor, @ColorInt int mainColor) {
        final var drawable = check.findDrawableByLayerId(areaToColor);
        if (drawable == null) {
            Log.e(TAG, "Could not find areaToColor (" + areaToColor + "). Cannot apply brand.");
        } else {
            DrawableCompat.setTint(drawable, mainColor);
        }
    }

    @ColorInt
    public static int getAttribute(@NonNull Context context, @AttrRes int id) {
        final var typedValue = new TypedValue();
        context.getTheme().resolveAttribute(id, typedValue, true);
        return typedValue.data;
    }

    @ColorInt
    public static int getTextHighlightBackgroundColor(@NonNull Context context, @ColorInt int mainColor, @ColorInt int colorPrimary, @ColorInt int colorAccent) {
        if (isDarkThemeActive(context)) { // Dark background
            if (ColorUtil.INSTANCE.isColorDark(mainColor)) { // Dark brand color
                if (NotesColorUtil.contrastRatioIsSufficient(mainColor, colorPrimary)) { // But also dark text
                    return mainColor;
                } else {
                    return ContextCompat.getColor(context, R.color.defaultTextHighlightBackground);
                }
            } else { // Light brand color
                if (NotesColorUtil.contrastRatioIsSufficient(mainColor, colorAccent)) { // But also dark text
                    return Color.argb(77, Color.red(mainColor), Color.green(mainColor), Color.blue(mainColor));
                } else {
                    return ContextCompat.getColor(context, R.color.defaultTextHighlightBackground);
                }
            }
        } else { // Light background
            if (ColorUtil.INSTANCE.isColorDark(mainColor)) { // Dark brand color
                if (NotesColorUtil.contrastRatioIsSufficient(mainColor, colorAccent)) { // But also dark text
                    return Color.argb(77, Color.red(mainColor), Color.green(mainColor), Color.blue(mainColor));
                } else {
                    return ContextCompat.getColor(context, R.color.defaultTextHighlightBackground);
                }
            } else { // Light brand color
                if (NotesColorUtil.contrastRatioIsSufficient(mainColor, colorPrimary)) { // But also dark text
                    return mainColor;
                } else {
                    return ContextCompat.getColor(context, R.color.defaultTextHighlightBackground);
                }
            }
        }
    }
}
