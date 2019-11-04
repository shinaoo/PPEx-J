package ppex.proto.pcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpscArrayQueue;
import ppex.proto.msg.entity.Connection;
import ppex.utils.tpool.IMessageExecutor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.zip.CRC32;

/**
 * Wrapper for kcp
 *
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class Ukcp {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(Ukcp.class);

    public static final int HEADER_CRC = 4, HEADER_NONCESIZE = 16;

    private final Kcp kcp;

    private boolean fastFlush = true;

    private long tsUpdate = -1;

    private boolean active;

    private final MpscArrayQueue<ByteBuf> sendList;

    private final Queue<ByteBuf> recieveList;

    private final IMessageExecutor iMessageExecutor;

    private final PcpListener kcpListener;

    private boolean crc32Check = false;

    /**
     * 上次收到消息时间
     **/
    private long lastRecieveTime = System.currentTimeMillis();

    /**
     * 超时关闭时间
     **/
    private long timeoutMillis = 0;

    private final CRC32 crc32 = new CRC32();


    /**
     * Creates a new instance.
     *
     * @param output output for kcp
     */
    public Ukcp(PcpOutput output, PcpListener kcpListener, IMessageExecutor iMessageExecutor, Connection connection) {
        this.kcp = new Kcp(1, output, connection);
        this.active = true;
        this.kcpListener = kcpListener;
        this.iMessageExecutor = iMessageExecutor;
        //默认2<<16   可以修改
        sendList = new MpscArrayQueue<>(2 << 11);
        recieveList = new SpscArrayQueue<>(2 << 11);
        //recieveList = new SpscLinkedQueue<>();
        int headerSize = 0;
        //init encryption

//        kcp.setReserved(headerSize);
//        intKcpConfig(channelConfig);
    }

//    private void intKcpConfig(ChannelConfig channelConfig){
//        kcp.nodelay(channelConfig.isNodelay(),channelConfig.getInterval(),channelConfig.getFastresend(),channelConfig.isNocwnd());
//        kcp.setSndWnd(channelConfig.getSndwnd());
//        kcp.setRcvWnd(channelConfig.getRcvwnd());
//        kcp.setMtu(channelConfig.getMtu());
//        kcp.setStream(channelConfig.isStream());
//        kcp.setAckNoDelay(channelConfig.isAckNoDelay());
//        kcp.setAutoSetConv(channelConfig.isAutoSetConv());
//        kcp.setAckMaskSize(channelConfig.getAckMaskSize());
//        this.fastFlush = channelConfig.isFastFlush();
//        this.timeoutMillis = channelConfig.getTimeoutMillis();
//    }


    /**
     * Receives ByteBufs.
     *
     * @param bufList received ByteBuf will be add to the list
     */
    protected void receive(List<ByteBuf> bufList) {
        kcp.recv(bufList);
    }


    public ByteBuf mergeReceive() {
        return kcp.mergeRecv();
    }


    public void input(ByteBuf data, long current) throws IOException {
        lastRecieveTime = System.currentTimeMillis();
        input(data, true, current);
    }

    private void input(ByteBuf data, boolean regular, long current) throws IOException {
        int ret = kcp.input(data, regular, current);
        switch (ret) {
            case -1:
                throw new IOException("No enough bytes of head");
            case -2:
                throw new IOException("No enough bytes of data");
            case -3:
                throw new IOException("Mismatch cmd");
            case -4:
                throw new IOException("Conv inconsistency");
            default:
                break;
        }
    }


    /**
     * Sends a Bytebuf.
     *
     * @param buf
     * @throws IOException
     */
    void send(ByteBuf buf) throws IOException {
        int ret = kcp.send(buf);
        switch (ret) {
            case -2:
                throw new IOException("Too many fragments");
            default:
                break;
        }
    }

    /**
     * The size of the first msg of the kcp.
     *
     * @return The size of the first msg of the kcp, or -1 if none of msg
     */
    public int peekSize() {
        return kcp.peekSize();
    }

    /**
     * Returns {@code true} if there are bytes can be received.
     *
     * @return
     */
    public boolean canRecv() {
        return kcp.canRecv();
    }


    public long getLastRecieveTime() {
        return lastRecieveTime;
    }


    /**
     * Returns {@code true} if the kcp can send more bytes.
     *
     * @param curCanSend last state of canSend
     * @return {@code true} if the kcp can send more bytes
     */
    protected boolean canSend(boolean curCanSend) {
        int max = kcp.getSndWnd() * 2;
        int waitSnd = kcp.waitSnd();
        if (curCanSend) {
            return waitSnd < max;
        } else {
            int threshold = Math.max(1, max / 2);
            return waitSnd < threshold;
        }
    }

    /**
     * Udpates the kcp.
     *
     * @param current current time in milliseconds
     * @return the next time to update
     */
    public long update(long current) {
        kcp.update(current);
        long nextTsUp = check(current);
        setTsUpdate(nextTsUp);

        return nextTsUp;
    }

    public long flush(long current) {
        return kcp.flush(false, current);
    }

    /**
     * Determines when should you invoke udpate.
     *
     * @param current current time in milliseconds
     * @return
     * @see Kcp#check(long)
     */
    public long check(long current) {
        return kcp.check(current);
    }

    /**
     * Returns {@code true} if the kcp need to flush.
     *
     * @return {@code true} if the kcp need to flush
     */
    public boolean checkFlush() {
        return kcp.checkFlush();
    }

    /**
     * Sets params of nodelay.
     *
     * @param nodelay  {@code true} if nodelay mode is enabled
     * @param interval protocol internal work interval, in milliseconds
     * @param resend   fast retransmission mode, 0 represents off by default, 2 can be set (2 ACK spans will result
     *                 in direct retransmission)
     * @param nc       {@code true} if turn off flow control
     */
    public void nodelay(boolean nodelay, int interval, int resend, boolean nc) {
        kcp.nodelay(nodelay, interval, resend, nc);
    }

    /**
     * Returns conv of kcp.
     *
     * @return conv of kcp
     */
    public int getConv() {
        return kcp.getConv();
    }

    /**
     * Set the conv of kcp.
     *
     * @param conv the conv of kcp
     */
    public void setConv(int conv) {
        kcp.setConv(conv);
    }

    /**
     * Returns {@code true} if and only if nodelay is enabled.
     *
     * @return {@code true} if and only if nodelay is enabled
     */
    public boolean isNodelay() {
        return kcp.isNodelay();
    }

    /**
     * Sets whether enable nodelay.
     *
     * @param nodelay {@code true} if enable nodelay
     * @return this object
     */
    public Ukcp setNodelay(boolean nodelay) {
        kcp.setNodelay(nodelay);
        return this;
    }

    /**
     * Returns update interval.
     *
     * @return update interval
     */
    public int getInterval() {
        return kcp.getInterval();
    }

    /**
     * Sets update interval
     *
     * @param interval update interval
     * @return this object
     */
    public Ukcp setInterval(int interval) {
        kcp.setInterval(interval);
        return this;
    }

    /**
     * Returns the fastresend of kcp.
     *
     * @return the fastresend of kcp
     */
    public int getFastResend() {
        return kcp.getFastresend();
    }

    /**
     * Sets the fastresend of kcp.
     *
     * @param fastResend
     * @return this object
     */
    public Ukcp setFastResend(int fastResend) {
        kcp.setFastresend(fastResend);
        return this;
    }

    public boolean isNocwnd() {
        return kcp.isNocwnd();
    }

    public Ukcp setNocwnd(boolean nocwnd) {
        kcp.setNocwnd(nocwnd);
        return this;
    }

    public int getMinRto() {
        return kcp.getRxMinrto();
    }

    public Ukcp setMinRto(int minRto) {
        kcp.setRxMinrto(minRto);
        return this;
    }

    public int getMtu() {
        return kcp.getMtu();
    }

    public Ukcp setMtu(int mtu) {
        kcp.setMtu(mtu);
        return this;
    }

    public boolean isStream() {
        return kcp.isStream();
    }

    public Ukcp setStream(boolean stream) {
        kcp.setStream(stream);
        return this;
    }

    public int getDeadLink() {
        return kcp.getDeadLink();
    }

    public Ukcp setDeadLink(int deadLink) {
        kcp.setDeadLink(deadLink);
        return this;
    }

    /**
     * Sets the {@link ByteBufAllocator} which is used for the kcp to allocate buffers.
     *
     * @param allocator the allocator is used for the kcp to allocate buffers
     * @return this object
     */
    public Ukcp setByteBufAllocator(ByteBufAllocator allocator) {
        kcp.setByteBufAllocator(allocator);
        return this;
    }

    public boolean isAutoSetConv() {
        return kcp.isAutoSetConv();
    }

    public Ukcp setAutoSetConv(boolean autoSetConv) {
        kcp.setAutoSetConv(autoSetConv);
        return this;
    }

    public int waitSnd() {
        return kcp.waitSnd();
    }

    public int getRcvWnd() {
        return kcp.getRcvWnd();
    }

    public Ukcp setRcvWnd(int rcvWnd) {
        kcp.setRcvWnd(rcvWnd);
        return this;
    }

    public int getSndWnd() {
        return kcp.getSndWnd();
    }

    public Ukcp setSndWnd(int sndWnd) {
        kcp.setSndWnd(sndWnd);
        return this;
    }

    public boolean isFastFlush() {
        return fastFlush;
    }


    public boolean isAckNoDelay() {
        return this.kcp.isAckNoDelay();
    }

    public Ukcp setAckNoDelay(boolean ackNoDelay) {
        this.kcp.setAckNoDelay(ackNoDelay);
        return this;
    }


    public void read(ByteBuf byteBuf) {
        //System.out.println("recieve "+Thread.currentThread().getName());
        this.recieveList.add(byteBuf);
        notifyReadEvent();
    }

    /**
     * 主动发消息使用
     * 线程安全的
     *
     * @param byteBuf 发送后需要手动释放
     * @return
     */
    public boolean write(ByteBuf byteBuf) {
        byteBuf = byteBuf.retainedDuplicate();
        if (!sendList.offer(byteBuf)) {
            System.out.println("满了");
            byteBuf.release();
            return false;
        }
        notifyWriteEvent();
        return true;
    }

    public IMessageExecutor getiMessageExecutor() {
        return iMessageExecutor;
    }

    /**
     * 主动关闭连接调用
     */
    public void notifyCloseEvent() {
        this.iMessageExecutor.execute(() -> close());
    }

    private void notifyReadEvent() {
        RecieveTask recieveTask = RecieveTask.New(this);
        this.iMessageExecutor.execute(recieveTask);
    }

    protected void notifyWriteEvent() {
        SendTask sendTask = SendTask.New(this);
        this.iMessageExecutor.execute(sendTask);
    }


    public long getTsUpdate() {
        return tsUpdate;
    }

    public Queue<ByteBuf> getRecieveList() {
        return recieveList;
    }

    public Ukcp setTsUpdate(long tsUpdate) {
        this.tsUpdate = tsUpdate;
        return this;
    }

    public int getState() {
        return kcp.getState();
    }


    public MpscArrayQueue<ByteBuf> getSendList() {
        return sendList;
    }

    public PcpListener getKcpListener() {
        return kcpListener;
    }

    public boolean isActive() {
        return active;
    }


    void close() {
        this.active = false;
    }

    void release() {
        kcp.setState(-1);
        kcp.release();
        for (; ; ) {
            ByteBuf byteBuf = sendList.poll();
            if (byteBuf == null)
                break;
            byteBuf.release();
        }
        for (; ; ) {
            ByteBuf byteBuf = recieveList.poll();
            if (byteBuf == null)
                break;
            byteBuf.release();
        }
    }


    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    @Override
    public String toString() {
        return "Ukcp(" +
                "getConv=" + kcp.getConv() +
                ", state=" + kcp.getState() +
                ", active=" + active +
                ')';
    }
}
