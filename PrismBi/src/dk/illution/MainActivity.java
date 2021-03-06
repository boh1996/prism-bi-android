package dk.illution;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.database.Cursor;
import android.net.Uri;
import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import java.io.*;
import java.util.TreeSet;
import java.util.Date;
import java.util.Set;
import java.util.SortedSet;

import android.provider.CallLog;
import android.util.Log;

import dk.illution.data.PhoneData;
import dk.illution.data.SMSData;
import dk.illution.data.Data;
import dk.illution.data.Send;
import dk.illution.data.Call;
import dk.illution.data.MediaObject;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "PrismBI";

    private String getMmsText(String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException e) {
            Log.e("PRISM", "STRING-READER", e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e("PRISM", "STRING-READER", e);
                }
            }
        }
        return sb.toString();
    }

    private String getAddressNumber(String id) {
        Cursor mCursor = getContentResolver().query(Uri.parse("content://mms"), null, null, null, null);

        String messageAddress = "";
        while (mCursor.moveToNext()) {
            String messageId = mCursor.getString(mCursor.getColumnIndex("_id"));

            if ( messageId.equals(id) ) {
                Uri.Builder builder = Uri.parse("content://mms").buildUpon();
                builder.appendPath(messageId).appendPath("addr");
                Cursor c = getContentResolver().query(builder.build(), new String[] {
                        "*"
                }, null, null, null);
                while (c.moveToNext()) {
                    messageAddress = c.getString(c.getColumnIndex("address"));

                    if (!messageAddress.equals("insert-address-token")) {
                        c.moveToLast();
                    }
                }
                c.close();
            }
        }

        return messageAddress;
    }

    public List<SMSData> fetchSMS () {
        List<SMSData> smsList = new ArrayList<SMSData>();
        EditText ownersPhone   = (EditText)findViewById(R.id.ownersPhone);

        Uri uri = Uri.parse("content://sms");

        Cursor cursor = getContentResolver().query(uri, null, null ,null,null);

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
                            sms.sender = cursor.getString(cursor.getColumnIndexOrThrow("address")).toString().replace("+45","").replace(" ", "").replace("-", "");
                        } catch (Exception e) {
                            sms.sender = "";
                        }
                        sms.recipient = ownersPhone.getText().toString().replace("+45","").replace(" ", "").replace("-", "");
                    } else {
                        try {
                            sms.recipient = cursor.getString(cursor.getColumnIndexOrThrow("address")).toString().replace("+45","").replace(" ", "").replace("-", "");
                        } catch (Exception e) {
                            sms.recipient = "";
                        }
                        sms.sender = ownersPhone.getText().toString().replace("+45","").replace(" ", "").replace("-", "");
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

        return smsList;
    }

    private static final int RAW_DATA_BLOCK_SIZE = 16384; //Set the block size used to write a ByteArrayOutputStream to byte[]
    public static final int ERROR_IO_EXCEPTION = 1;
    public static final int ERROR_FILE_NOT_FOUND = 2;

    public static byte[] LoadRaw(InputStream inputStream, int Error){
        byte[] ret = new byte[0];

        //Open inputStream from the specified URI
        try {

            //Try read from the InputStream
            if(inputStream!=null)
                ret = InputStreamToByteArray(inputStream);

        }
        catch (FileNotFoundException e1) {
            Error = ERROR_FILE_NOT_FOUND;
        }
        catch (IOException e) {
            Error = ERROR_IO_EXCEPTION;
        }
        finally{
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    //Problem on closing stream.
                    //The return state does not change.
                    Error = ERROR_IO_EXCEPTION;
                }
            }
        }


        //Return
        return ret;
    }


    //Create a byte array from an open inputStream. Read blocks of RAW_DATA_BLOCK_SIZE byte
    private static byte[] InputStreamToByteArray(InputStream inputStream) throws IOException{
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[RAW_DATA_BLOCK_SIZE];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Fetches a List<MediaObject> of the available media components in an MMS
     * @param id The message id
     * @return List<MediaObject>
     */
    private List<MediaObject> getMMSMediaParts ( String id ) {
        String selectionPart = "mid=" + id;
        Uri uri = Uri.parse("content://mms/part");
        Cursor cPart = getContentResolver().query(uri, null,
                selectionPart, null, null);
        List<MediaObject> media = new ArrayList<MediaObject>();
        if (cPart.moveToFirst()) {
            do {
                String partId = cPart.getString(cPart.getColumnIndex("_id"));
                Uri partURI = Uri.parse("content://mms/part/" + partId);
                String type = cPart.getString(cPart.getColumnIndex("ct"));
                if ( ! "text/plain".equals(type) && ! "application/smil".equals(type) ) {
                    InputStream is = null;
                    try {
                        is = getContentResolver().openInputStream(partURI);
                        int error = 0;
                        MediaObject mediaObject = new MediaObject();
                        mediaObject.attachment = Base64.encodeToString(LoadRaw(is, error), Base64.DEFAULT);
                        mediaObject.type = type;
                        media.add(mediaObject);
                    } catch ( Exception e ) {

                    }
                }
            } while (cPart.moveToNext());
        }

        cPart.close();

        return media;
    }

    /**
     * Fetches the text part of a MMS
     * @param id The message id
     * @return String
     */
    private String getMMSTextParts ( String id ) {
        String selectionPart = "mid=" + id;
        Uri uri = Uri.parse("content://mms/part");
        String body = "";
        Cursor cursor = getContentResolver().query(uri, null,
                selectionPart, null, null);
        if (cursor.moveToFirst()) {
            do {
                String partId = cursor.getString(cursor.getColumnIndex("_id"));
                String type = cursor.getString(cursor.getColumnIndex("ct"));
                if ("text/plain".equals(type)) {
                    String data = cursor.getString(cursor.getColumnIndex("_data"));
                    if (data != null) {
                        // implementation of this method below
                        body += getMmsText(partId);
                    } else {
                        body += cursor.getString(cursor.getColumnIndex("text"));
                    }
                }
            } while (cursor.moveToNext());
        }

        return body;
    }

    /**
     * Fetches a List<SMSData> of the MMS messages on the phone
     * @return List<SMSData>
     */
    public List<SMSData> fetchMMS () {
        List<SMSData> mmsList = new ArrayList<SMSData>();

        EditText ownersPhone   = (EditText)findViewById(R.id.ownersPhone);

        Uri uri = Uri.parse("content://mms");

        Cursor cursor = getContentResolver().query(uri, null, null ,null,null);

        try {
            if(cursor.moveToFirst()) {
                for(int i=0; i < cursor.getCount(); i++) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow("_id")).toString();
                    SMSData mms = new SMSData();

                    String type = cursor.getString(cursor.getColumnIndex("m_type"));
                    mms.type = type;
                    if ( type.equals("128") ) {
                        mms.sender = ownersPhone.getText().toString().replace("+45","").replace(" ", "").replace("-", "");
                        mms.recipient = getAddressNumber(id).replace("+45","").replace(" ", "").replace("-", "");
                    } else {
                        mms.recipient = ownersPhone.getText().toString().replace("+45","").replace(" ", "").replace("-", "");
                        mms.sender = getAddressNumber(id).replace("+45","").replace(" ", "").replace("-", "");
                    }

                    try {
                        String message = getMMSTextParts(id);
                        if ( ! message.equals("Message not found") ) {
                            mms.message = message;
                        } else {
                            mms.message = "";
                        }
                    } catch ( Exception e ) {
                        mms.message = "";
                    }
                    try {
                        mms.thread_id = cursor.getString(cursor.getColumnIndexOrThrow("thread_id")).toString();
                    } catch (Exception e) {
                        mms.thread_id = "";
                    }
                    try {
                        mms.date = cursor.getString(cursor.getColumnIndexOrThrow("date")).toString();
                    } catch (Exception e) {
                        mms.date = "";
                    }
                    try {
                        mms.subject = cursor.getString(cursor.getColumnIndexOrThrow("subject")).toString();
                    } catch (Exception e) {
                        mms.subject = "";
                    }
                    try {
                        mms.attachments = getMMSMediaParts(id);
                    } catch ( Exception e ) {

                    }
                    mmsList.add(mms);

                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {}
        cursor.close();

        return mmsList;
    }

    private List<Call> getCallDetails() {
        EditText ownersPhone   = (EditText)findViewById(R.id.ownersPhone);

        List<Call> calls = new ArrayList<Call>();

        Cursor managedCursor = null;

        try {
            managedCursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null,
                null, null, null);
        } catch ( Exception e ) {
            Log.e("PRISM", "CURSOR", e);
        }

        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

        while (managedCursor.moveToNext()) {
            Call call = new Call();

            try {
                call.date = managedCursor.getString(date).toString();
            } catch ( Exception e ) {}

            try {
                call.duration = managedCursor.getString(duration).toString();
            } catch ( Exception e ) {}

            try{
                call.number_type = managedCursor.getString(managedCursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE)).toString();
            }   catch ( Exception e ) {}

            try {
                call.number_label = managedCursor.getString(managedCursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_LABEL)).toString();
            } catch ( Exception e ) {}

            String direction = null;
            int dirCode = Integer.parseInt(managedCursor.getString(type));
            switch (dirCode) {
                case CallLog.Calls.OUTGOING_TYPE:
                    direction = "OUTGOING";
                    call.caller = ownersPhone.getText().toString().replace("+45","").replace(" ", "").replace("-", "");
                    call.reciever = managedCursor.getString(number).toString().replace("+45","").replace(" ", "").replace("-", "");
                    break;

                case CallLog.Calls.INCOMING_TYPE:
                    direction = "INCOMING";
                    call.caller = managedCursor.getString(number).toString().replace("+45","").replace(" ", "").replace("-", "");
                    call.reciever = ownersPhone.getText().toString().replace("+45","").replace(" ", "").replace("-", "");
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    direction = "MISSED";
                    call.caller = managedCursor.getString(number).toString().replace("+45","").replace(" ", "").replace("-", "");
                    call.reciever = ownersPhone.getText().toString().replace("+45","").replace(" ", "").replace("-", "");
                    break;
            }
            call.direction = direction;
            calls.add(call);
        }
        managedCursor.close();
        return calls;

    }

    protected void sendData (String apiUrl) {
        List<PhoneData> messageList = new ArrayList<PhoneData>();
        messageList.addAll(fetchSMS());
        messageList.addAll(fetchMMS());

        Send smsSend = new Send();
        smsSend.host = apiUrl;

        smsSend.execute(new Gson().toJson(new Data(messageList)), "sms");

        List<PhoneData> calls = new ArrayList<PhoneData>();
        calls.addAll(getCallDetails());

        Send callsSend = new Send();
        callsSend.host = apiUrl;

        callsSend.execute(new Gson().toJson(new Data(calls)), "calls");
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
        } catch ( Exception e ) {
            Log.e("PRISM", "CONTENT-VIEW", e);
        }

        EditText apiUrl   = (EditText)findViewById(R.id.apiUrl);
        EditText ownersPhone   = (EditText)findViewById(R.id.ownersPhone);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String settingsApiUrl = settings.getString("apiUrl", "https://illution.dk/prismbi");
        String settingsOwnerPhone = settings.getString("ownersPhone", "");
        apiUrl.setText(settingsApiUrl);
        ownersPhone.setText(settingsOwnerPhone);

        try {
            Button button= (Button) findViewById(R.id.sendButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText apiUrl   = (EditText)findViewById(R.id.apiUrl);
                    EditText ownersPhone   = (EditText)findViewById(R.id.ownersPhone);
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("apiUrl", apiUrl.getText().toString());
                    editor.putString("ownersPhone", ownersPhone.getText().toString());
                    editor.commit();
                    sendData(apiUrl.getText().toString());
                }
            });
        } catch ( Exception e ) {
        }
	}
	
}
