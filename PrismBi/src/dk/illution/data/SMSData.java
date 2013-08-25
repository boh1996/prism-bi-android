package dk.illution.data;

import java.util.List;
import dk.illution.data.MediaObject;
import dk.illution.data.PhoneData;

public class SMSData implements PhoneData {

	public String message;

    public String sender;

    public String thread_id;

    public String date;

    public String recipient;

    public String subject;

    public String type;

    public List<MediaObject> attachments;
	
}
