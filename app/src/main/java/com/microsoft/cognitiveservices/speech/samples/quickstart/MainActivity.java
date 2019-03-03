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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
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

import android.widget.ImageView;
import android.content.pm.ActivityInfo;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    //■ Speech Service
    // speech service subscriptionkey
    private static String speechSubscriptionKey = "1e8f878e47964d568280b9434902af91";
    // region (e.g., "westus").
    private static String serviceRegion = "westus";
    private String gResultString="";

    private TextView recognizedTextView;
    private Button recognizeContinuousButton;

    //■ Bot Service
    // bot service key
    private static String primaryToken = "KJyTilMi2EA.cwA.UjY.WDd-zeGtpG9s41Sp_E11jEDb7EkTNvIHmShSTZFtRo8";
    private static String botName = "nandakke_qna_bot";
    private static String primaryToken2 = "0tyZhernqF8.fnqsTQ3pUWXe3gfEZu7VH7QNprqJmN1EPKqDzPPYBJk";
    private static String botName2 = "ys-temp-bot";
    private static String primaryToken3 = "Oe1ifNUBT4s.07EOcXobHiymaFu8CXyf5sbEK_al6_--YWRumusHSU8";
    private static String botName3 = "nandakke_bot_stable";

    private String conversationId = "";
    private String conversationId2 = "";
    private String conversationId3 = "";

    private String localToken = "";
    private String localToken2 = "";
    private String localToken3 = "";

    private String[] strLogCurId = {"","",""};

    private TextView textResTextView;
    private TextView textResTextView2;
    private TextView textResTextView3;


    //■ 音声出力用
    private Integer sound = 0;
    private Integer intSpeakEnable = 0;
    private TextToSpeech mTextToSpeech;

    private MicrophoneStream microphoneStream;
    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    private Integer intSpeekCnt=0;

    // ポーリングによるBot応答用
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        public void run() {
            pollBotResponses(conversationId, localToken, R.id.resTextView, botName,  1,1.0f);
            pollBotResponses(conversationId2, localToken2, R.id.resTextView2, botName2, 2,0.5f);
            pollBotResponses(conversationId3, localToken3, R.id.resTextView3, botName3, 3,1.8f);
            gResultString = "";
            handler.postDelayed(runnable, 1000*2);
        }
    };

    //タイトルボタン押下時処理(OnCreateの処理をこっちにもってきた)
    public void onTitleButtonClicked(View v) {

        //main activity表示
        setContentView(R.layout.activity_main);

        recognizedTextView = findViewById(R.id.hello);
        textResTextView =  findViewById(R.id.resTextView);
        textResTextView2 =  findViewById(R.id.resTextView2);
        textResTextView3 =  findViewById(R.id.resTextView3);

        // TextToSpeech作成(Androidのライブラリ。
        mTextToSpeech = new TextToSpeech(this,this);

        // Speech Service
        recognizeContinuousButton = (Button)findViewById(R.id.button2);
        //recognizeContinuousButton.setEnabled(false);
        //recognizeContinuousButton.setVisibility(View.INVISIBLE);


        // create config
        final SpeechConfig speechConfig;
        try {
            speechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey,  serviceRegion);
            speechConfig.setSpeechRecognitionLanguage("ja-JP");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
            return;
        }

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

                //質問ボタンを押下時にビープ音を鳴らす
                ToneGenerator toneGenerator
                        = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
                toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT);

                //BOTからの回答をクリアする
                textResTextView.setText("");
                textResTextView2.setText("");
                textResTextView3.setText("");
                sound = 0;
                disableButtons();


                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> {
                                clickedButton.setText(buttonText);
                            });
                            enableButtons();
                            continuousListeningStarted = false;
                            gResultString = content.toString();
                        });
                    } else {
                        continuousListeningStarted = false;
                    }
                    return;
                }

                clearTextBox();

                try {
                    content.clear();

                    audioInput = AudioConfig.fromDefaultMicrophoneInput();
                    //audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
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
                            clickedButton.setText("話すのを止める");
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        //アプリの画面を縦固定にする
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //タイトル画面を設定する
        setContentView(R.layout.titlelayout);

        //Botと接続開始
        new Connection().execute("");

        //GIFのアニメーションを実行する(3回繰り返して停止)
        ImageView imageView = (ImageView) findViewById(R.id.gifView);
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(imageView, 3);
        Glide.with(this).load(R.raw.logo).into(target);

    }

    public void onSpeechButtonClicked(View v) {

        //質問ボタンを押下時にビープ音を鳴らす
        ToneGenerator toneGenerator
                = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT);

        //質問をクリア
        recognizedTextView.setText("");

        //音声出力中の場合は停止する
        if(mTextToSpeech.isSpeaking()){
            mTextToSpeech.stop();
        }

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
            //if (true) {

                //認識した音声を表示する
                recognizedTextView.setText(result.getText());
                //txt.setText("サンプル質問になりますので回答をお願いします。");

                gResultString = result.getText();
                //gResultString = "今日は何の日ですか？";

                //BOTからの回答をクリアする
                textResTextView.setText("");
                textResTextView2.setText("");
                textResTextView3.setText("");

                //BOTに認識した音声を投げる
                //sendMessageToBot(result.getText(), conversationId,localToken);
                //sendMessageToBot(result.getText(), conversationId2,localToken2);
                //sendMessageToBot(result.getText(), conversationId3,localToken3);

                sound = 0;
            }
            else {
                recognizedTextView.setText("声が小さいかも。もう一回話して");
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
            String conversationTokenInfo3 = startConversation(primaryToken3);
            JSONObject jsonObject = null;
            JSONObject jsonObject2 = null;
            JSONObject jsonObject3 = null;

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

            if(conversationTokenInfo3 != "") {
                try {
                    jsonObject3 = new JSONObject(conversationTokenInfo3);
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

            if(jsonObject3 != null) {
                try {
                    conversationId3 = jsonObject3.get("conversationId").toString();
                    localToken3 = jsonObject3.get("token").toString();
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

    public void pollBotResponses(String strConvId, String strToken,  Integer intViewId, String StrBotName,Integer intSpeakId, float fSpeakSpeed)
    {

        String botResponse = "";
        if(gResultString !="") {
            sendMessageToBot(gResultString, strConvId,strToken);
        }

        if(strConvId != "" && strToken != "") {
            botResponse = getBotResponse(strConvId,strToken);
            if (botResponse != "") {
                try {
                    JSONObject jsonObject = new JSONObject(botResponse);
                    String responseMsg = "";
                    Integer arrayLength = jsonObject.getJSONArray("activities").length();
                    String msgFrom = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).getJSONObject("from").get("id").toString();
                    String curMsgId = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).get("id").toString();
                    TextView resTxt = (TextView) this.findViewById(intViewId); //

                    if (msgFrom.trim().toLowerCase().equals(StrBotName)) {
                        responseMsg = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).get("text").toString();
                     //   Log.e("debug_id",curMsgId+responseMsg);
                     //   Log.e(      "debug_id", strLogCurId[intSpeakId-1]);

                        /* 同一会話の場合は出力しない */
                        if( !strLogCurId[intSpeakId-1].equals(curMsgId) ) {
                            resTxt.setText(responseMsg);


                            /* 正常応答の場合のみ、音声出力済みとする */
                            if( intSpeekCnt == 1) {
                                if (speechText(responseMsg, fSpeakSpeed) == Boolean.TRUE) {
                                    strLogCurId[intSpeakId - 1] = curMsgId;
                                }
                            }

                            if (intSpeakId == 3) {
                                if( intSpeekCnt == 0 ){
                                    intSpeekCnt = 1;
                                }else{
                                    intSpeekCnt = 0;
                                }
                            }

                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void resTextView_onClick(View view){
        //効果音を出す
        ToneGenerator toneGenerator
                = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD);

        sound = 1;

        // インテント作成  引数はIntent.ACTION_WEB_SEARCH固定
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        // putExtraのSearchManager.QUERYに対して検索する文字列を指定する
        intent.putExtra(SearchManager.QUERY, ( (TextView)findViewById(view.getId())).getText().toString());
        startActivity(intent);
    }


    private void displayException(Exception ex) {
        recognizedTextView.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
    }

    private void clearTextBox() {
        AppendTextLine("", true);
    }

    private void setRecognizedText(final String s) {
        AppendTextLine(s, true);
    }

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


        private void disableButtons() {
            MainActivity.this.runOnUiThread(() -> {
                recognizeContinuousButton.setEnabled(false);
              });
        }


        private void enableButtons() {
            MainActivity.this.runOnUiThread(() -> {
                recognizeContinuousButton.setEnabled(true);
            });
        }

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
                intSpeakEnable = 1;
            } else {
                Log.e("MainActivity","言語設定エラー");
            }
        } else {

            Log.e("MainActivity","TextToSpeech 初期設定エラー");
        }
    }

    private Boolean speechText(String text, float fSpeakSpeed){

        if(sound == 1 ) {
            if(mTextToSpeech.isSpeaking()){
                mTextToSpeech.stop();
                return Boolean.TRUE;
            }
        }

        //Oninit完了していない場合はエラーを返す
        if( intSpeakEnable != 1) {
            return Boolean.FALSE;

        }

        if(text.length() > 0){
            mTextToSpeech.setSpeechRate(fSpeakSpeed);
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, null);

        }
        return Boolean.TRUE;
    }

}

