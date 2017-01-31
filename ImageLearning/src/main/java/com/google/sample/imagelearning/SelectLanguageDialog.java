package com.google.sample.imagelearning;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by matteo on 1/30/17.
 */

public class SelectLanguageDialog extends DialogFragment{

    String language = "";
    private OnCompleteListener mListener;
    private int langCode;


    /**
     * Called when a dialog to select language is created. It sets up the layout and click listeners for items. When
     * 'ok' message is pressed, it is dismissed and the string representing the language is given back to ShowPictureActivty.
     * @param savedInstanceState the state
     * @return the dialog
     */
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View v = inflater.inflate(R.layout.language_select, null);
        builder.setCancelable(false);

        builder.setView(v);
        builder.setTitle("Select language");

        final ListView mLocationList = (ListView)v.findViewById(R.id.listview);

        final CustomAdapter adapter = new CustomAdapter(getActivity(),5); //4 languages supported
        mLocationList.setAdapter(adapter);
        mLocationList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        adapter.setSelectedIndex(langCode);

        mLocationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView selectedLanguageTV = (TextView) view.findViewById(R.id.language_textview);
                String selectedLanguage = selectedLanguageTV.getText().toString();

                String locale = "en_UK";
                if(selectedLanguage.equals("English"))
                    locale = "it_IT";
                if(selectedLanguage.equals("Italiano"))
                    locale = "it_IT";
                else if(selectedLanguage.equals("French"))
                    locale = "fr_FR";
                else if(selectedLanguage.equals("Espanol"))
                    locale = "es_ES";
                else if(selectedLanguage.equals("German")){
                    locale = "de_DE";
                }
                //set the string to pass back to ShowPictureActivity representing the language
                setLanguage(locale);
                //tell to the CustomAdapter the selected view in order to change its background. See Customadapter.getView()
                //for mor details
                adapter.setSelectedIndex(position);
            }
        });

        builder.setMessage("").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onComplete(getLanguage());
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });


        // Create the AlertDialog object and return it
        return builder.create();
    }

    /**
     *  Sets the string used to change the language. It is returned in the form 'xx_XX' to create a proper Locale object.
     * @param language the language that has to be set.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    // make sure the Activity implemented it
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnCompleteListener)activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCompleteListener");
        }
    }

    /**
     * Used to manage the interaction between fragment and calling activity. In particular, ShowPictureActivity implements
     * this method to know when the dialog finished.
     */
    public static interface OnCompleteListener {
        public abstract void onComplete(String time);
    }

    public void setInitaliiySelectedLang(String language){
        if(language.contains("EN"))
            this.langCode = 0;
        if(language.contains("IT"))
            this.langCode = 1;
        if(language.contains("FR"))
            this.langCode = 2;
        if(language.contains("ES"))
            this.langCode = 3;
        if(language.contains("DE"))
            this.langCode = 4;
    }
}

