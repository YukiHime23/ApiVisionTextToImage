package com.example.apivisiontexttoimage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    TextView textView;
    Button btnGetText,btnReadText;
    TextRecognizer textRecognizer;

    private TextToSpeech textToSpeech;
    private boolean ready;


    private final static int PICK_IMAGE_REQUEST = 100;
    private final static int Request_code_camera = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.txt_text_show);
        imageView = findViewById(R.id.camera_get);
        btnGetText = findViewById(R.id.btn_get_text);
        btnReadText = findViewById(R.id.btn_read_text);
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(intent,Request_code_camera);
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"select picture"),PICK_IMAGE_REQUEST);
            }
        });
    }

    public void setButtonGetText(final Bitmap bit){
        btnGetText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (textRecognizer.isOperational() && bit != null){
                    Frame frame = new Frame.Builder().setBitmap(bit).build();
                    final SparseArray<TextBlock> items = textRecognizer.detect(frame);
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i=0 ; i<items.size(); i++)
                    {
                        TextBlock item = items.valueAt(i);
                        stringBuilder.append(item.getValue());
                        stringBuilder.append("\n");
//                        for (Text line : item.getComponents()){
//                            stringBuilder.append(line.getValue());
//                        }
                    }
                    textView.setText(stringBuilder.toString());
                    if (items.size() == 0) {
                        textView.setText("Scan Failed: Found nothing to scan");
                    } else {
                        textView.setText(stringBuilder.toString());
                    }
                } else {
                    textView.setText("Could not set up the detector!");
                }

                btnReadText.setEnabled(true);
                setBtnReadText();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK && data != null){
            if(requestCode == Request_code_camera){
                Bitmap bit = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(bit);
                btnGetText.setEnabled(true);
                setButtonGetText(bit);
            }
            if(requestCode == PICK_IMAGE_REQUEST){
                try {
                    Bitmap bit = MediaStore.Images.Media.getBitmap(this.getContentResolver(),data.getData());
                    imageView.setImageBitmap(bit);
                    btnGetText.setEnabled(true);
                    setButtonGetText(bit);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void speakOut() {
        if (!ready) {
            Toast.makeText(this, "Text to Speech not ready", Toast.LENGTH_LONG).show();
            return;
        }

        // Văn bản cần đọc.
        String toSpeak = textView.getText().toString();
        // Một String ngẫu nhiên (ID duy nhất).
        String utteranceId = UUID.randomUUID().toString();
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    @Override
    protected void onPause() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    public void setBtnReadText(){
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                Set<Locale> language = textToSpeech.getAvailableLanguages();
                if(language!= null) {
                    for (Locale lang : language) {
                        Log.e("TTS", "Supported Language: " + lang);
                    }
                }

                Locale lang = Locale.ENGLISH;
                int result = textToSpeech.setLanguage(lang);
                if (result == TextToSpeech.LANG_MISSING_DATA) {
                    ready = false;
                    Log.e("Msg","Missing language data");
                    return;
                } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ready = false;
                    Log.e("Msg","Language not supported");
                    return;
                } else {
                    ready = true;
                    Locale currentLanguage = textToSpeech.getVoice().getLocale();
                    Log.e("Msg","Language: "+currentLanguage);
                }
            }
        });
        btnReadText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakOut();
            }
        });
    }

}
