package Utils;

import java.util.Map;
import java.util.TreeMap;

public class MessageParser {
     public Map<Integer, Message> messageMap;
     {
        messageMap = new TreeMap<>();
     }

    public byte[] parse(int i) {
        byte[] bb = new byte[getLen(i)];
        Message m = messageMap.get(i);
        byte[] body = m.getBody();
        int iter = 0;
        for (; iter < Math.min(body.length,bb.length); iter++) {
            bb[iter] = body[iter];
        }
        if (m.getNext() != -1) {
            int index = iter;
            body = parse(m.getNext());
            for (; iter < bb.length; iter++) {
                bb[iter] = body[iter-index];
            }
        }
        messageMap.remove(i);
        return bb;
    }

    int getLen(int i) {
        if(!messageMap.containsKey(i))
            return 0;
        Message m = messageMap.get(i);
        return m.getLen()+getLen(m.getNext());
    }

    public boolean isFullGot(int i){
         return messageMap.containsKey(i) & isFullGot(messageMap.get(i).getNext());
    }
}
