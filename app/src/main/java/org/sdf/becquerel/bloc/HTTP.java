package org.sdf.becquerel.bloc;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import android.webkit.CookieManager;
//import java.net.CookieHandler;
//import java.net.CookieManager;
//import java.net.CookieStore;
//import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

/**
 * Created by stephen on 19/12/15.
 */
public class HTTP {

    public void post(String url, String params, DataDownloadListener listener) {
        AsyncPost client = new AsyncPost();
        client.setDataDownloadListener(listener);
        client.execute(url, params);
    }

    public void post(String url, HashMap<String, String> params, DataDownloadListener listener) {
        post(url, buildPOST(params), listener);
    }

    public void get(String url, DataDownloadListener listener) {
        AsyncGet client = new AsyncGet();
        client.setDataDownloadListener(listener);
        client.execute(url);
    }

    //converts hashmap of key=>value into a POST request ("key1=value1&key2=value2...")
    private static String buildPOST(HashMap<String, String> params) {
        String out = "";
        for (HashMap.Entry<String, String> entry : params.entrySet())
        {
            out += entry.getKey() + "=" + entry.getValue() + "&";
        }
        return out;
    }

    //Android won't allow running HTTP requests on the main thread.
    //This interface allows us to do a request, then run code on the result when it finishes
    public static interface DataDownloadListener {
        void dataDownloadedSuccessfully(Object data);
        void dataDownloadFailed();
    }

    private class AsyncGet extends android.os.AsyncTask<String, Void, String>{
        private DataDownloadListener dataDownloadListener;

        public void setDataDownloadListener(DataDownloadListener dataDownloadListener) {
            this.dataDownloadListener = dataDownloadListener;
        }

        @Override
        protected String doInBackground(String... args) {
            HttpURLConnection urlConn = null;
            String response = null;
            InputStream is = null;
            CookieManager cookieManager = CookieManager.getInstance();
            String cookie = cookieManager.getCookie("blocgame.com");

//            Log.d("dummy", "cookies:");
//            if(cookie != null)
//                Log.d("dummy", cookie);
            try {
                URL _url = new URL(args[0]);
                urlConn = (HttpURLConnection) _url.openConnection();
                urlConn.setRequestMethod("GET");
                if (cookie != null) {
                    urlConn.setRequestProperty("Cookie", cookie);
                }
                urlConn.connect();

                if(urlConn.getResponseCode() == HttpURLConnection.HTTP_OK){
                    // Get cookies from response and save into the cookie manager
                    List<String> cookieList = urlConn.getHeaderFields().get("Set-Cookie");
                    if (cookieList != null) {
                        for (String cookieTemp : cookieList) {
                            cookieManager.setCookie("blocgame.com", cookieTemp);
                        }
                    }
                    is = urlConn.getInputStream();
                } else {
                    is = urlConn.getErrorStream();
                }
            } catch (java.net.MalformedURLException e) {
                e.printStackTrace();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } //finally {
             //   if(urlConn != null)
             //       urlConn.disconnect();
           // }

            try {
                BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                response = sb.toString();
            } catch (Exception e) {
                android.util.Log.e("Buffer Error", "Error converting result " + e.toString());
            }
            return response;
        }

        protected void onPostExecute(String response) {
            android.util.Log.d("dummy", response);
            if (response != null)
            {
                dataDownloadListener.dataDownloadedSuccessfully(response);
            }
            else
                dataDownloadListener.dataDownloadFailed();
        }
    }

    private class AsyncPost extends android.os.AsyncTask<String, Void, String>{
        private DataDownloadListener dataDownloadListener;

        public void setDataDownloadListener(DataDownloadListener dataDownloadListener) {
            this.dataDownloadListener = dataDownloadListener;
        }

        @Override
        protected String doInBackground(String... args) {
            String response = null;
            java.io.InputStream is = null;
            HttpURLConnection urlConn = null;
            //CookieStore cStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
            //List<HttpCookie> cookies = cStore.getCookies();

            //for (HttpCookie cookie : cookies) {
            //    Log.d("dummy", cookie.toString());
            //}
            CookieManager cookieManager = CookieManager.getInstance();
            String cookie = cookieManager.getCookie("blocgame.com");
            Log.d("dummy", "cookies:");
            if(cookie != null)
                Log.d("dummy", cookie);

            try {
                URL _url = new URL(args[0]);
                urlConn =(HttpURLConnection) _url.openConnection();
                urlConn.setRequestMethod("POST");
                urlConn.setDoOutput(true);
                if (cookie != null) {
                    urlConn.setRequestProperty("Cookie", cookie);
                }
                urlConn.connect();
                BufferedWriter writer = new BufferedWriter(new java.io.OutputStreamWriter(urlConn.getOutputStream()));
                writer.write(args[1]);
                writer.flush();
                writer.close();
                if(urlConn.getResponseCode() == HttpURLConnection.HTTP_OK){
                    // Get cookies from responses and save into the cookie manager
                    List<String> cookieList = urlConn.getHeaderFields().get("Set-Cookie");
                    if (cookieList != null) {
                        for (String cookieTemp : cookieList) {
                            cookieManager.setCookie("blocgame.com", cookieTemp);
                        }
                    }
                    is = urlConn.getInputStream();
                } else {
                    is = urlConn.getErrorStream();
                }

            } catch (java.net.MalformedURLException e) {
                e.printStackTrace();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

            try {
                BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                response = sb.toString();
            } catch (Exception e) {
                android.util.Log.e("Buffer Error", "Error converting result " + e.toString());
            }
            return response;
        }

        protected void onPostExecute(String response) {
            android.util.Log.d("dummy", response);
            if (response != null)
            {
                dataDownloadListener.dataDownloadedSuccessfully(response);
            }
            else
                dataDownloadListener.dataDownloadFailed();
        }
    }
}
