package com.microsoft.cognitiveservices.speech.samples.quickstart;

import android.app.SearchManager;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.speech.tts.TextToSpeech;
import java.util.Locale;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    // Replace below with your own subscription key
    private static String speechSubscriptionKey = "1e8f878e47964d568280b9434902af91";
    // Replace below with your own service region (e.g., "westus").
    private static String serviceRegion = "westus";

    private static String primaryToken = "KJyTilMi2EA.cwA.UjY.WDd-zeGtpG9s41Sp_E11jEDb7EkTNvIHmShSTZFtRo8";
    private static String botName = "nandakke_qna_bot";
    private static String primaryToken2 = "7yTJO2s_wP4.cwA.bcc.AVTtKCskIzkZ4FqX1XkQld9hM6tNlW3DgGSPBvBwO04";
    private static String botName2 = "shimabot";

    private Button recognizeContinuousButton;

    private String conversationId = "";
    private String localToken = "";


    private String conversationId2 = "";
    private String localToken2 = "";

    private TextView recognizedTextView;


    private TextToSpeech mTextToSpeech; // 音声読上用

    private MicrophoneStream microphoneStream;
    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }

        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }



    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        public void run() {
            pollBotResponses();
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TextToSpeech作成
        mTextToSpeech = new TextToSpeech(this,this);

        recognizeContinuousButton = (Button)findViewById(R.id.button2);


        // create config
        final SpeechConfig speechConfig;
        try {
            speechConfig = SpeechConfig.fromSubscription("1e8f878e47964d568280b9434902af91", "westus");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
            return;
        }


        //アプリ起動時にBotと接続する
        new Connection().execute("");

        // Note: we need to request the permissions
        int requestCode = 5; // unique code for the permission request
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, requestCode);
        runnable.run();

        ///////////////////////////////////////////////////
        // recognize continuously
        ///////////////////////////////////////////////////
        recognizeContinuousButton.setOnClickListener(new View.OnClickListener() {
            private static final String logTag = "reco 3";
            private boolean continuousListeningStarted = false;
            private SpeechRecognizer reco = null;
            private AudioConfig audioInput = null;
            private String buttonText = "";
            private ArrayList<String> content = new ArrayList<>();

            @Override
            public void onClick(final View view) {
                final Button clickedButton = (Button) view;
                // disableButtons();
                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> {
                                clickedButton.setText(buttonText);
                            });
                            //   enableButtons();
                            continuousListeningStarted = false;
                        });
                    } else {
                        continuousListeningStarted = false;
                    }

                    return;
                }

                clearTextBox();

                try {
                    content.clear();

                    // audioInput = AudioConfig.fromDefaultMicrophoneInput();
                    audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
                    reco = new SpeechRecognizer(speechConfig, audioInput);

                    reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                        Log.i(logTag, "Intermediate result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                        content.remove(content.size() - 1);
                    });

                    reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                        Log.i(logTag, "Final result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                    });

                    final Future<Void> task = reco.startContinuousRecognitionAsync();
                    setOnTaskCompletedListener(task, result -> {
                        continuousListeningStarted = true;
                        MainActivity.this.runOnUiThread(() -> {
                            buttonText = clickedButton.getText().toString();
                            clickedButton.setText("Stop");
                            clickedButton.setEnabled(true);
                        });
                    });
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }
            }
        });

    }






    public void onSpeechButtonClicked(View v) {

        TextView txt = (TextView) this.findViewById(R.id.hello); //
        TextView resTxt = (TextView) this.findViewById(R.id.resTextView); //
        TextView resTxt2 = (TextView) this.findViewById(R.id.resTextView2); //

        //質問ボタンを押下時にビープ音を鳴らす
        ToneGenerator toneGenerator
                = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT);

        //質問をクリア
        txt.setText("");

        try {
            SpeechConfig config = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
            config.setSpeechRecognitionLanguage("ja-JP");
            assert(config != null);

            SpeechRecognizer reco = new SpeechRecognizer(config);
            assert(reco != null);

            Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();
            assert(task != null);

            // Note: this will block the UI thread, so eventually, you want to
            //        register for the event (see full samples)
            SpeechRecognitionResult result = task.get();
            assert(result != null);



            if (result.getReason() == ResultReason.RecognizedSpeech) {

                //認識した音声を表示する
                txt.setText(result.getText());

                //BOTからの回答をクリアする
                resTxt.setText("");
                resTxt2.setText("");

                //BOTに認識した音声を投げる
                sendMessageToBot(result.getText(), conversationId,localToken);
                sendMessageToBot(result.getText(), conversationId2,localToken2);
            }
            else {
                txt.setText("音声をうまく認識できませんした。ボタンを押下してもう一度最初からお願いします");
            }

            reco.close();
        } catch (Exception ex) {
            Log.e("SpeechSDKDemo", "unexpected " + ex.getMessage());
            assert(false);
        }
    }

    private class Connection extends AsyncTask<String,Integer,String> {

        @Override
        protected String doInBackground(String... arg0) {
            String conversationTokenInfo = startConversation(primaryToken);
            String conversationTokenInfo2 = startConversation(primaryToken2);
            JSONObject jsonObject = null;
            JSONObject jsonObject2 = null;

            if(conversationTokenInfo != "") {
                try {
                    jsonObject = new JSONObject(conversationTokenInfo);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if(conversationTokenInfo2 != "") {
                try {
                    jsonObject2 = new JSONObject(conversationTokenInfo2);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            //send message to bot and get the response using the api conversations/{conversationid}/activities
            if(jsonObject != null) {
                try {
                    conversationId = jsonObject.get("conversationId").toString();
                    localToken = jsonObject.get("token").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if(jsonObject2 != null) {
                try {
                    conversationId2 = jsonObject2.get("conversationId").toString();
                    localToken2 = jsonObject2.get("token").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if(conversationId != "") {
                //sendMessageToBot(arg0[0]);
            }

            return null;
        }

    }

    private String startConversation(String token)
    {
        //Only for demo sake, otherwise the network work should be done over an asyns task
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String UrlText = "https://directline.botframework.com/v3/directline/conversations";
        URL url = null;
        String responseValue = "";

        try {
            url = new URL(UrlText);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection urlConnection = null;
        try {
            String basicAuth = "Bearer "  + token;
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", basicAuth);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            responseValue = readStream(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            urlConnection.disconnect();
        }

        return  responseValue;
    }

    //read the chat bot response
    private String readStream(InputStream in) {
        char[] buf = new char[2048];
        Reader r = null;
        try {
            r = new InputStreamReader(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuilder s = new StringBuilder();
        while (true) {
            int n = 0;
            try {
                n = r.read(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (n < 0)
                break;
            s.append(buf, 0, n);
        }

        Log.w("streamValue",s.toString());
        return s.toString();
    }

    private void sendMessageToBot(String messageText, String conId, String localTk) {
        //Only for demo sake, otherwise the network work should be done over an asyns task
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String UrlText = "https://directline.botframework.com/v3/directline/conversations/" + conId + "/activities";
        URL url = null;

        try {
            url = new URL(UrlText);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection urlConnection = null;
        try {
            String basicAuth = "Bearer " + localTk;

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("type","message");
                jsonObject.put("text",messageText);
                jsonObject.put("from",(new JSONObject().put("id","user1")));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String postData = jsonObject.toString();

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", basicAuth);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Content-Length", "" + postData.getBytes().length);
            OutputStream out = urlConnection.getOutputStream();
            out.write(postData.getBytes());

            int responseCode = urlConnection.getResponseCode(); //can call this instead of con.connect()
            if (responseCode >= 400 && responseCode <= 499) {
                throw new Exception("Bad authentication status: " + responseCode); //provide a more meaningful exception message
            }
            else {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String responseValue = readStream(in);
                Log.w("responseSendMsg ",responseValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            urlConnection.disconnect();
        }

    }

    private String getBotResponse(String conId, String localTk) {
        //Only for demo sake, otherwise the network work should be done over an asyns task
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String UrlText = "https://directline.botframework.com/v3/directline/conversations/" + conId + "/activities";
        URL url = null;
        String responseValue = "";

        try {
            url = new URL(UrlText);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection urlConnection = null;
        try {
            String basicAuth = "Bearer " + localTk;
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", basicAuth);
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type", "application/json");

            int responseCode = urlConnection.getResponseCode(); //can call this instead of con.connect()
            if (responseCode >= 400 && responseCode <= 499) {
                throw new Exception("Bad authentication status: " + responseCode); //provide a more meaningful exception message
            }
            else {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                responseValue = readStream(in);
                Log.w("responseSendMsg ",responseValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            urlConnection.disconnect();
        }

        return responseValue;
    }

    public void pollBotResponses()
    {
        //Toast.makeText(getBaseContext(),
        //       "test",
        //     Toast.LENGTH_SHORT).show();
        String botResponse = "";
        String botResponse2 = "";
        if(conversationId != "" && localToken != "") {
            botResponse = getBotResponse(conversationId,localToken);
            if (botResponse != "") {
                try {
                    JSONObject jsonObject = new JSONObject(botResponse);
                    String responseMsg = "";
                    Integer arrayLength = jsonObject.getJSONArray("activities").length();
                    String msgFrom = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).getJSONObject("from").get("id").toString();
                    String curMsgId = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).get("id").toString();
                    TextView resTxt = (TextView) this.findViewById(R.id.resTextView); //

                    if (msgFrom.trim().toLowerCase().equals(botName)) {

                        responseMsg = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).get("text").toString();

                        speechText(responseMsg);
                        resTxt.setText(responseMsg);




                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if(conversationId2 != "" && localToken2 != "") {
            botResponse2 = getBotResponse(conversationId2,localToken2);
            if (botResponse2 != "") {
                try {
                    JSONObject jsonObject = new JSONObject(botResponse2);
                    String responseMsg = "";
                    Integer arrayLength = jsonObject.getJSONArray("activities").length();
                    String msgFrom = jsonObject.getJSONArray("activities").getJSONObject(arrayLength-1).getJSONObject("from").get("id").toString();
                    String curMsgId = jsonObject.getJSONArray("activities").getJSONObject(arrayLength-1).get("id").toString();
                    TextView resTxt = (TextView) this.findViewById(R.id.resTextView2); //

                    if(msgFrom.trim().toLowerCase().equals(botName2)) {

                        responseMsg = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).get("text").toString();

                        resTxt.setText(responseMsg);


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        handler.postDelayed(runnable, 1000*5);
    }

    public void resTextView_onClick(View view){
        //質問ボタンを押下時にビープ音を鳴らす
        ToneGenerator toneGenerator
                = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT);

        // インテント作成  引数はIntent.ACTION_WEB_SEARCH固定
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        // putExtraのSearchManager.QUERYに対して検索する文字列を指定する
        intent.putExtra(SearchManager.QUERY, ( (TextView)findViewById(R.id.resTextView)).getText().toString());
        startActivity(intent);


    }




    private void displayException(Exception ex) {
        recognizedTextView.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
    }

    private void clearTextBox() {
      //  AppendTextLine("", true);
    }

    private void setRecognizedText(final String s) {
      //  AppendTextLine(s, true);
    }
    /*
    private void AppendTextLine(final String s, final Boolean erase) {
        MainActivity.this.runOnUiThread(() -> {
            if (erase) {
                recognizedTextView.setText(s);
            } else {
                String txt = recognizedTextView.getText().toString();
                recognizedTextView.setText(txt + System.lineSeparator() + s);
            }
        });
    }
    */
    /*
        private void disableButtons() {
            MainActivity.this.runOnUiThread(() -> {
                recognizeButton.setEnabled(false);
                recognizeIntermediateButton.setEnabled(false);
                recognizeContinuousButton.setEnabled(false);
                recognizeIntentButton.setEnabled(false);
            });
        }
        */
    /*
        private void enableButtons() {
            MainActivity.this.runOnUiThread(() -> {
                recognizeButton.setEnabled(true);
                recognizeIntermediateButton.setEnabled(true);
                recognizeContinuousButton.setEnabled(true);
                recognizeIntentButton.setEnabled(true);
            });
        }
    */
    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    private static ExecutorService s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        if(mTextToSpeech != null){
            mTextToSpeech.shutdown();
        }
    }

    @Override
    public void onInit(int status) {
        if(TextToSpeech.SUCCESS == status){
            Locale locale = Locale.JAPAN;
            if(mTextToSpeech.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE){
                mTextToSpeech.setLanguage(locale);
            } else {
                Log.e("MainActivity","言語設定エラー");
            }
        } else {

            Log.e("MainActivity","TextToSpeech 初期設定エラー");
        }
    }

    private void speechText(String text){
        if(text.length() > 0){
            if(mTextToSpeech.isSpeaking()){
                mTextToSpeech.stop();
            }
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

}

