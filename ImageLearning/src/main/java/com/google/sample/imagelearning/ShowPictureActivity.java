package com.google.sample.imagelearning;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import org.apmem.tools.layouts.FlowLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ShowPictureActivity extends AppCompatActivity {
    private ImageView img;
    private ScrollView scrollView;
    private RelativeLayout.LayoutParams scrollViewParams;
    private RelativeLayout relativeLayout;

    private Bitmap bmp;
    private String absolutePath;
    private String visionValues;
    private FlowLayout scrollViewLinearLayout;

    private TextToSpeech t1;
    private Pattern doubleRegex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int orientation = getResources().getConfiguration().orientation;
        if(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            setContentView(R.layout.activity_show_picture);
        else
            setContentView(R.layout.activity_show_picture_land);

        //scrollView = (ScrollView) findViewById(R.id.button_scrollview);
        relativeLayout = (RelativeLayout) findViewById(R.id.activity_show_picture);
        //scrollViewParams = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
        scrollViewLinearLayout = (FlowLayout) findViewById(R.id.scrollView_linearLayout);
        img = (ImageView) findViewById(R.id.fullscreen_img);

        absolutePath = savedInstanceState == null? getIntent().getStringExtra("IMAGE"): savedInstanceState.getString("path");
        visionValues = getIntent().getStringExtra("VALUES");
        doubleRegex = Pattern.compile(getString(R.string.double_regex));

        addButtons();
        
        setImageViewBitmap(absolutePath);


    }

    private void addButtons() {
        scrollViewLinearLayout = (FlowLayout) findViewById(R.id.scrollView_linearLayout);
        Scanner scanner = new Scanner(visionValues);
        scanner.useDelimiter("I found these things:|\\W|\\n|\\s ");
        while (scanner.hasNext()) {
            final String next = scanner.next();
            boolean isDouble = doubleRegex.matcher(next).matches();
            System.out.println(next +", "+ isDouble);
            if(!isDouble && ! next.equals("")) {
                Button calculatedWordButton = new Button(this);
                calculatedWordButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ttsGreater21(next);
                        } else {
                            ttsUnder20(next);
                        }
                    }
                });
                calculatedWordButton.setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                calculatedWordButton.setText(next);
                scrollViewLinearLayout.addView(calculatedWordButton);
            }
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });
    }

    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("path",absolutePath);
    }

    private void setImageViewBitmap(String absolutePath) {
        try {
            FileInputStream fis = new FileInputStream(new File(absolutePath));
            bmp = BitmapFactory.decodeStream(fis);
            bmp = rotateBitmap(bmp,calculateImageOrientation(absolutePath));
            img.setImageBitmap(bmp);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int calculateImageOrientation(String path){
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);


        return orientation;
    }

    public Bitmap rotateBitmap(Bitmap bitmap, int orientation) {


        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            System.out.println("BITMAP: "+bitmap);
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            System.out.println("BITMAP: "+bitmap);
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        finishWithResult();
    }

    private void finishWithResult() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig){
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_show_picture);
            ImageView img  = (ImageView) findViewById(R.id.fullscreen_img);
            img.setImageBitmap(bmp);
        }
        else if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_show_picture_land);
            ImageView img  = (ImageView) findViewById(R.id.fullscreen_img);
            img.setImageBitmap(bmp);
        }
        addButtons();
        super.onConfigurationChanged(newConfig);
    }

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        t1.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId=this.hashCode() + "";
        t1.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

}
