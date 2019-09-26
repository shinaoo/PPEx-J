package ppex.server.socket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.apache.log4j.Logger;
import ppex.proto.MessageHandler;
import ppex.proto.StandardMessageHandler;
import ppex.proto.type.TypeMessage;
import ppex.server.handlers.ProbeTypeMsgHandler;

public class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private Logger logger = Logger.getLogger(UdpServerHandler.class);


    private MessageHandler msgHandler;

    public UdpServerHandler() {
        msgHandler = new StandardMessageHandler();
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(),new ProbeTypeMsgHandler());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.info("---->channel Active");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("---->channel inactive:" + ctx.channel().remoteAddress());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause);
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        try {
            msgHandler.handleDatagramPacket(channelHandlerContext,datagramPacket);
        }catch (Exception e){
            logger.warn("---->ChannelRead0 exception:" + e.getMessage());
            System.out.println("server recv msg error");
        }
    }
}
