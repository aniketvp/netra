package com.aniketpednekar.netra;

import android.content.Context;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aniketpednekar on 3/6/16.
 */
public class RetrieveCaption extends AsyncTask<byte[], Void, String> {

    private Exception exception;
    private Context context;
    private TextView captionTextView;
    private TextToSpeech tts;

    public RetrieveCaption(Context appcontext, TextView captionTv, TextToSpeech t1){
        context = appcontext;
        captionTextView = captionTv;
        tts = t1;
    }

    protected String doInBackground(byte[]... params) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://api.imgur.com/3/image");
            httpPost.setHeader("Authorization", "Client-ID 3f0030c1273b4e7");
            System.out.println("Length of the byte array sent" + params[0].length);
            httpPost.setEntity(new ByteArrayEntity(params[0]));
            HttpResponse response = httpClient.execute(httpPost);

            String imgur = EntityUtils.toString(response.getEntity());
            JSONObject reader = new JSONObject(imgur);
            JSONObject data = reader.getJSONObject("data");
            String link = data.getString("link");
            Log.d("IMGUR LINK", link);
            HttpClient httpClient2 = new DefaultHttpClient();
            HttpPost httpPost2 = new HttpPost("https://stage-api.algomus.com/v1.0/caption/");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("image_url", link));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs,"UTF-8");
            httpPost2.setEntity(entity);
            HttpResponse response2 = httpClient2.execute(httpPost2);
            String algomus = EntityUtils.toString(response2.getEntity());
            JSONObject reader2 = new JSONObject(algomus);
            String data2  = reader2.getString("caption");

            Log.d("ALGOMUS RESP", data2);
            return data2;

        } catch (Exception e) {
            this.exception = e;
            Log.d("EXCEPTION", e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(String feed){
        Log.d("feed", feed);
        Log.d("TAG", "IN PostExecute");
        captionTextView.setText(feed);
        Log.d("TAG", "IN PostExecute2");
        captionTextView.setVisibility(View.VISIBLE);
        Log.d("TAG", "IN PostExecute3");
        tts.speak(feed, TextToSpeech.QUEUE_FLUSH, null);
        /*try{
            JSONObject reader = new JSONObject(feed);
            String data  = reader.getString("caption");
            Toast.makeText(context, data, Toast.LENGTH_LONG)
                    .show();
        }
        catch(org.json.JSONException e){

        }*/
    }
}