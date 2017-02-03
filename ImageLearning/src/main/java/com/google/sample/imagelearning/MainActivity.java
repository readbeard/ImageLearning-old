/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.sample.imagelearning;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    private String CLOUD_VISION_API_KEY;
    private java.lang.String VISIONSERVICE_API_KEY;
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    private static final int SHOW_PICTURE_ACTIVITY = 4;
    private RelativeLayout mainActivityLayout;
    private ImageView mainActivityImageView;
    private Bitmap bitmap;

    private VisionServiceClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CLOUD_VISION_API_KEY = getString(R.string.cloud_vision_apikey);
        VISIONSERVICE_API_KEY = getString(R.string.visionservice_apikey);

        mainActivityLayout = (RelativeLayout) findViewById(R.id.mainactivity_layout);
        mainActivityImageView = (ImageView) findViewById(R.id.mainactivity_background_imageview);

        if (client==null){
            client = new VisionServiceRestClient(VISIONSERVICE_API_KEY);
        }

        startCamera();
    }



    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getCameraFile()));
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            uploadImage(Uri.fromFile(getCameraFile()));
            mainActivityImageView.setBackground(Drawable.createFromPath(Uri.fromFile(getCameraFile()).getPath()));
        }else if(requestCode == SHOW_PICTURE_ACTIVITY && resultCode == RESULT_OK){
            startCamera();
        }else if(requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_CANCELED){
            this.finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
        }
    }

    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                 bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                1200);

                callCloudVision(bitmap);
                /*Intent showImageFullscreen = new Intent(MainActivity.this,ShowPictureActivity.class);
                showImageFullscreen.putExtra("IMAGE",getCameraFile().getAbsolutePath());
                showImageFullscreen.putExtra("VALUES"," THE :0.987 : QUICK :0.876: BROWN :0.764 : FOX :0.654: JUMPS :0.976: OVER :0.324: THE:0.496 : LAZY : DOG");
                startActivityForResult(showImageFullscreen,SHOW_PICTURE_ACTIVITY);*/

            } catch (Exception e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Switch text to loading

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Integer, String[]>() {
            private ProgressDialog mProgressDialog = new ProgressDialog(MainActivity.this);
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDialog.setMessage("Please wait");
                mProgressDialog.setTitle("Calculating results...");
                mProgressDialog.setCancelable(false);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setOwnerActivity(MainActivity.this);
                mProgressDialog.show();


            }

            @Override
            protected String[] doInBackground(Object... params) {
                String[] res = new String[2];
                try {
                    //call first microsoft
                    String microsoftResulst = process();

                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(5);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    String googleResults = convertGoogleResponseToString(response);

                     res[0] = microsoftResulst;
                     res[1] = googleResults;

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                    res[0] = e.getContent();
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                } catch (VisionServiceException e) {
                    Log.d(TAG,"failed to call microsoft APIs "+e.getMessage());
                    res[1] = e.getMessage();
                }
                return res;
            }

            protected void onPostExecute(String[] result) {
                String microsoftFinalResults = getMicrosoftDataPretty(result[0]);
                String googleFinalResults = result[1];

                String matches = getMatches(microsoftFinalResults,googleFinalResults);
                System.out.println("MATCHES: "+matches);

                Intent showImageFullscreen = new Intent(MainActivity.this,ShowPictureActivity.class);
                showImageFullscreen.putExtra("IMAGE",getCameraFile().getAbsolutePath());
                showImageFullscreen.putExtra("VALUES",googleFinalResults);
                showImageFullscreen.putExtra("MATCHES", matches);
                startActivityForResult(showImageFullscreen,SHOW_PICTURE_ACTIVITY);
                //System.out.println(result[0]+ "\n"+result[1]);

                mProgressDialog.dismiss();
            }
        }.execute();
    }

    private String getMatches(String microsoftFinalResults, String googleFinalResults) {
        String matches ="";
        Scanner microsoftScanner = new Scanner(microsoftFinalResults);
        microsoftScanner.useDelimiter(":");
        Scanner googleScanner = new Scanner(googleFinalResults);
        googleScanner.useDelimiter(":|\\n|\\s ");

        while(googleScanner.hasNext()){
            String nextGoogle = googleScanner.next();
            if(!nextGoogle.startsWith("0")){
                while(microsoftScanner.hasNext()){
                    String nextMicrosoft = microsoftScanner.next();
                    if(nextGoogle.contains(nextMicrosoft))
                        matches+= nextGoogle;
                }
            }
            microsoftScanner = new Scanner(microsoftFinalResults);
            microsoftScanner.useDelimiter(":");
        }
        return matches;
    }

    private String getMicrosoftDataPretty(String s) {
        Gson gson = new Gson();
        AnalysisResult analysisResult = gson.fromJson(s, AnalysisResult.class);

        /*for (Caption caption: result.description.captions) {
            res += ("Caption: " + caption.text + ", confidence: " + caption.confidence + " ");
        }*/
        String res = "";
        for (String tag: analysisResult.description.tags) {
            res+=( tag +":");
        }
        return res;
    }

    public static Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }

        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertGoogleResponseToString(BatchAnnotateImagesResponse response) {
        String message = "";

        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                message += String.format("%.3f: %s", label.getScore(), label.getDescription());
                message += "\n";
            }
        } else {
            message += "nothing";
        }

        return message;
    }


    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        AnalysisResult v = this.client.describe(inputStream, 1);

        String result = gson.toJson(v);
        Log.d("result", result);

        return result;
    }


}
