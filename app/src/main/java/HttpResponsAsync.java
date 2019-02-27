package com.microsoft.cognitiveservices.speech.samples.quickstart;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HttpResponsAsync extends AsyncTask<Void, Void, String> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // doInBackground前処理
    }

    @Override
    protected String doInBackground(Void... params) {

        HttpURLConnection con = null;
        URL url = null;
        String urlSt = "http://www.google.co.jp";

        try {
            // URLの作成
            url = new URL(urlSt);
            // 接続用HttpURLConnectionオブジェクト作成
            con = (HttpURLConnection)url.openConnection();
            // リクエストメソッドの設定
            con.setRequestMethod("POST");
            // リダイレクトを自動で許可しない設定
            con.setInstanceFollowRedirects(false);
            // URL接続からデータを読み取る場合はtrue
            con.setDoInput(true);
            // URL接続にデータを書き込む場合はtrue
            con.setDoOutput(true);

            // 接続
            con.connect(); // ①
            // HTTPレスポンスコード
            final int status = con.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {


                // 通信に成功した
                // テキストを取得する
                StringBuilder result = new StringBuilder();
                InputStream in = con.getInputStream();
                String encoding = con.getContentEncoding();
                InputStreamReader inReader = new InputStreamReader(in, encoding);
                BufferedReader bufReader = new BufferedReader(inReader);
                String line = null;
                // 1行ずつテキストを読み込む
                //while((line = bufReader.readLine()) != null) {
                //   result.append(line);
                //}

                // 本文の取得
                //InputStream in = con.getInputStream();
                //byte bodyByte[] = new byte[1024];
                //in.read(bodyByte);
                in.close();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        // doInBackground後処理
    }

}