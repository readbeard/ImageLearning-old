package com.google.sample.imagelearning;

import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

public class IntroActivity extends AppIntro {
    private static final int INTRO_FINISHED = 5;
    private static final int INTRO_FAILED = 7;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note here that we DO NOT use setContentView();


        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance("Welcome to ImageLearning!", "Learn languages by catching photos\n\nTake a picture of something you don't know how to say ", R.drawable.slide_1, getColor(R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance("Ask the big ones!", "Let ImageLearning work for you...\n\n ...it will combine machine learning results taken from Google and Microsoft", R.drawable.slide_2, getColor(R.color.blueaccent)));
        addSlide(AppIntroFragment.newInstance("Great!", "Get your words displayed in buttons, select language, click to listen and learn the word!\n\n The green buttons show the found matches between Google and Microsoft ", R.drawable.slide_3, getColor(R.color.deep_purple)));
        addSlide(AppIntroFragment.newInstance("Cool!", "Get your words displayed and a graph about result probabilities\n\n Long click on a button to Google search for it inside the app ", R.drawable.slide_4, getColor(R.color.blue)));

        // OPTIONAL METHODS
        // Override bar/separator color.
        //setBarColor(getColor(R.color.blue));
        setSeparatorColor(Color.GRAY);

        // Hide Skip/Done button.
        showSkipButton(true);
        setProgressButtonEnabled(true);

    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
        setResult(INTRO_FINISHED);
        this.finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
        setResult(INTRO_FINISHED);
        this.finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(INTRO_FAILED);
        this.finish();
    }
}
