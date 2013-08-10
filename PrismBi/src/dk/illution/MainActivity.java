package dk.illution;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

import android.widget.Button;
import android.widget.EditText;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import dk.illution.data.SMSData;
import dk.illution.data.Data;

class Send extends AsyncTask<String, Void, String> {
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
        HttpPost httppost = new HttpPost("http://192.168.1.104:8080/sms");

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

public class MainActivity extends Activity {

    protected void sendData () {
        List<SMSData> smsList = new ArrayList<SMSData>();

        Uri uri = Uri.parse("content://sms");
        Cursor cursor = getContentResolver().query(uri, null, null ,null,null);
        //startManagingCursor(cursor);

        EditText ownersPhone   = (EditText)findViewById(R.id.ownersPhone);

        // Read the sms data and store it in the list
        try {
            if(cursor.moveToFirst()) {
                for(int i=0; i < cursor.getCount(); i++) {
                    SMSData sms = new SMSData();
                    String type;
                    try {
                        type = cursor.getString(cursor.getColumnIndexOrThrow("type")).toString();
                    } catch (Exception e){
                        type = "";
                    }
                    sms.type = type;
                    try {
                        sms.message = cursor.getString(cursor.getColumnIndexOrThrow("body")).toString();
                    } catch (Exception e){
                        sms.message = "";
                    }
                    try {
                        sms.thread_id = cursor.getString(cursor.getColumnIndexOrThrow("thread_id")).toString();
                    } catch (Exception e) {
                        sms.thread_id = "";
                    }
                    if ( type.equals("1") ) {
                        try {
                            sms.sender = cursor.getString(cursor.getColumnIndexOrThrow("address")).toString().replace("+45","").replace(" ", "");
                        } catch (Exception e) {
                            sms.sender = "";
                        }
                        sms.recipient = ownersPhone.getText().toString();
                    } else {
                        try {
                            sms.recipient = cursor.getString(cursor.getColumnIndexOrThrow("address")).toString().replace("+45","").replace(" ", "");
                        } catch (Exception e) {
                            sms.recipient = "";
                        }
                        sms.sender = ownersPhone.getText().toString();
                    }
                    try {
                        sms.date = cursor.getString(cursor.getColumnIndexOrThrow("date")).toString();
                    } catch (Exception e) {
                        sms.date = "";
                    }
                    try {
                        sms.subject = cursor.getString(cursor.getColumnIndexOrThrow("subject")).toString();
                    } catch (Exception e) {
                        sms.subject = "";
                    }
                    smsList.add(sms);

                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {}
        cursor.close();

        new Send().execute(new Gson().toJson(new Data(smsList)));
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
        } catch ( Exception e ) {
            Log.e("PRISM", "CONTENT-VIEW", e);
        }

        try {
            Button button= (Button) findViewById(R.id.sendButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendData();
                }
            });
        } catch ( Exception e ) {
        }
	}
	
}
