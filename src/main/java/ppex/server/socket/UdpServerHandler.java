package ppex.server.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.log4j.Logger;
import ppex.proto.msg.MessageHandler;
import ppex.proto.msg.StandardMessageHandler;
import ppex.proto.msg.entity.Connection;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.pcp.*;
import ppex.server.handlers.*;
import ppex.server.myturn.ServerOutput;
import ppex.utils.tpool.DisruptorExectorPool;
import ppex.utils.tpool.IMessageExecutor;

public class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private Logger LOGGER = Logger.getLogger(UdpServerHandler.class);

    private IChannelManager channelManager;

    private MessageHandler msgHandler;

    private PcpListener pcpListener;
    private DisruptorExectorPool disruptorExectorPool;

    public UdpServerHandler(PcpListener pcpListener, DisruptorExectorPool disruptorExectorPool, IChannelManager channelManager) {
        msgHandler = StandardMessageHandler.New();
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(), new ProbeTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_THROUGH.ordinal(), new ThroughTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_HEART_PING.ordinal(), new PingTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_FILE.ordinal(), new FileTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_TXT.ordinal(), new TxtTypeMsgHandler());

        this.pcpListener = pcpListener;
        this.disruptorExectorPool = disruptorExectorPool;
        this.channelManager = channelManager;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        LOGGER.info("---->channel Active");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("---->channel inactive:" + ctx.channel().remoteAddress());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(cause);
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        try {
            LOGGER.info("UdpServerHandler channelread0:" + datagramPacket.sender());
            Channel channel = channelHandlerContext.channel();
            IMessageExecutor executor = disruptorExectorPool.getAutoDisruptorProcessor();
//            PcpPack pcpPack = channelManager.get(channel, datagramPacket);
            Ukcp ukcp = channelManager.get(channel,datagramPacket);
            if (ukcp == null) {
                Connection connection = new Connection("", datagramPacket.sender(), "", 0, channel);
                PcpOutput pcpOutput = new ServerOutput();

                ukcp = new Ukcp(pcpOutput, pcpListener, executor, connection);
                channelManager.New(channel, ukcp);
            }
            ukcp.read(datagramPacket.content());
            ScheduleTask scheduleTask = new ScheduleTask(executor, ukcp, channelManager);
            DisruptorExectorPool.scheduleHashedWheel(scheduleTask, ukcp.getInterval());
            //2019-10-30修改.使用pcppack
//            msgHandler.handleDatagramPacket(channelHandlerContext, datagramPacket);

            //保留测试用
//            TypeMessage msg = MessageUtil.packet2Typemsg(datagramPacket);
//            ProbeTypeMsg pmsg = JSON.parseObject(msg.getBody(),ProbeTypeMsg.class);
//            pmsg.setFromInetSocketAddress(datagramPacket.sender());
//            if (pmsg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()){
//                //向client发回去包,并且向server2:port1发包
//                //19-10-8优化,向Server2:Port2发包
//                pmsg.setType(ProbeTypeMsg.Type.FROM_SERVER1.ordinal());
//                pmsg.setRecordInetSocketAddress(pmsg.getFromInetSocketAddress());
//                pmsg.setFromInetSocketAddress(Server.getInstance().SERVER1);
////                channelHandlerContext.writeAndFlush(MessageUtil.probemsg2Packet(pmsg,pmsg.getRecordInetSocketAddress()));
////                channelHandlerContext.channel().writeAndFlush(new DatagramPacket())
//                DatagramPacket returnMsg = MessageUtil.probemsg2Packet(pmsg,datagramPacket.sender());
//                System.out.println("datagram sender:" + datagramPacket.sender().toString() + " record:" + pmsg.getRecordInetSocketAddress().toString() + " rece:" + datagramPacket.recipient().toString());
//                channelHandlerContext.writeAndFlush(returnMsg);
//                System.out.println("write to client");
//            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("---->ChannelRead0 exception:" + e.getCause());
            System.out.println("server recv msg error");
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case ALL_IDLE:
                    handleAllIdleEvent(ctx, e);
                    break;
                case READER_IDLE:
                    handleReadIdleEvent(ctx, e);
                    break;
                case WRITER_IDLE:
                    handleWriteIdleEvent(ctx, e);
                    break;
                default:
                    break;
            }
        }
    }

    private void handleAllIdleEvent(ChannelHandlerContext ctx, IdleStateEvent e) {
        LOGGER.info("server all idleEvent");
    }

    private void handleReadIdleEvent(ChannelHandlerContext ctx, IdleStateEvent e) {
        LOGGER.info("server read idleEvent");
    }

    private void handleWriteIdleEvent(ChannelHandlerContext ctx, IdleStateEvent e) {
        LOGGER.info("server write idleEvent");
    }

}
