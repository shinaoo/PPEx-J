package ppex.server.handlers;

import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;
import ppex.proto.type.ProbeTypeMsg;
import ppex.proto.type.TypeMessage;
import ppex.proto.type.TypeMessageHandler;
import ppex.server.entity.Server;
import ppex.utils.Identity;
import ppex.utils.MessageUtil;

public class ProbeTypeMsgHandler implements TypeMessageHandler {

    private Logger LOGGER = Logger.getLogger(ProbeTypeMsgHandler.class);

    @Override
    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage msg) throws Exception{
        if (msg.getType() != TypeMessage.Type.MSG_TYPE_PROBE.ordinal())
            return;
        ProbeTypeMsg pmsg = JSON.parseObject(msg.getBody(),ProbeTypeMsg.class);
        if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER1.ordinal()){
            if(Identity.INDENTITY == Identity.Type.SERVER1.ordinal()){
                throw new Exception("Wrong ProbeTypeMsg:" + msg.toString());
            }else if (Identity.INDENTITY == Identity.Type.CLIENT.ordinal()){
                handleServer1FromClientMsg(ctx,pmsg);
            }else if (Identity.INDENTITY == Identity.Type.SERVER2_PORT1.ordinal()){
                handleServer1FromServer2Port1Msg(ctx,pmsg);
            }else if (Identity.INDENTITY == Identity.Type.SERVER2_PORT2.ordinal()){
                handleServer1FromServer2Port2Msg(ctx,pmsg);
            }else{
                throw new Exception("Unknown ProbeTypeMsg:" + pmsg.toString());
            }
        }else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER2_PORT1.ordinal()){
            if(Identity.INDENTITY == Identity.Type.SERVER2_PORT1.ordinal()){
                throw new Exception("Wrong ProbeTypeMsg:" + msg.toString());
            }else if (Identity.INDENTITY == Identity.Type.CLIENT.ordinal()){
                handleServer2Port1FromClientMsg(ctx,pmsg);
            }else if (Identity.INDENTITY == Identity.Type.SERVER1.ordinal()){
                handleServer2Port1FromServer1Msg(ctx,pmsg);
            }else if (Identity.INDENTITY == Identity.Type.SERVER2_PORT2.ordinal()){
                handleServer2Port1FromServer2Port2Msg(ctx,pmsg);
            }else{
                throw new Exception("Unknown ProbeTypeMsg:" + pmsg.toString());
            }
        }else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER2_PORT2.ordinal()){
            if(Identity.INDENTITY == Identity.Type.SERVER2_PORT2.ordinal()){
                throw new Exception("Wrong ProbeTypeMsg:" + msg.toString());
            }else if (Identity.INDENTITY == Identity.Type.CLIENT.ordinal()){
                handleServer2Port2FromClientMsg(ctx,pmsg);
            }else if (Identity.INDENTITY == Identity.Type.SERVER1.ordinal()){
                handleServer2Port2FromServer1Msg(ctx,pmsg);
            }else if (Identity.INDENTITY == Identity.Type.SERVER2_PORT1.ordinal()){
                handleServer2Port2FromServer2Port1Msg(ctx,pmsg);
            }else{
                throw new Exception("Unknown ProbeTypeMsg:" + pmsg.toString());
            }
        }else{
            throw new Exception("Unknown ProbeTypeMsg:" + pmsg.toString());
        }
    }


    //server1处理消息
    private void handleServer1FromClientMsg(ChannelHandlerContext ctx,ProbeTypeMsg msg) {
        LOGGER.info("s1 handle msg recv from client:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()){
            //向client发回去包,并且向server2:port1发包
            msg.setType(ProbeTypeMsg.Type.FROM_SERVER1.ordinal());
            msg.setRecordInetSocketAddress(msg.getFromInetSocketAddress());
            msg.setFromInetSocketAddress(Server.getInstance().SERVER1);
            ctx.writeAndFlush(MessageUtil.probemsg2Packet(msg,msg.getRecordInetSocketAddress()));
            ctx.writeAndFlush(MessageUtil.probemsg2Packet(msg,Server.getInstance().SERVER2P1));
        }
    }

    private void handleServer1FromServer2Port1Msg(ChannelHandlerContext ctx,ProbeTypeMsg msg) {
        //目前没有从server2port1发送到server1的消息
    }

    private void handleServer1FromServer2Port2Msg(ChannelHandlerContext ctx,ProbeTypeMsg msg){
        //没有从server2port2发送到server1的消息
    }

    //Server2:Port1处理消息
    private void handleServer2Port1FromClientMsg(ChannelHandlerContext ctx,ProbeTypeMsg msg) {

    }

    private void handleServer2Port1FromServer1Msg(ChannelHandlerContext ctx,ProbeTypeMsg msg) {

    }

    private void handleServer2Port1FromServer2Port2Msg(ChannelHandlerContext ctx,ProbeTypeMsg msg){

    }

    //Server2:Port2处理消息
    private void handleServer2Port2FromClientMsg(ChannelHandlerContext ctx,ProbeTypeMsg msg){

    }

    private void handleServer2Port2FromServer1Msg(ChannelHandlerContext ctx,ProbeTypeMsg msg){

    }

    private void handleServer2Port2FromServer2Port1Msg(ChannelHandlerContext ctx,ProbeTypeMsg msg){

    }




}