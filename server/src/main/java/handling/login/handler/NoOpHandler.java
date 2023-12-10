package handling.login.handler;

import client.MapleClient;
import handling.AbstractMaplePacketHandler;
import tools.data.input.InPacket;

public class NoOpHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(InPacket packet, MapleClient c) {}
}