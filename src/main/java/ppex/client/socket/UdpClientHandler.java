package ppex.client.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.log4j.Logger;
import ppex.client.entity.Client;
import ppex.client.handlers.PongTypeMsgHandler;
import ppex.client.handlers.ProbeTypeMsgHandler;
import ppex.client.handlers.ThroughTypeMsgHandler;
import ppex.client.handlers.TxtTypeMsgHandler;
import ppex.proto.msg.Message;
import ppex.proto.msg.MessageHandler;
import ppex.proto.msg.StandardMessageHandler;
import ppex.proto.msg.entity.Connection;
import ppex.proto.msg.type.PingTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.pcp.IChannelManager;
import ppex.proto.pcp.PcpListener;
import ppex.proto.pcp.PcpOutput;
import ppex.proto.pcp.PcpPack;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.Output;
import ppex.proto.rudp.ResponseListener;
import ppex.proto.rudp.RudpPack;
import ppex.utils.Constants;
import ppex.utils.MessageUtil;
import ppex.utils.tpool.DisruptorExectorPool;
import ppex.utils.tpool.IMessageExecutor;


public class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> implements ResponseListener {

    private static Logger LOGGER = Logger.getLogger(UdpClientHandler.class);

    private MessageHandler msgHandler;

    private PcpListener pcpListener;
    private DisruptorExectorPool disruptorExectorPool;
    private IChannelManager channelManager;
    private IAddrManager addrManager;

    public UdpClientHandler(PcpListener pcpListener, DisruptorExectorPool disruptorExectorPool, IAddrManager addrManager) {
        msgHandler = StandardMessageHandler.New();
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(), new ProbeTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_THROUGH.ordinal(), new ThroughTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_HEART_PONG.ordinal(),new PongTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_TXT.ordinal(),new TxtTypeMsgHandler());

        this.pcpListener = pcpListener;
        this.disruptorExectorPool = disruptorExectorPool;
        this.addrManager = addrManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        try {
            Channel channel = channelHandlerContext.channel();
//            LOGGER.info("ClientHandler channel local:" + channel.localAddress() + " remote:" + channel.remoteAddress());
//            PcpPack pcpPack = channelManager.get(channel,datagramPacket.sender());
//            if (pcpPack == null){
//                Connection connection = new Connection("",datagramPacket.sender(),"From1", Constants.NATTYPE.UNKNOWN.ordinal(),channel);
//                IMessageExecutor executor = disruptorExectorPool.getAutoDisruptorProcessor();
//                PcpOutput pcpOutput = new ClientOutput();
//                pcpPack = new PcpPack(0x1,null,executor,connection,pcpOutput);
//                channelManager.New(channel,pcpPack);
//            }
//            pcpPack.read(datagramPacket.content());

            RudpPack rudpPack = addrManager.get(datagramPacket.sender());
            if (rudpPack == null){
                IMessageExecutor executor = disruptorExectorPool.getAutoDisruptorProcessor();
                Connection connection = new Connection("",Client.getInstance().SERVER1,"server1",Constants.NATTYPE.PUBLIC_NETWORK.ordinal(),channel);
                Output output = new ClientOutput();
                rudpPack = new RudpPack(output,connection,executor,null);
                addrManager.New(Client.getInstance().SERVER1,rudpPack);
            }
            rudpPack.read(datagramPacket.content());

//            msgHandler.handleDatagramPacket(channelHandlerContext, datagramPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case WRITER_IDLE:
                    handleWriteIdle(ctx);
                    break;
                case READER_IDLE:
                    handleReadIdle();
                    break;
                case ALL_IDLE:
                    handleAllIdle();
                    break;
                default:
                    break;
            }
        }
    }

    private void handleWriteIdle(ChannelHandlerContext ctx){
        LOGGER.info("client handleWriteIdle");
        //心跳包
        PingTypeMsg pingTypeMsg = new PingTypeMsg();
//        pingTypeMsg.setType(PingTypeMsg.Type.HEART.ordinal());
//        pingTypeMsg.setContent(JSON.toJSONString(Client.getInstance().localConnection));
        ctx.writeAndFlush(MessageUtil.pingMsg2Packet(pingTypeMsg, Client.getInstance().SERVER1));
    }
    private void handleReadIdle(){
        LOGGER.info("client handleReadIdle");
    }
    private void handleAllIdle(){
        LOGGER.info("client handleAllIdle");
    }

//    @Override
//    public void onResponse(Message message) {
//        LOGGER.info("client response msg:" + message.getContent());
//    }
}
