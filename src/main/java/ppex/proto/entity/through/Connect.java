package ppex.proto.entity.through;

/**
 * 因为是客户端和服务端共用.所以比较麻烦
 * 如果是Client端用,直接将CONNECT序列化放入ThroughTypeMsg即可
 * 如果是Server端用,需要将CONNECT序列化放入RECVINFO中,RECVINFO再放入ThroughTypeMsg.
 */
public class Connect {

    public enum TYPE{

        ;
    }

    private int type;
    //之前保留from和to.现在使用content保存connect包下的有关类的JSON字符串
    private String content;


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Connect{" +
                "type=" + type +
                ", content='" + content + '\'' +
                '}';
    }
}
