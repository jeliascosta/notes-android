package it.niedermann.owncloud.notes.edit.title;

import static it.niedermann.owncloud.notes.branding.BrandingUtil.applyBrandToEditTextInputLayout;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import it.niedermann.owncloud.notes.R;
import it.niedermann.owncloud.notes.branding.BrandedDialogFragment;
import it.niedermann.owncloud.notes.databinding.DialogEditTitleBinding;

public class EditTitleDialogFragment extends BrandedDialogFragment {

    private static final String TAG = EditTitleDialogFragment.class.getSimpleName();
    static final String PARAM_OLD_TITLE = "old_title";
    private DialogEditTitleBinding binding;

    private String oldTitle;
    private EditTitleListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        final var args = getArguments();
        if (args == null) {
            throw new IllegalArgumentException("Provide at least " + PARAM_OLD_TITLE);
        }
        oldTitle = args.getString(PARAM_OLD_TITLE);

        if (getTargetFragment() instanceof EditTitleListener) {
            listener = (EditTitleListener) getTargetFragment();
        } else if (getActivity() instanceof EditTitleListener) {
            listener = (EditTitleListener) getActivity();
        } else {
            throw new IllegalArgumentException("Calling activity or target fragment must implement " + EditTitleListener.class.getSimpleName());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final var dialogView = View.inflate(getContext(), R.layout.dialog_edit_title, null);
        binding = DialogEditTitleBinding.bind(dialogView);

        if (savedInstanceState == null) {
            binding.title.setText(oldTitle);
        }

        return new MaterialAlertDialogBuilder(requireContext()/*, R.style.Theme_Material3_Light_Dialog_Alert*/)
                .setTitle(R.string.change_note_title)
                .setView(dialogView)
                .setBackgroundInsetTop(0)
                .setCancelable(true)
                .setPositiveButton(R.string.action_edit_save, (dialog, which) -> listener.onTitleEdited(binding.title.getText().toString()))
                .setNegativeButton(R.string.simple_cancel, null)
                .create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        binding.title.requestFocus();
        final var window = requireDialog().getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, 700);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        } else {
            Log.w(TAG, "can not enable soft keyboard because " + Window.class.getSimpleName() + " is null.");
        }
    }

    public static DialogFragment newInstance(String title) {
        final var fragment = new EditTitleDialogFragment();
        final var args = new Bundle();
        args.putString(PARAM_OLD_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void applyBrand(int mainColor, int textColor) {
        applyBrandToEditTextInputLayout(mainColor, binding.inputWrapper);
    }

    /**
     * Interface that must be implemented by the calling Activity.
     */
    public interface EditTitleListener {
        /**
         * This method is called after the user has changed the title of a note manually.
         *
         * @param newTitle the new title that a user submitted
         */
        void onTitleEdited(String newTitle);
    }
}
