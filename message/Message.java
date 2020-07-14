package message;

import java.io.Serializable;
import java.util.Date;
import java.net.InetAddress;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String text, nickname, status;
    private final Date date;
    private final InetAddress sender;

    public Message(String _text, String _nickname, String _status, Date _date,
                                                            InetAddress _sender)
    {
        text = _text;
        nickname = _nickname;
        status = _status;
        date = _date;
        sender = _sender;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return nickname + " at " + date.toString() + ": " + text;
    }
}
