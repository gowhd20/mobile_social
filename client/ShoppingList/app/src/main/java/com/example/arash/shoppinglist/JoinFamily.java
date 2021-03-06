package com.example.arash.shoppinglist;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Arash on 4/28/2016.
 */
public class JoinFamily extends AsyncTask<Void, Void, Void> {

    Runnable mR;
    ProgressDialog dialog;
    String res_str = "";
    final String url, familyName, macAddress, userName, googleToken, familyID;
    Context context;
    DatabaseHelper database;

    public JoinFamily(Context CONTEXT, final String URL,final String FamilyName ,final String FamilyID, final String MacAddress, final String UserName, final String GoogleToken) {
        context = CONTEXT;
        dialog = new ProgressDialog(context);
        url = URL;
        familyName = FamilyName;
        familyID = FamilyID;
        macAddress = MacAddress;
        userName = UserName;
        googleToken = GoogleToken;
    }

    @Override
    protected void onPreExecute() {
        dialog.setMessage("Processing...");
        dialog.show();
    }

    @Override
    protected void onPostExecute(Void result) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        if (res_str.equals(""))
            Toast.makeText(context, "Server not responding", Toast.LENGTH_SHORT).show();
        else{
            FamilyGroupActivity.JoinStatus_static = true;
            FamilyGroupActivity.familyName_static = familyName;
            getUserNameAndID();
            //update family members local database
            ((FamilyGroupActivity)context).updateFamilyMembersLocalDatabase();
            //update shopping cart items local database
            GetShoppingCart getShoppingCart = new GetShoppingCart(context, FamilyGroupActivity.familyID_static);
            getShoppingCart.execute();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        Thread networkChecking = new Thread() {
            @Override
            public void run() {
                mR = new Runnable() {
                    @Override
                    public void run() {
                        HttpCallback cb = new HttpCallback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.d("main", "failed");
                            }

                            @Override
                            public void onSuccess(Response response) {
                                Log.d("main", "succeed");
                                try {
                                    res_str = response.body().string();
                                    Log.d("main", res_str);
                                }catch (IOException e){
                                    e.printStackTrace();
                                }

                            }
                        };
                        JsonObject obj = new JsonObject();
                        obj.addProperty("uuid", familyID);
                        obj.addProperty("mac_addr", macAddress);
                        obj.addProperty("google_token", googleToken);
                        obj.addProperty("user_name", userName);


                        try {
                            postRequest(url, obj, cb);

                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                };
                mR.run();
            }
        };
        networkChecking.start();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }


    public interface HttpCallback{
        void onFailure(Call call, IOException e);

        void onSuccess(Response response);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public void postRequest(String url, Object json, final HttpCallback cb) throws IOException {
        //Log.i(TAG, "what do i post to server? : " + json.toString());

        RequestBody requestBody = RequestBody.create(JSON, new Gson().toJson(json));
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("token", "test")
                .build();
        //request = addBasicAuthHeaders(request);       // add pwd and id

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //Log.i(TAG, Boolean.toString(httpService.NETWORK_AVAILABLE));
                cb.onFailure(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                cb.onSuccess(response);

            }
        });
    }

    public void getUserNameAndID(){
        Log.d("main", res_str);
        JSONObject obj, dataObj;

        try{
            obj = new JSONObject(res_str);
            dataObj = obj.optJSONObject("data");
            FamilyGroupActivity.userID_static = dataObj.getString("uid");
            FamilyGroupActivity.familyID_static = dataObj.getString("uuid");
            Log.d("main", FamilyGroupActivity.userID_static);
            Log.d("main", FamilyGroupActivity.familyID_static);
        }catch(JSONException e){
            e.printStackTrace();
            Log.d("main", "test_failed");
        }
    }



}




