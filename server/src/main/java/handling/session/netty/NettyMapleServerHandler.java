package handling.session.netty;

import client.MapleClient;
import constants.ServerConstants;
import handling.PacketProcessor;
import handling.cashshop.CashShopServer;
import handling.session.HandlerHelper;
import handling.world.WorldServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import tools.Randomizer;
import tools.packet.LoginPacket;

@Slf4j
public class NettyMapleServerHandler extends ChannelInboundHandlerAdapter {
    public static final AttributeKey<Object> CLIENT_KEY = AttributeKey.valueOf(MapleClient.CLIENT_KEY);
    private final boolean isCashShop;
    private final PacketProcessor processor;
    private final int channel;

    public NettyMapleServerHandler(int channel, boolean isCashShop, PacketProcessor processor) {
        this.channel = channel;
        this.isCashShop = isCashShop;
        this.processor = processor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (channel > -1) {
            if (WorldServer.getInstance().getChannel(channel).isShutdown()) {
                ctx.channel().close();
                return;
            }
        } else if (isCashShop) {
            if (CashShopServer.getInstance().isShutdown()) {
                ctx.channel().close();
                return;
            }
        }
        final byte[] ivSend = new byte[]{82, 48, 120, (byte) Randomizer.nextInt(255)};
        final byte[] ivRecv = new byte[]{70, 114, 122, (byte) Randomizer.nextInt(255)};
        final var client = new MapleClient(ivSend, ivRecv, new NettySession(ctx.channel()));
        client.setChannel(channel);
        NettyMaplePacketEncoder encoder = new NettyMaplePacketEncoder();
        ctx.pipeline().addFirst(new NettyMaplePacketDecoder(client), encoder);
        ctx.channel().writeAndFlush(LoginPacket.getHello(ServerConstants.MAPLE_VERSION, ivSend, ivRecv));
        ctx.channel().attr(CLIENT_KEY).set(client);
        encoder.setClient(client);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] message = (byte[]) msg;
        var client = (MapleClient) ctx.channel().attr(AttributeKey.valueOf(MapleClient.CLIENT_KEY)).get();
        HandlerHelper.handlePacket(client, processor, isCashShop, message);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception with client", cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        final MapleClient client = (MapleClient) ctx.channel().attr(CLIENT_KEY).get();
        if (client != null) {
            try {
                client.disconnect(true, isCashShop);
            } finally {
                ctx.channel().close();
                ctx.channel().attr(CLIENT_KEY).remove();
            }
        }
    }

}