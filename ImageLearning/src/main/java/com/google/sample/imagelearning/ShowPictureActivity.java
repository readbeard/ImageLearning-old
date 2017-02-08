package com.google.sample.imagelearning;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

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

public class ShowPictureActivity extends AppCompatActivity implements  SelectLanguageDialog.OnCompleteListener{
    private ImageView img;
    private RelativeLayout relativeLayout;

    private Bitmap bmp;
    private String absolutePath;
    private String visionValues;
    private FlowLayout scrollViewFlowLayout;

    private TextToSpeech t1;

    private int buttonTotalNumber;
    private String currentLanguage="en_GB";
    private ImageButton changeLanguageButton;
    private BarChart bc;
    private List<BarEntry> entries = new ArrayList<>();
    private ArrayList<String> visionWords = new ArrayList<>();
    private String matches;
    private String oldmatches;
    private WebView myWebView;
    private ImageButton closeWebView;
    private String currentURL="";
    private View webViewDivisor;
    private ProgressBar loading;

    /**
     * Called on activity create. It sets the proper layout, considering the current orientation, and sets up the view that are
     * to be used in the whole class. Note that views have the same name even if they belong to two different XMLs, depending
     * on the orientation. This was done to not write the same code multiple times.
     * This includes also the buttons, that are dinamically inserted (see addButtons() for more details)
     * and the imageview containing the picture taken by the user. It is resized because of screen sizes issues.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int orientation = getResources().getConfiguration().orientation;
        if(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_show_picture);
        }
        else {
            setContentView(R.layout.activity_show_picture_land);
        }

        relativeLayout = (RelativeLayout) findViewById(R.id.activity_show_picture);
        bc = (BarChart) findViewById(R.id.chart);
        disableVirtualButtons();

        scrollViewFlowLayout = (FlowLayout) findViewById(R.id.buttons_flowlayout);
        img = (ImageView) findViewById(R.id.fullscreen_img);

        absolutePath = savedInstanceState == null? getIntent().getStringExtra("IMAGE"): savedInstanceState.getString("path");
        visionValues = getIntent().getStringExtra("VALUES");
        matches = getIntent().getStringExtra("MATCHES");

        changeLanguageButton = (ImageButton) findViewById(R.id.change_language);

        setUpWebView();

        setImageViewBitmap(absolutePath);
        addButtons();


    }

    /**
     * Virtual buttons can cover views and occupy space that is needed to show all the layout.
     * Removes them for simplicity, making the app fullscreen
     */
    private void disableVirtualButtons() {
        //if there are virtual buttons, don't show them since they would waste some space and make some buttons invisible
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    /**
     * Initilizes the graph showing statistics about the calculated words. In particular, the graph is shown only in the mode that
     * best fits it, that is: when an image is too small in a particolar orientation mode, it means that some space is left unused,
     * so the graph fills that space. X-axis are
     */
    private void initializeGraph() {

        // the labels that should be drawn on the XAxis. If the word has more than 3 characters, cut it and put
        // it as label, otherwise put the entire

        IAxisValueFormatter formatter = new IAxisValueFormatter() {

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                //Since data received from Google have an initial space (" "),
                String res = " ";
                //means that the translation of all the buttons is not finished, some button needs
                //to fill visionword. Skip for now the filling, the next button will do it.
                if(value < visionWords.size())
                    res += visionWords.get((int)value);
                if(res.length()>10)
                    return res.substring(1,8)+".";
                else
                    return res;

            }


        };

        XAxis xAxis = bc.getXAxis();
        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);


        BarDataSet dataSet = new BarDataSet(entries,"results interval of confidence");
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        BarData lineData = new BarData(dataSet);
        bc.setData(lineData);
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
        oldmatches = matches;
        matches="";
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
            //if reading an interval of confidence, add a new Entry to the graph. If an element is present in a position
            //don't add  it (it means that it was previously already added).
            if(next.startsWith("0") && entries.size() <= graphEntryCount) {

                entries.add(new BarEntry(graphEntryCount, Float.parseFloat(next.replace(',', '.'))));
                graphEntryCount++;
            }

            if(!next.startsWith("0") && !visionValues.substring(0,i).contains(next)){
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

                calculatedWordButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        showWebViewSearch(calculatedWordButton.getText());
                        return true;
                    }
                });

                calculatedWordButton.setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                calculatedWordButton.setText(next);
                calculatedWordButton.setCompoundDrawablesWithIntrinsicBounds(0,0,R.mipmap.ic_volume_up_black_24dp,0);
                calculatedWordButton.setTextColor(ContextCompat.getColor(this,R.color.colorAccent));
                calculatedWordButton.setTag("button_"+i);
                if(matches.contains(next)) {
                    calculatedWordButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.light_green), PorterDuff.Mode.MULTIPLY);

                }else {
                    calculatedWordButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
                }
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
        initializeGraph();
    }

    /**
     * Makes webview visible, and starts google searching what is specified in the parameter, appending it to
     * the standard URL to make a google search
     * @param text the thing thas has to be searched.
     */
    private void showWebViewSearch(CharSequence text) {
        myWebView.setVisibility(View.VISIBLE);
        webViewDivisor.setVisibility(View.VISIBLE);
        closeWebView.setVisibility(View.VISIBLE);
        myWebView.loadUrl("https://www.google.com/search?q="+text);
        bc.setVisibility(View.GONE);
    }

    /**
     * Callback method only invoked by imagebutton in XML code. It stops webview loading, sets it to invisible (gone)
     * together with loading spinner.
     * @param v the view from which this method is called.
     */
    public void closeWebViewFromXML(View v){
        myWebView.setVisibility(View.GONE);
        closeWebView.setVisibility(View.GONE);
        webViewDivisor.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
        currentURL = "";
        bc.setVisibility(View.VISIBLE);
        myWebView.stopLoading();
    }

    /**
     * Callback method to handle user interaction with system buttons. In particular, it deals with back button
     * to allow user go back in navigation history in web view. If no back pages are available, it closes the web view.
     * If web view already closed, behaves like normal back button and closes the activity.
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (myWebView.canGoBack() && (loading.getVisibility()== View.VISIBLE || myWebView.getVisibility()==View.VISIBLE)) {
                        myWebView.goBack();
                    } else if(!currentURL.isEmpty()){
                        closeWebViewFromXML(null);
                    }else
                        finishWithResult();

                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
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
                    t1.setLanguage(new Locale(currentLanguage));
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
            }
            //NOT useful at all, but to be implemented together with onStart
            @Override
            public void onDone(String utteranceId) {

            }
            //NOT useful at all, but to be implemented together with onStart
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

            DisplayMetrics display = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(display);
            int screenWidth = display.widthPixels;
            int  screenHeight = display.heightPixels;

            if(getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                bmp = getResizedBitmap(bmp, screenWidth, screenHeight / 2);
                img.setMaxHeight(screenHeight/2);
                img.setMinimumHeight(screenHeight/2);
            }
            else
                bmp = getResizedBitmap(bmp,screenWidth/2,screenHeight);

            img.setImageBitmap(bmp);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Bitmap getResizedBitmap(Bitmap image, int maxWidth,int maxHeight) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxWidth;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxHeight;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
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

        }
        else if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_show_picture_land);
        }

        relativeLayout = (RelativeLayout) findViewById(R.id.activity_show_picture);
        img  = (ImageView) findViewById(R.id.fullscreen_img);
        bc = (BarChart) findViewById(R.id.chart);
        setUpWebView();
        setImageViewBitmap(absolutePath);
        disableVirtualButtons();
        initializeGraph();
        addButtons();
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Sets up web view, loading spinner and View divisor (a straight line, just for GUI purposes). It initializes
     * the callback functions of the webview, in order to show it only when available, or to show the loading
     * spinner if not ready yet. Last condition checks if there was a saved URL from previous activity of this view. That is,
     * checks if a request was issued while changing orientation of the screen, and restarts it if true.
     */
    private void setUpWebView() {
        loading = (ProgressBar) findViewById(R.id.progressBar1);
        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.getSettings().setDisplayZoomControls(false);
        webViewDivisor = (View) findViewById(R.id.divisor5);
        closeWebView = (ImageButton) findViewById(R.id.close_webview_button);
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                currentURL = url;
                loading.setVisibility(View.VISIBLE);
                myWebView.setVisibility(View.INVISIBLE);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i("Listener", "Finish loading: "+ url);
                if(myWebView.getVisibility() == View.INVISIBLE) {
                    loading.setVisibility(View.INVISIBLE);
                    myWebView.setVisibility(View.VISIBLE);
                }
                super.onPageFinished(view, url);
            }
        });

        if(!currentURL.isEmpty()) {
            myWebView.loadUrl(currentURL);
            myWebView.setVisibility(View.VISIBLE);
            webViewDivisor.setVisibility(View.VISIBLE);
            closeWebView.setVisibility(View.VISIBLE);
            bc.setVisibility(View.GONE);
        }
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
     * so potentially we generate a lot of async tasks. But since they are very fast (litte job to do, the text of a button is very
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
                        .setTranslateRequestInitializer(new TranslateRequestInitializer(getString(R.string.cloud_vision_apikey)))
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
                if(!result.isEmpty()) {
                    visionValues = visionValues + result + ":";
                    if (oldmatches.contains(textToTranslate))
                        matches = matches + result;
                    //check this, since if rotating the view can be null
                    buttonToTranslate.setText(result);

                    visionWords.add(result);
                    initializeGraph();
                }
                else{
                    Toast.makeText(ShowPictureActivity.this,"An error occurred, please try again",Toast.LENGTH_LONG).show();

                }
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
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if(isConnected) {
            currentLanguage = lang;
            changeLanguage(new Locale(lang));
        }
        else
            Toast.makeText(ShowPictureActivity.this,"An error occurred (internet not working?) please try again",Toast.LENGTH_LONG).show();

        //cancel values to put translated words
        visionWords.clear();
        disableVirtualButtons();

    }

}
