package dk.illution;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

import android.widget.Button;
import android.widget.EditText;
import android.database.Cursor;
import android.net.Uri;
import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import java.io.*;
import android.util.Log;
import java.text.MessageFormat;

import dk.illution.data.SMSData;
import dk.illution.data.Data;
import dk.illution.data.Send;
import dk.illution.data.MediaObject;

public class MainActivity extends Activity {

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
        } catch (IOException e) {}
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return sb.toString();
    }

    private String getAddressNumber(String id) {
        String selectionAdd = new String("msg_id=" + id);
        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        Cursor cAdd = getContentResolver().query(uriAddress, null,
                selectionAdd, null, null);
        String name = null;
        if (cAdd.moveToFirst()) {
            do {
                String number = cAdd.getString(cAdd.getColumnIndex("address"));
                if (number != null) {
                    try {
                        Long.parseLong(number.replace("-", ""));
                        name = number;
                    } catch (NumberFormatException nfe) {
                        if (name == null) {
                            name = number;
                        }
                    }
                }
            } while (cAdd.moveToNext());
        }
        if (cAdd != null) {
            cAdd.close();
        }
        return name;
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

        return media;
    }

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
                    if ( type == "128" ) {
                        mms.sender = ownersPhone.getText().toString().replace("+45","").replace(" ", "").replace("-", "");
                        mms.recipient = getAddressNumber(id).replace("+45","").replace(" ", "").replace("-", "");;
                    } else {
                        mms.recipient = ownersPhone.getText().toString().replace("+45","").replace(" ", "").replace("-", "");
                        mms.sender = getAddressNumber(id).replace("+45","").replace(" ", "").replace("-", "");;
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

    protected void sendData () {
        List<SMSData> messageList = new ArrayList<SMSData>();
        messageList.addAll(fetchSMS());
        messageList.addAll(fetchMMS());

        Log.d("PRISM", "SENDING");
        new Send().execute(new Gson().toJson(new Data(messageList)), "sms");
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
