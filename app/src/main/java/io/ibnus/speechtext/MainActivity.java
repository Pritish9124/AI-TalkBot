package io.ibnus.speechtext;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.Snackbar;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.yalantis.waves.util.Horizon;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    public  static final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    private TextView bot_user_status;
    private Button recordBtn,stop_btn;

    boolean playing = false;


    boolean autoBotPlay = false;

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = "temp_audio.wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    MediaPlayer mp;


    boolean first_answer = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bot_user_status= (TextView) findViewById(R.id.user_bot_text);
        recordBtn = (Button) findViewById(R.id.recording_btn);

        checkPermission();
        String filePath = Environment.getExternalStorageDirectory()+"/new_audio.3gp";





        playDefaultBotAudio();


        bufferSize = AudioRecord.getMinBufferSize(8000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);



        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                {

                    if (!playing)
                    {
                        recordBtn.setText("Stop");
                        playing = true;
                        bot_user_status.setText("User is talking...");
                        startRecording();
                    }
                    else
                    {
                        bot_user_status.setText("waiting for Bot's reply...");
                        recordBtn.setText("Talk");
                        playing = false;
                        stopRecording();
                    }

                }
                else
                {
                    checkPermission();
                }

            }
        });




    }

    private void playDefaultBotAudio() {





        bot_user_status.setText("Bot is talking...");
        autoBotPlay = true;

        convertToAudio("<speak>हिंदुस्तान कोका कोला कंपनी में आपका स्वागत है<break time=\"1s\"/>मैं आपकी कंप्लेंट रजिस्ट्रेशन और शिकायत पंजीकरण में मदद कर सकता हूं<break time=\"1s\"/> कृपया मुझे अपना सीरियल नंबर बताएं</speak>");
    }


    private void convertToText(final String encoded_audio) {


        recordBtn.setVisibility(View.GONE);

        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, "https://speech.googleapis.com/v1/speech:recognize?key=AIzaSyBMss6M4sPupqDXw1oXRl77Wq8h66kQ53g", new Response.Listener<String>() {
            @Override
            public void onResponse(String response)
            {

                Log.d("Response", response);

                try
                {
                    JSONObject jsonObject = new JSONObject(response);

                    JSONArray jsonArray = jsonObject.getJSONArray("results");

                    JSONObject jsonObject1 = jsonArray.getJSONObject(0);
                    JSONArray jsonArray2 = jsonObject1.getJSONArray("alternatives");

                    JSONObject jsonObject2 = jsonArray2.getJSONObject(0);

                    Log.e("trasnacdscd",jsonObject2.getString("transcript"));

                    String user_reply = jsonObject2.getString("transcript");


                    boolean hasDigit = user_reply.matches(".*\\d+.*");

                    if (!first_answer)
                    {
                        if (hasDigit)
                        {
                            first_answer = true;
                            convertToAudio("<speak>कृपया प्रतीक्षा करें ताकि रिकॉर्डिंग की जांच की जा सके,<break time=\"2s\"/> कृपया पुष्टि करें कि श्री अनुराग के नाम पर पंजीकृत है</speak>");

                        }
                        else
                        {
                            convertToAudio("<speak>कृपया प्रतीक्षा करें ताकि रिकॉर्डिंग की जांच की जा सके<break time=\"2s\"/> आपका बताया हुआ रजिस्ट्रेशन नंबर गलत है कृपया दोबारा प्रयास करें</speak>");

                        }
                    }
                    else
                    {


                        if (user_reply.contains("हां"))
                        {
                            convertToAudio(" <speak>कृपया बताएं कि कूलर में क्या समस्या है</speak> ");

                        }

                       else if (user_reply.contains("कूलर"))
                        {
                            convertToAudio(" <speak>आपको हुई दुविधा के लिए क्षमा करें<break time=\"1s\"/> हमने आपकी शिकायत दर्ज कर ली है, हमारी सेवा टेक्नीशियन आ जाएगी और 36 घंटे के भीतर समस्या को ठीक कर देगी<break time=\"1s\"/> हिंदुस्तान कोका कोला कंपनी को बुलाने के लिए धन्यवाद आपका दिन अच्छा रहे</speak>");

                        }
                        else
                        {
                            convertToAudio(" <speak>क्षमा करना</speak> ");
                        }
                    }





                    // askforPermission();
                } catch (JSONException e) {
                    e.printStackTrace();

                }


            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error)
            {


                Toast.makeText(MainActivity.this,"Oops!!! Server Error", Toast.LENGTH_LONG).show();
            }
        }) {




            @Override
            public byte[] getBody() throws AuthFailureError {

                JSONObject mainObject = new JSONObject();


                try {

                    JSONObject childObject1 = new JSONObject();
                    childObject1.put("languageCode","hi-IN");
                    childObject1.put("enableWordTimeOffsets",false);
                    childObject1.put("audio_channel_count",1);

                    JSONObject childObject2 = new JSONObject();
                    childObject2.put("content",encoded_audio);

                    mainObject.put("config",childObject1);
                    mainObject.put("audio",childObject2);


                    Log.e("mainobj",mainObject.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }




                return mainObject.toString().getBytes();
            };

            public String getBodyContentType()
            {
                return "application/json; charset=utf-8";
            }


        };

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(MyStringRequest);
    }

    private void convertToAudio(final String translated_text) {

        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, "https://texttospeech.googleapis.com/v1/text:synthesize?fields=audioContent&key=AIzaSyBMss6M4sPupqDXw1oXRl77Wq8h66kQ53g", new Response.Listener<String>() {
            @Override
            public void onResponse(String response)
            {

                Log.d("Response2", response);

                try
                {
                    JSONObject jsonObject = new JSONObject(response);
                    writeToFile(jsonObject.getString("audioContent"));

                    File file = new File(Environment.getExternalStorageDirectory() + "/testing.mp3");

                    decodeAudio(jsonObject.getString("audioContent"),file,Environment.getExternalStorageDirectory() + "/testing.mp3");

                   // botAudioToText(jsonObject.getString("audioContent"));
                    // askforPermission();
                } catch (JSONException e) {
                    e.printStackTrace();

                }


            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error)
            {


                Toast.makeText(MainActivity.this,"Oops!!! Server Error", Toast.LENGTH_LONG).show();
            }
        }) {




            @Override
            public byte[] getBody() throws AuthFailureError {

                JSONObject mainObject = new JSONObject();


                try {

                    JSONObject childObject1 = new JSONObject();
                    childObject1.put("ssml",translated_text);

                    JSONObject childObject2 = new JSONObject();
                    childObject2.put("languageCode","hi-IN");
                    childObject2.put("ssmlGender","MALE");

                    JSONObject childObject3 = new JSONObject();
                    childObject3.put("audioEncoding","MP3");



                    mainObject.put("audioConfig",childObject3);
                    mainObject.put("input",childObject1);
                    mainObject.put("voice",childObject2);


                    Log.e("mainobj",mainObject.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }




                return mainObject.toString().getBytes();
            };

            public String getBodyContentType()
            {
                return "application/json; charset=utf-8";
            }


        };

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(MyStringRequest);
    }

    private void botAudioToText(final String audioContent) {


        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, "https://speech.googleapis.com/v1/speech:recognize?key=AIzaSyBMss6M4sPupqDXw1oXRl77Wq8h66kQ53g", new Response.Listener<String>() {
            @Override
            public void onResponse(String response)
            {

                Log.d("Response", response);

                try
                {
                    JSONObject jsonObject = new JSONObject(response);

                    JSONArray jsonArray = jsonObject.getJSONArray("results");

                    JSONObject jsonObject1 = jsonArray.getJSONObject(0);
                    JSONArray jsonArray2 = jsonObject1.getJSONArray("alternatives");

                    JSONObject jsonObject2 = jsonArray2.getJSONObject(0);

                    Log.e("trasnacdscd",jsonObject2.getString("transcript"));

                    String user_reply = jsonObject2.getString("transcript");


                    recordBtn.setVisibility(View.VISIBLE);



                    // askforPermission();
                } catch (JSONException e) {
                    e.printStackTrace();

                }


            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error)
            {


                Toast.makeText(MainActivity.this,"Oops!!! Server Error", Toast.LENGTH_LONG).show();
            }
        }) {




            @Override
            public byte[] getBody() throws AuthFailureError {

                JSONObject mainObject = new JSONObject();


                try {

                    JSONObject childObject1 = new JSONObject();
                    childObject1.put("languageCode","hi-IN");
                    childObject1.put("enableWordTimeOffsets",false);
                    childObject1.put("audio_channel_count",1);

                    JSONObject childObject2 = new JSONObject();
                    childObject2.put("content",audioContent);

                    mainObject.put("config",childObject1);
                    mainObject.put("audio",childObject2);


                    Log.e("mainobj",mainObject.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }




                return mainObject.toString().getBytes();
            };

            public String getBodyContentType()
            {
                return "application/json; charset=utf-8";
            }


        };

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(MyStringRequest);
    }


    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) +
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)+
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {

                Snackbar.make(MainActivity.this.findViewById(android.R.id.content),
                        "Please Grant Permissions to upload Home photo",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                requestPermissions(
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, PERMISSIONS_MULTIPLE_REQUEST);
                            }
                        }).show();
            } else {
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                        PERMISSIONS_MULTIPLE_REQUEST);
            }
        } else {
            // write code if permission already granted
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean recordingPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    boolean readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeExternalFile = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if(recordingPermission && readExternalFile && writeExternalFile)
                    {
                        // write your logic here
                    } else {
                        Snackbar.make(MainActivity.this.findViewById(android.R.id.content),
                                "Please Grant Permissions to record Audio",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        requestPermissions(
                                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                                PERMISSIONS_MULTIPLE_REQUEST);
                                    }
                                }).show();
                    }
                }
                break;
        }
    }





    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private String getTempFilename2(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_FILE_EXT_WAV);


        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_FILE_EXT_WAV);
    }



    private void startRecording(){


        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        int i = recorder.getState();
        if(i==1)

            Log.w("started","started");
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {

                Log.w("started","started2");
                writeAudioDataToFile();
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }

    private void writeAudioDataToFile(){
        byte data[] = new byte[bufferSize];


        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;

        if(null != os){
            while(isRecording){
                read = recorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {
                        os.write(data);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording(){

        Log.w("stop","stopped");
        if(null != recorder){
            isRecording = false;

            int i = recorder.getState();
            if(i==1)


                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(),getFilename());
        deleteTempFile();

        File file = new File(getTempFilename2());

        String encode_audio = getStringFile(file);

        writeToFile(encode_audio);
        Log.e("encoded",encode_audio);
        convertToText(encode_audio);
    }

    private void deleteTempFile() {

        Log.w("deleteeFile","");
     //   File file = new File(getTempFilename());

      //  file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){

        Log.w("copyWaveFile","");
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

             Log.w("pathhh",outFilename);
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate,outFilename);

            while(in.read(data) != -1){
                out.write(data);

            }

            in.close();
            out.close();



        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate, String outFilename) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);



    }



    public String getStringFile(File f) {
        InputStream inputStream = null;
        String encodedFile= "", lastVal;
        try {
            inputStream = new FileInputStream(f.getAbsolutePath());

            byte[] buffer = new byte[10240];//specify the size to allow
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output64.write(buffer, 0, bytesRead);
            }


            output64.close();


            encodedFile =  output.toString();

        }
        catch (FileNotFoundException e1 ) {
            e1.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        lastVal = encodedFile;
        return lastVal.replaceAll("\\n","");
    }

    private void writeToFile(String content) {




        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/test.txt");

            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
        }
    }



    File decodedFile(String encoded)
    {


        return null;
    }


    private void decodeAudio(String base64AudioData, File fileName, String path) {

        try {

            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(Base64.decode(base64AudioData.getBytes(), Base64.DEFAULT));
            fos.close();
            bot_user_status.setText("Bot is talking...");
            try {

               mp  = new MediaPlayer();
                mp.setDataSource(path);

                mp.prepare();
                mp.start();

                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        recordBtn.setVisibility(View.VISIBLE);
                        bot_user_status.setText("Click on talk with bot...");
                    }
                });

            } catch (Exception e) {

               e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        mp.stop();
        mp.release();
    }



}
