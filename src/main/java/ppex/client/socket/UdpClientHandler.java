package ppex.client.socket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import ppex.client.handlers.ProbeTypeMsgHandler;
import ppex.proto.MessageHandler;
import ppex.proto.StandardMessageHandler;
import ppex.proto.type.TypeMessage;


public class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private MessageHandler msgHandler;

    public UdpClientHandler() {
        msgHandler = new StandardMessageHandler();
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(),new ProbeTypeMsgHandler());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        try {
            msgHandler.handleDatagramPacket(channelHandlerContext, datagramPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Message msg = MessageUtil.packet2Msg(datagramPacket);
//        if (msg != null){
//            System.out.println("client recv:" + msg.toString() + " from:" + datagramPacket.sender());
//        }else{
//            System.out.println("client recv error");
//        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}