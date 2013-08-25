package dk.illution.data;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class Send extends AsyncTask<String, Void, String> {
    protected String doInBackground (String... params ) {
        StringEntity entity = null;
        try {
            entity = new StringEntity(params[0], "UTF-8");
        } catch ( Exception e ) {
            Log.d("PRISM", "ENCODE");
        }

        entity.setContentType("application/json");
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://192.168.1.101/post.php");//+params[1]

        try {
            // Add your data
            httppost.setEntity(entity);

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);

        } catch (Exception e) {
            Log.e("PRISM", "POST", e);
        }

        return "YES";
    }
}