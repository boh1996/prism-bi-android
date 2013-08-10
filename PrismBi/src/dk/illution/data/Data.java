package dk.illution.data;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpHost;
import java.io.InputStreamReader;
import org.apache.http.message.BasicNameValuePair;
import java.util.ArrayList;
import java.io.IOException;
import java.io.*;
import org.apache.http.entity.StringEntity;

/**
 * Created by Bo on 09-08-13.
 */
public class Data {
    public List<SMSData> data;

    public Data (List<SMSData> data) {
        this.data = data;
    }
}