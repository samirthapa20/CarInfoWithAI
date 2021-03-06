package com.halo.carInfoWithAi.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLLocalModel;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;
import com.halo.carInfoWithAi.Models.Data;
import com.halo.carInfoWithAi.R;
import com.theartofdev.edmodo.cropper.CropImage;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class LPDetectActivity extends AppCompatActivity {
    FirebaseAutoMLRemoteModel remoteModel; // For loading the model remotely
    FirebaseVisionImageLabeler labeler; //For running the image labeler
    FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder optionsBuilder; // Which option is use to run the labeler local or remotely
    ProgressDialog progressDialog; //Show the progress dialog while model is downloading...
    FirebaseModelDownloadConditions conditions; //Conditions to download the model
    FirebaseVisionImage image; // preparing the input image
    TextView textView; // Displaying the label for the input image
    Button button; // To select the image from device
    ImageView imageView; //To display the selected image
    private FirebaseAutoMLLocalModel localModel;
    public String textOfImage;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    DatabaseReference mDatabase;
    private  float confidence;

    private  List<Data> datas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actitity_lp_detect);
        textView = findViewById(R.id.text);
        button = findViewById(R.id.selectImage);
        imageView = findViewById(R.id.image);
        progressDialog = new ProgressDialog(LPDetectActivity.this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCanceledOnTouchOutside(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity().start(LPDetectActivity.this);
                //   fromRemoteModel();
            }
        });

        datas = new ArrayList<>();
//        final String postKey = getIntent().getStringExtra(EXTRA_POST_KEY);
//        if(postKey == null){
//            onBackPressed();
//            return;
//        }
//        fAuth = FirebaseAuth.getInstance();
//        fStore = FirebaseFirestore.getInstance();
//        userID = fAuth.getCurrentUser().getUid();
//        Log.e("postKey",postKey+"");
        mDatabase = FirebaseDatabase.getInstance().getReference().child("CarInfoData");
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue() != null){
                    Log.i("onDataChange","*******************"+snapshot.getValue().toString()  );
                     List<Data> ds =  new ArrayList<>();
                    HashMap hashMap = ((HashMap) snapshot.getValue());
                    Log.i("onDataChange","*******************"+hashMap.toString() );
                     for(Object key : hashMap.keySet().toArray()){
                         Log.i("onDataChange","*******************"+key.toString()  );
                          HashMap objHash =  (HashMap) hashMap.get(key);
                    Data data = new Data(
                            objHash.get("noPlate").toString(),
                            objHash.get("cname").toString(),
                            objHash.get("modelNumber").toString(),
                            objHash.get("ccInfo").toString(),
                            objHash.get("colorInfo").toString(),
                            objHash.get("manufactureDate").toString(),
                            objHash.get("ownerName").toString(),
                            objHash.get("ownerContactInfo").toString(),
                            objHash.get("ownerOccupation").toString(),
                            objHash.get("ownerPhoneNo").toString(),
                            objHash.get("id").toString()
                    );
                         Log.i("onDataChange","******************data added*"+ data.toString()  );
                    ds.add(data);
                     }

                     datas = ds;
                    Log.i("onDataChange","******************data added*"+ datas );
//                    Data data = new Data(
//                            hashMap.get("noPlate").toString(),
//                            hashMap.get("cname").toString(),
//                            hashMap.get("modelNumber").toString(),
//                            hashMap.get("ccInfo").toString(),
//                            hashMap.get("colorInfo").toString(),
//                            hashMap.get("manufactureDate").toString(),
//                            hashMap.get("ownerName").toString(),
//                            hashMap.get("ownerContactInfo").toString(),
//                            hashMap.get("ownerOccupation").toString(),
//                            hashMap.get("ownerPhoneNo").toString(),
//                            hashMap.get("id").toString()
//                    );
//                    //Log.e("snapshot data",data.toString()+"");
//                    CarInfoDetailActivity.this.data = data;
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i("onDataChange","*******************"+error.toString()  );
            }
        });

    }

    private void setLabelerFromLocalModel(Uri uri) {
        localModel = new FirebaseAutoMLLocalModel.Builder()
                .setAssetFilePath("model/manifest.json")
                .build();
        try {
            FirebaseVisionOnDeviceAutoMLImageLabelerOptions options =
                    new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(localModel)
                            .setConfidenceThreshold(0.0f)
                            .build();
            labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);
            image = FirebaseVisionImage.fromFilePath(LPDetectActivity.this, uri);
            processImageLabeler(labeler, image);
        } catch (FirebaseMLException | IOException e) {
            // ...
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                if (result != null) {
                    Uri uri = result.getUri(); //path of image in phone
                    imageView.setImageURI(uri); //set image in imageview
                    textView.setText(""); //so that previous text don't get append with new one
                    setLabelerFromLocalModel(uri);
                    // setLabelerFromRemoteLabel(uri);
                    if(imageView != null) {

                        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();

                        if(!textRecognizer.isOperational()) {
                            // Note: The first time that an app using a Vision API is installed on a
                            // device, GMS will download a native libraries to the device in order to do detection.
                            // Usually this completes before the app is run for the first time.  But if that
                            // download has not yet completed, then the above call will not detect any text,
                            // barcodes, or faces.
                            // isOperational() can be used to check if the required native libraries are currently
                            // available.  The detectors will automatically become operational once the library
                            // downloads complete on device.
//            Log.w(LOG_TAG, "Detector dependencies are not yet available.");

                            // Check for low storage.  If there is low storage, the native library will not be
                            // downloaded, so detection will not become operational.
                            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

                            if (hasLowStorage) {
                                Toast.makeText(this,"Low Storage", Toast.LENGTH_LONG).show();
//                Log.w(LOG_TAG, "Low Storage");
                            }
                        }

                        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                        Bitmap bitmap = drawable.getBitmap();
                        Frame imageFrame = new Frame.Builder()
                                .setBitmap(bitmap)
                                .build();

                        SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < textBlocks.size(); i++) {
                            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                            stringBuilder.append(textBlock.getValue());
                            stringBuilder.append("\n");

//            Log.i(LOG_TAG, textBlock.getValue());
//            // Do something with value
                        }
                        textView.setText(stringBuilder.toString());
                        textOfImage=textView.getText().toString();
                        textView.setVisibility(View.INVISIBLE);
                    }
                } else
                    progressDialog.cancel();
            } else
                progressDialog.cancel();
        }
    }

    private void setLabelerFromRemoteLabel(final Uri uri) {
        FirebaseModelManager.getInstance().isModelDownloaded(remoteModel)
                .addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isComplete()) {
                            optionsBuilder = new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(remoteModel);
                            FirebaseVisionOnDeviceAutoMLImageLabelerOptions options = optionsBuilder
                                    .setConfidenceThreshold(0.0f)
                                    .build();
                            try {
                                labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);
                                image = FirebaseVisionImage.fromFilePath(LPDetectActivity.this, uri);
                                processImageLabeler(labeler, image);
                            } catch (FirebaseMLException | IOException exception) {
                                Log.e("TAG", "onSuccess: " + exception);
                                Toast.makeText(LPDetectActivity.this, "Ml exeception", Toast.LENGTH_SHORT).show();
                            }
                        } else
                            Toast.makeText(LPDetectActivity.this, "Not downloaded", Toast.LENGTH_SHORT).show();

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("TAG", "onFailure: "+e );
                Toast.makeText(LPDetectActivity.this, "err"+e, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void processImageLabeler(FirebaseVisionImageLabeler labeler, FirebaseVisionImage image) {
        labeler.processImage(image).addOnCompleteListener(new OnCompleteListener<List<FirebaseVisionImageLabel>>() {
            @Override
            public void onComplete(@NonNull Task<List<FirebaseVisionImageLabel>> task) {
                progressDialog.cancel();
                for (FirebaseVisionImageLabel label : task.getResult()) {
                    String eachlabel = label.getText().toUpperCase();
                     confidence = label.getConfidence();
                    Log.e( "LANESS: ", eachlabel+eachlabel.toLowerCase().trim()+ eachlabel.toLowerCase().trim().equals("car"));
                    textView.append(eachlabel + " - " + ("" + confidence * 100).subSequence(0, 4) + "%" + "\n\n");

                    if(confidence>0.7 && eachlabel.toLowerCase().trim().equals("car")){
                        // it is car

                        textView.setVisibility(View.VISIBLE);

                        for(Data data:datas){
                            Log.e("DAtasz",data.toString() );
                            Log.e("DAtasz",textOfImage.trim() );
                            Log.e("DAtasz",data.getNoPlate().trim() );
                            Log.e("DAtasz", data.getNoPlate().equals(textOfImage)+"");
                            if(data.getNoPlate().trim().equals(textOfImage.trim())){
                                Intent intent= new Intent(LPDetectActivity.this,CarInfoDetailActivity.class);
                                intent.putExtra(CarInfoDetailActivity.EXTRA_POST_KEY,data.getId());
                                startActivity(intent);
                            }
                        }
                    }
                }
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_VIEW);
//                intent.setData(Uri.parse("https://www.google.com/search?q=" + task.getResult().get(0).getText()));
//                startActivity(intent);

//            TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
//            if (!textRecognizer.isOperational()) {
//                Log.w("LPDetectActivity", "Detector dependencies are not yet available");
//            } else {
//                textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
//                    @Override
//                    public void release() {
//
//                    }
//                    @Override
//                    public void receiveDetections(Detector.Detections<TextBlock> detections) {
//
//                        final SparseArray<TextBlock> items = detections.getDetectedItems();
//                        if(items.size() != 0)
//                        {
//                            textView.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    StringBuilder stringBuilder = new StringBuilder();
//                                    for(int i =0;i<items.size();++i)
//                                    {
//                                        TextBlock item = items.valueAt(i);
//                                        stringBuilder.append(item.getValue());
//                                        stringBuilder.append("\n");
//                                    }
//                                    textView.setText(stringBuilder.toString());
//                                    textOfImage=textView.getText().toString();
//                                }
//                            });
//                        }
//                    }
//                });
//            }
            }


        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("OnFail", "" + e);
                Toast.makeText(LPDetectActivity.this, "Something went wrong! " + e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fromRemoteModel() {
        progressDialog.show();                                         /* model name*/
        remoteModel = new FirebaseAutoMLRemoteModel.Builder("CarDetectionModel").build();
        conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();
        //first download the model
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        CropImage.activity().start(LPDetectActivity.this); // open image crop activity
                    }
                });
    }

}