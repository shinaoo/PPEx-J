package ppex.client.socket;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.SocketUtils;
import org.apache.log4j.Logger;
import ppex.client.entity.Client;
import ppex.proto.msg.entity.testpack.Files;
import ppex.proto.msg.Message;
import ppex.proto.msg.entity.Connection;
import ppex.proto.msg.entity.txt.Response;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.pcp.IChannelManager;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.Output;
import ppex.proto.rudp.RudpPack;
import ppex.proto.rudp.RudpScheduleTask;
import ppex.utils.Constants;
import ppex.utils.Identity;
import ppex.utils.MessageUtil;
import ppex.utils.tpool.DisruptorExectorPool;
import ppex.utils.tpool.IMessageExecutor;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class UdpClient {

    private static Logger LOGGER = Logger.getLogger(UdpClient.class);

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private DisruptorExectorPool disruptorExectorPool;
    private List<Channel> channels = new Vector<>(2);
    private IChannelManager channelManager = ClientChannelManager.New();
    private IAddrManager addrManager = ClientAddrManager.getInstance();

    public void startClient() {

        Identity.INDENTITY = Identity.Type.CLIENT.ordinal();
        getLocalInetSocketAddress();

        try {
            bootstrap = new Bootstrap();
            int cpunum = Runtime.getRuntime().availableProcessors();
            disruptorExectorPool = new DisruptorExectorPool();
            IntStream.range(0, cpunum).forEach(val -> disruptorExectorPool.createDisruptorProcessor("disruptro:" + val));
            boolean epoll = Epoll.isAvailable();
            if (epoll) {
                bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
            }
            group = epoll ? new EpollEventLoopGroup(cpunum) : new NioEventLoopGroup(cpunum);
            Class<? extends Channel> channelCls = epoll ? EpollDatagramChannel.class : NioDatagramChannel.class;
            bootstrap.channel(channelCls);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
//                    channel.pipeline().addLast(new IdleStateHandler(0, 10, 0, TimeUnit.SECONDS));
                    channel.pipeline().addLast(new UdpClientHandler(null, disruptorExectorPool, addrManager));
                }
            });
            Channel ch = bootstrap.bind(Constants.PORT3).sync().channel();
            LOGGER.info("client ch local:" + ch.localAddress() + " remote:" + ch.remoteAddress());
//            PcpPack pcpPack = channelManager.get(ch, Client.getInstance().SERVER1);
//
//            if (pcpPack == null) {
//                Connection connection = new Connection("", Client.getInstance().SERVER1, "server1", Constants.NATTYPE.PUBLIC_NETWORK.ordinal(), ch);
//                IMessageExecutor executor = disruptorExectorPool.getAutoDisruptorProcessor();
//                PcpOutput pcpOutput = new ClientOutput();
//                pcpPack = new PcpPack(0x1, null, executor, connection, pcpOutput);
//                channelManager.New(ch, pcpPack);
//            }
//
//            PcpPack finalPcpPack = pcpPack;
//            IntStream.range(0, Integer.MAX_VALUE).forEach(val -> {
//                ByteBuf sndbuf = MessageUtil.makeTestBytebuf("this is test msg");
//                finalPcpPack.write(sndbuf);
//                if (val % 200 == 0) {
//                    try {
//                        TimeUnit.SECONDS.sleep(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });

            RudpPack rudpPack = addrManager.get(Client.getInstance().SERVER1);
            IMessageExecutor executor = disruptorExectorPool.getAutoDisruptorProcessor();
            if (rudpPack == null) {
                Connection connection = new Connection("", Client.getInstance().SERVER1, "server1", Constants.NATTYPE.PUBLIC_NETWORK.ordinal(), ch);
                Output output = new ClientOutput();
                rudpPack = new RudpPack(output, connection, executor, null,null);
                addrManager.New(Client.getInstance().SERVER1, rudpPack);
            }

            //增加reset
            rudpPack.sendReset();

            RudpPack finalpack = rudpPack;
            File file = new File("D:/");
            File[] files = file.listFiles();
            List<Files> fileList = new ArrayList<>();
            Arrays.stream(files).forEach(file1 -> {
                Files files1 = new Files();
                files1.setDirectory(file1.isDirectory());
                files1.setName(files1.getName());
                fileList.add(files1);
            });
            Response response = new Response();
            response.setBody(JSON.toJSONString(fileList));
            response.setHead("/file/getfiles\r\n");
            TxtTypeMsg txtTypeMsg = new TxtTypeMsg();
            txtTypeMsg.setContent(JSON.toJSONString(response));

            IntStream.range(0, 100).forEach(val -> {
//                Message msg = MessageUtil.makeTestStr2Msg("1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1234567891011121314151617181920212223242526272829301你去死吧1");
                Message msg = MessageUtil.txtmsg2Msg(txtTypeMsg);
                finalpack.write(msg);
//                if (val % 32 == 0) {
//                    try {
//                        TimeUnit.SECONDS.sleep(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
            });

            RudpScheduleTask scheduleTask = new RudpScheduleTask(executor, rudpPack, addrManager);
            DisruptorExectorPool.scheduleHashedWheel(scheduleTask, rudpPack.getInterval());


            //1.探测阶段.开始DetectProcess
//            DetectProcess.getInstance().setChannel(ch);
//            DetectProcess.getInstance().startDetect();
//
//            Client.getInstance().NAT_TYPE = DetectProcess.getInstance().getClientNATType().ordinal();
//            System.out.println("Client NAT type is :" + Client.getInstance().NAT_TYPE);
//
//            Connection connection = new Connection(Client.getInstance().MAC_ADDRESS,Client.getInstance().address,
//                    Client.getInstance().peerName,Client.getInstance().NAT_TYPE,ch);
//            Client.getInstance().localConnection = connection;
//
//            //2.穿越阶段
//            ThroughProcess.getInstance().setChannel(ch);
//            ThroughProcess.getInstance().sendSaveInfo();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
            while (true) {
                TimeUnit.SECONDS.sleep(5);
                LOGGER.info("client while...");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    private void getLocalInetSocketAddress() {
        Client.getInstance();
        try {
            InetAddress address = InetAddress.getLocalHost();//获取的是本地的IP地址 //PC-20140317PXKX/192.168.0.121
            String hostAddress = address.getHostAddress();//192.168.0.121
            Client.getInstance().local_address = address.getHostAddress();
            Client.getInstance().SERVER1 = SocketUtils.socketAddress(Constants.SERVER_HOST1, Constants.PORT1);
            Client.getInstance().SERVER2P1 = SocketUtils.socketAddress(Constants.SERVER_HOST2, Constants.PORT1);
            Client.getInstance().SERVER2P2 = SocketUtils.socketAddress(Constants.SERVER_HOST2, Constants.PORT2);
            Client.getInstance().MAC_ADDRESS = getMacAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private String getMacAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            byte[] mac = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || netInterface.isPointToPoint() || !netInterface.isUp()) {
                    continue;
                } else {
                    mac = netInterface.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X", mac[i]));
                        }
                        if (sb.length() > 0) {
                            return sb.toString();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            return "";
        }
        return "";
    }

    public void stop() {
        channels.forEach(channel -> channel.close());
        if (disruptorExectorPool != null) {
            disruptorExectorPool.stop();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}
