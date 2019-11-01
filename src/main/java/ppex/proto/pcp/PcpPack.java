package ppex.proto.pcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import org.apache.log4j.Logger;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpscArrayQueue;
import ppex.proto.msg.Message;
import ppex.proto.msg.entity.Connection;
import ppex.utils.MessageUtil;
import ppex.utils.tpool.IMessageExecutor;

import java.util.Queue;

public class PcpPack {
    private static Logger LOGGER = Logger.getLogger(PcpPack.class);

    private final Pcp pcp;
    private PcpListener pcpListener;
    private IMessageExecutor iMessageExecutor;

    private final Queue<ByteBuf> rcvList;
    private final MpscArrayQueue<ByteBuf> sndList;

    private long tsUpdate = -1;

    private Connection connection;

    public PcpPack(int conv, PcpListener pcpListener, IMessageExecutor iMessageExecutor, Connection connection,PcpOutput pcpOutput) {
        this.pcp = new Pcp(conv,pcpOutput,connection);
        sndList = new MpscArrayQueue<>(2 << 11);
        rcvList = new SpscArrayQueue<>(2 << 11);
        this.iMessageExecutor = iMessageExecutor;
        this.pcpListener = pcpListener;
    }

    public boolean write(Message msg){
        return write(MessageUtil.msg2ByteBuf(msg));
    }

    public boolean write(ByteBuf byteBuf) {
        LOGGER.info("PcpPack write:" + byteBuf.readableBytes());
        byteBuf = byteBuf.retainedDuplicate();
        if (!sndList.offer(byteBuf)) {
            LOGGER.error("sndList full");
            byteBuf.release();
            return false;
        }
        notifyWriteEvent();
        return true;
    }

    public void send(ByteBuf byteBuf) throws Exception {
        int ret = pcp.send(byteBuf);
        if (ret == -2){
            throw new Exception("too many");
        }
    }

    public long flush(long current){
        return pcp.flush(false,current);
    }

    protected boolean canSend(boolean curCanSend) {
        int max = pcp.getSnd_wnd() * 2;
        int waitSnd = pcp.waitSnd();
        if (curCanSend) {       //等待ack的数量比窗口的小
            return waitSnd < max;
        } else {                //等待ack的数量窗口的小
            int hold = Math.max(1, max >> 1);
            return waitSnd < hold;
        }
    }

    public void read(ByteBuf buf){
        LOGGER.info("PcpPack read:" + buf.readableBytes());
        this.rcvList.add(buf.readRetainedSlice(buf.readableBytes()));
        buf.release();
        notifyReadEvent();
    }
    public void read(DatagramPacket pkt){
        read(pkt.content());
    }

    public void input(ByteBuf data,long current) throws Exception{
        input(data,true,current);
    }
    private void input(ByteBuf data,boolean regular,long current){
        int ret = pcp.input(data,regular,current);
    }

    protected void notifyWriteEvent() {
        LOGGER.info("PcpPack notifyWriteEvent");
        SendTask sndTask = SendTask.New(this);
        this.iMessageExecutor.execute(sndTask);
    }

    protected void notifyReadEvent() {
        LOGGER.info("PcpPack notifyReadEvent");
        RecvTask recvTask = RecvTask.New(this);
        this.iMessageExecutor.execute(recvTask);
    }

    public MpscArrayQueue<ByteBuf> getSndList() {
        return sndList;
    }

    public Queue<ByteBuf> getRcvList() {
        return rcvList;
    }

    public PcpPack setTsUpdate(long tsUpdate) {
        this.tsUpdate = tsUpdate;
        return this;
    }

    public Connection getConnection() {
        return connection;
    }
    public int getInterval(){
        return pcp.getInterval();
    }

    public PcpListener getPcpListener() {
        return pcpListener;
    }

    public boolean canRecv(){
        return pcp.canRecv();
    }
    public ByteBuf mergeReceive(){
        return pcp.mergeRecv();
    }
    public boolean checkFlush(){
        return pcp.checkFlush();
    }
    public boolean isFastFlush(){
        return pcp.isFastFlush();
    }
}
