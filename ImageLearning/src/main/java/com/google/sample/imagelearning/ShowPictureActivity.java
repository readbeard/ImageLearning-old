package com.google.sample.imagelearning;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;

import com.google.api.services.translate.Translate;
import com.google.api.services.translate.TranslateRequestInitializer;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.google.api.services.translate.model.TranslationsResource;


import org.apmem.tools.layouts.FlowLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ShowPictureActivity extends AppCompatActivity implements  SelectLanguageDialog.OnCompleteListener{
    private ImageView img;
    private ScrollView scrollView;
    private RelativeLayout.LayoutParams scrollViewParams;
    private RelativeLayout relativeLayout;

    private Bitmap bmp;
    private String absolutePath;
    private String visionValues;
    private FlowLayout scrollViewFlowLayout;

    private TextToSpeech t1;
    private Pattern doubleRegex;

    private boolean utteranceCompleted;

    private int buttonTotalNumber;
    private String currentLanguage="en_GB";
    private ImageButton changeLanguageButton;
    private BarChart bc;
    private List<BarEntry> entries = new ArrayList<>();
    private ArrayList<String> visionWords = new ArrayList<>();

    /**
     * Called on activity create. It sets the proper layout, considering the current orientation, and sets up the view that are
     * to be used in the whole class. This includes also the buttons, that are dinamically inserted (see addButtons() for more details)
     * and the imageview containing the picture taken by the user. It is resized because of screen sizes issues.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int orientation = getResources().getConfiguration().orientation;
        if(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_show_picture);
            relativeLayout = (RelativeLayout) findViewById(R.id.activity_show_picture);
            bc = (BarChart) findViewById(R.id.chart);
        }
        else {
            setContentView(R.layout.activity_show_picture_land);
            relativeLayout = (RelativeLayout) findViewById(R.id.activity_show_picture_land);
            bc = (BarChart) findViewById(R.id.chart_land);
        }

        //scrollView = (ScrollView) findViewById(R.id.button_scrollview);
        //scrollViewParams = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
        scrollViewFlowLayout = (FlowLayout) findViewById(R.id.buttons_flowlayout);
        img = (ImageView) findViewById(R.id.fullscreen_img);

        absolutePath = savedInstanceState == null? getIntent().getStringExtra("IMAGE"): savedInstanceState.getString("path");
        visionValues = getIntent().getStringExtra("VALUES");

        changeLanguageButton = (ImageButton) findViewById(R.id.change_language);



        setImageViewBitmap(absolutePath);
        addButtons();


    }

    /**
     * Initilizes the graph showing statistics about the calculated words. In particular, the graph is shown only in the mode that
     * best fits it, that is: when an image is too small in a particolar orientation mode, it means that some space is left unused,
     * so the graph fills that space. X-axis are
     */
    private void initializeGraph() {
        int orientation = getResources().getConfiguration().orientation;

        if(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            if(bmp.getWidth() > bmp.getHeight())
                bc.setVisibility(View.VISIBLE);
        }
        else {
            if(bmp.getWidth() < bmp.getHeight())
                bc.setVisibility(View.VISIBLE);
        }

        // the labels that should be drawn on the XAxis

        IAxisValueFormatter formatter = new IAxisValueFormatter() {

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                String res = visionWords.get((int)value);
                return res.substring(0,res.length()-2)+".";
            }


        };

        XAxis xAxis = bc.getXAxis();
        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);


        BarDataSet dataSet = new BarDataSet(entries,"results interval of confidence");
        BarData lineData = new BarData(dataSet);
        bc.setData(lineData);
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        Description barChartDesc = new Description();
        barChartDesc.setText("");
        bc.setDescription(barChartDesc);
        bc.setFitBars(true);
        bc.invalidate();
    }

    /**
     * Changes the language of the textToSpeech (t1) object and of the text of the buttons. It calls translateText() that is
     * triggering google translate APIs
     * @param targetLanguage the language that has to be set.
     */
    private void changeLanguage(Locale targetLanguage){
        //this is needed since when adding buttons on rotating screen we call again addButton(), that relies on
        //visualValues string to fill the text of the buttons. Since we are translating it, we need to update it.
        if(t1!= null ){
            t1.setLanguage(targetLanguage);
        }
        visionValues ="";
        for(int i =0; i<buttonTotalNumber;i++)
            translateText("button_"+i);

    }

    /**
     * Adds the buttton dinamically to the FlowLayout next to the imageView containing the picture. It parses the 'visionValues'
     * string, that is the result taken from Google Vision APIs in the previous activity. Note that, if language is set, this
     * string is updated with the translated version of it, since this method is called every time the language has to be changed.
     * Adds also entries to the chart, basing on the interval of confidence values related to words, that Google sends within the
     * JSON
     */
    private void addButtons() {
        scrollViewFlowLayout = (FlowLayout) findViewById(R.id.buttons_flowlayout);
        changeLanguageButton = (ImageButton) findViewById(R.id.change_language);
        Scanner scanner = new Scanner(visionValues);
        scanner.useDelimiter(":|\\n|\\s ");


        int i = 0;
        int graphEntryCount =0; //put a new Entry in the graph at the right position
        while (scanner.hasNext()) {
            final String next = scanner.next();
            System.out.println(next);
            //if i'm reading an interval of confidence, add a new Entry to the graph. If an element is present in a position
            //don't add  it (it means that it was previously already added).
            if(next.startsWith("0") && entries.size() <= graphEntryCount) {
                System.out.println(Float.parseFloat(next));
                entries.add(new BarEntry(graphEntryCount, Float.parseFloat(next)));
                graphEntryCount++;
            }

            if(!next.startsWith("0")){
                final Button calculatedWordButton = new Button(this);
                final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.fade_inout);

                calculatedWordButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(calculatedWordButton.getContentDescription()== null)
                            calculatedWordButton.startAnimation(myAnim);
                        else
                            calculatedWordButton.getAnimation().cancel();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            //needed to put the text of the button here. You cannot put 'next' since if you change
                            //the button's text, this onClick method won't see that change.
                            ttsGreater21(calculatedWordButton.getText().toString());
                        } else {
                            ttsUnder20(calculatedWordButton.getText().toString());
                        }
                    }
                });

                calculatedWordButton.setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                calculatedWordButton.setText(next);
                calculatedWordButton.setTag("button_"+i);
                scrollViewFlowLayout.addView(calculatedWordButton);
                visionWords.add(next);
                i++;
            }
            buttonTotalNumber = i;
        }

        changeLanguageButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SelectLanguageDialog dialog = new SelectLanguageDialog();
            dialog.setInitiallySelectedLang(currentLanguage);

            dialog.show(getFragmentManager(),"Language dialog");
            dialog.setRetainInstance(true);
            }
        });
        System.out.println(entries);
        initializeGraph();
    }

    /**
     * Initializes the 'textToSpeech' object, with as default the English language.
     */
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
        t1.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Button curr;
                for(int i = 0; i<buttonTotalNumber;i++){
                    curr = (Button) relativeLayout.findViewWithTag("button_"+i);
                    curr.setContentDescription(utteranceId);
                    curr.getAnimation().cancel();
                }
                utteranceCompleted = true;
            }

            @Override
            public void onDone(String utteranceId) {

            }

            @Override
            public void onError(String utteranceId) {

            }
        });
        //Backward compatibility...
        t1.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(String utteranceId) {
                Button curr;
                for(int i = 0; i<buttonTotalNumber;i++){
                    curr = (Button) relativeLayout.findViewWithTag("button_"+i);
                    curr.setAnimation(null);
                }
                utteranceCompleted = true;
            }
        });

    }

    /**
     * Frees memory by shutting down 'texToSpeech' object when activity is paused.
     */
    @Override
    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
            utteranceCompleted = false;
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("path",absolutePath);
    }

    /**
     * Sets the imageView background as the bitmap created from the picture taken. It scales it, in order to support different
     * picture sizes (fullscreen sometimes, bug still opened). It rotates it in order to perfectly fit the screen considering the
     * width and height of the picture.
     * @param absolutePath the path of the file containing the picture taken in previous activity.
     */
    private void setImageViewBitmap(String absolutePath) {
        try {
            FileInputStream fis = new FileInputStream(new File(absolutePath));
            bmp = BitmapFactory.decodeStream(fis);
            /*bmp =
                    MainActivity.scaleBitmapDown(
                            MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(new File(absolutePath))),
                            12000);*/
            bmp = rotateBitmap(bmp,calculateImageOrientation(absolutePath));
            img.setImageBitmap(bmp);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Computes the best orientation for the picture taken. Returns the orientation that has to be sent to 'rotateBitmap' method.
     * @param path the path of the picture taken
     * @return the orientation
     */
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

    /**
     * Rotates the bitmap in order to show it better basing on its size. If picture taken in landscape mode, it rotates it
     * to that mode, and so on...
     * @param bitmap the bitmap to be rotated
     * @param orientation the right orientation for it
     * @return the rotated bitmap.
     */
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

    /**
     * Called when the back button is pressed. It brings the flow back to MainActivity.
     */
    private void finishWithResult() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Called when the orientation of the screen changes. It needs to fill again the fields that are discarded
     * due to rotation, in particular it sets the proper layout basing on the screen orientation, it sets again
     * the imageView background to the picture, and adds again the buttons not to lose the information stored in them.
     * Unfortunately, for the moment is the best i could to. Maybe it can be optimized.
     * @param newConfig the new configuration of the changed mode activity.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_show_picture);
            img  = (ImageView) findViewById(R.id.fullscreen_img);
            relativeLayout = (RelativeLayout) findViewById(R.id.activity_show_picture);
            bc = (BarChart) findViewById(R.id.chart);
            img.setImageBitmap(bmp);
        }
        else if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_show_picture_land);
            img  = (ImageView) findViewById(R.id.fullscreen_img);
            img.setImageBitmap(bmp);
            relativeLayout = (RelativeLayout) findViewById(R.id.activity_show_picture_land);
            bc = (BarChart) findViewById(R.id.chart_land);
        }
        initializeGraph();
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

    /**
     * Translated the text that has to be shown in the buttons. In particular, it calls the Translate APIs from google via an
     * AsyncTask, it sets the buttons text to the (translated) result and updates the 'visionValues' string in order to be coherent
     * when 'addButton()' method will be called again. The method is called for every button to translate the text inside it,
     * so potentially we generate a lot of async tasks. But since they are very fast (litte job to to, the text of a button is very
     * short), this can be a good trade-off.
     * @param buttontag the tag of the button that called this method.
     */
    private void translateText(final String buttontag) {
        final Button buttonToTranslate = (Button) relativeLayout.findViewWithTag(buttontag);
        final String textToTranslate = buttonToTranslate==null? "":buttonToTranslate.getText().toString(); //you need to retrieve the text from main thread
        new AsyncTask<Object, Integer, String>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(Object... params) {
                List<TranslationsResource> list= null;

                final Translate translate = new Translate.Builder(
                        AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(),
                        new HttpRequestInitializer() {
                            @Override
                            public void initialize(HttpRequest httpRequest) throws IOException {
                                Log.d("TRANSLATE", "Http requst: " + httpRequest);
                            }
                        })
                        .setTranslateRequestInitializer(new TranslateRequestInitializer("AIzaSyD07diPeROl9YQQE-BLc7M9YLQCMQIKMQc"))
                        .build();
                try {
                    //Set the language to the one selected by user. This resides in the string 'currentLanguage', that
                    //has the form 'xx_YY', so getting the last two chars we get the language to pass to google trans.
                    Translate.Translations.List request = translate.translations().list(Arrays.asList(
                            //Pass in list of strings to be translated
                            textToTranslate),
                            //Target language: since the string currentLanguage
                            // is in the form 'xx_XX', where xx is the specific language in
                            // short, taking the first two characters of the string will be sufficient to Google Translate
                            currentLanguage.substring(0,2));
                    TranslationsListResponse tlr = request.execute();
                    return tlr.getTranslations().get(0).getTranslatedText();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "";
            }

            protected void onPostExecute(String result) {
                //this is needed since when adding buttons on rotating screen we call again addButton(), that relies on
                //visualValues string to fill the text of the buttons. Since we are translating it, we need to update it.
                visionValues = visionValues + result+ ":";
                //check this, since if rotating the view can be null
                buttonToTranslate.setText(result);

            }

        }.execute();

    }

    /**
     * Called when the language dialog is dismissed. It takes the language selected in orded to update the buttons and
     * the 'textToSpeech' object.
     * @param lang
     */
    @Override
    public void onComplete(String lang) {
        currentLanguage = lang;
        changeLanguage(new Locale(lang));
    }
}