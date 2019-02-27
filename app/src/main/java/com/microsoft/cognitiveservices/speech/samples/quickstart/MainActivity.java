package com.microsoft.cognitiveservices.speech.samples.quickstart;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;



import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.samples.quickstart.HttpResponsAsync;


import java.util.concurrent.Future;



import static android.Manifest.permission.*;

import android.os.StrictMode; //direct line sample 追加
import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Reader;
import java.io.InputStreamReader;
import android.os.AsyncTask;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.os.Handler;


import android.media.AudioManager;
import android.media.ToneGenerator;

public class MainActivity extends AppCompatActivity {

    // Replace below with your own subscription key
    private static String speechSubscriptionKey = "5fe3ee808b15424a851a42a1303f77ab";
    // Replace below with your own service region (e.g., "westus").
    private static String serviceRegion = "westus";

    private static String primaryToken = "KJyTilMi2EA.cwA.UjY.WDd-zeGtpG9s41Sp_E11jEDb7EkTNvIHmShSTZFtRo8";
    private static String botName = "nandakke_qna_bot";
    private static String primaryToken2 = "7yTJO2s_wP4.cwA.bcc.AVTtKCskIzkZ4FqX1XkQld9hM6tNlW3DgGSPBvBwO04";
    private static String botName2 = "shimabot";


    private String conversationId = "";
    private String localToken = "";
    private String lastResponseMsgId = "";

    private String conversationId2 = "";
    private String localToken2 = "";
    private String lastResponseMsgId2 = "";


    private ArrayList<ChatMessage> chatHistory;

    private Integer apl_status = 0;

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




        new Connection().execute("青葉区");

        // Note: we need to request the permissions
        int requestCode = 5; // unique code for the permission request
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, requestCode);
        runnable.run();
    }

    public void onSpeechButtonClicked(View v) {
        TextView txt = (TextView) this.findViewById(R.id.hello); // 'hello' is the ID of your text view
        TextView resTxt = (TextView) this.findViewById(R.id.resTextView); //

        ToneGenerator toneGenerator
                = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT);

        txt.setText(""); //質問をクリア

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

            apl_status = 1; //音声認識開始

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                txt.setText(result.getText());
                resTxt.setText("");
                sendMessageToBot(result.getText(), conversationId,localToken);
                sendMessageToBot(result.getText(), conversationId2,localToken2);
            }
            else {
                //txt.setText("Error recognizing. Did you update the subscription info?" + System.lineSeparator() + result.toString());
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
                    String msgFrom = jsonObject.getJSONArray("activities").getJSONObject(arrayLength-1).getJSONObject("from").get("id").toString();
                    String curMsgId = jsonObject.getJSONArray("activities").getJSONObject(arrayLength-1).get("id").toString();
                    TextView resTxt = (TextView) this.findViewById(R.id.resTextView); //

                    if(msgFrom.trim().toLowerCase().equals(botName)) {
                        if(lastResponseMsgId == "") {
                            responseMsg = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).get("text").toString();
                            AddResponseToChat(responseMsg);

                            if( apl_status == 1 ) {
                                resTxt.setText(responseMsg);
                                apl_status = 1;
                            }
                            //lastResponseMsgId = curMsgId;
                            lastResponseMsgId = "";
                        }
                        else if(!lastResponseMsgId.equals(curMsgId))
                        {
                            responseMsg = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).get("text").toString();
                            AddResponseToChat(responseMsg);
                            lastResponseMsgId = curMsgId;
                        }
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
                        if(lastResponseMsgId2 == "") {
                            responseMsg = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).get("text").toString();
                            AddResponseToChat(responseMsg);

                            if( apl_status == 1 ) {
                                resTxt.setText(responseMsg);
                                apl_status = 1;
                            }
                            //lastResponseMsgId2 = curMsgId;
                            lastResponseMsgId2 = "";
                        }
                        else if(!lastResponseMsgId2.equals(curMsgId))
                        {
                            responseMsg = jsonObject.getJSONArray("activities").getJSONObject(arrayLength - 1).get("text").toString();
                            AddResponseToChat(responseMsg);
                            lastResponseMsgId2 = curMsgId;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        handler.postDelayed(runnable, 1000*5);
    }

    private void AddResponseToChat(String botResponse)
    {
        ChatMessage message = new ChatMessage();
        //message.setId(2);
        message.setMe(false);
        message.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        message.setMessage(botResponse);
        displayMessage(message);
    }

    public void displayMessage(ChatMessage message) {
   //     adapter.add(message);
   //     adapter.notifyDataSetChanged();
    //    scroll();
    }



}

