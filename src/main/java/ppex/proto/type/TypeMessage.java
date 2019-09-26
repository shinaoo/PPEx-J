package ppex.proto.type;

public class TypeMessage {

    public static enum Type {
        MSG_TYPE_PROBE,
        MSG_TYPE_TXT,
    }

    public TypeMessage() {
    }

    public TypeMessage(int type, String body) {
        this.type = type;
        this.body = body;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    private int type;
    private String body;


    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "TypeMessage{" +
                "type=" + type +
                ", body='" + body + '\'' +
                '}';
    }
}