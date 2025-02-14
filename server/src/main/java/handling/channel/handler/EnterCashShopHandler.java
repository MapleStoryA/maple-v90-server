package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import constants.MapConstants;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.world.ServerMigration;
import handling.world.WorldServer;
import handling.world.helper.CharacterTransfer;
import handling.world.messenger.MapleMessengerCharacter;
import handling.world.messenger.MessengerManager;
import lombok.extern.slf4j.Slf4j;
import networking.data.input.InPacket;
import networking.packet.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;

@Slf4j
public class EnterCashShopHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(InPacket packet, MapleClient c) {
        MapleCharacter chr = c.getPlayer();

        if (!chr.isAlive()
                || chr.getEventInstance() != null
                || c.getChannelServer() == null
                || MapConstants.isStorylineMap(chr.getMapId())) {
            c.getSession().write(MaplePacketCreator.serverBlocked(2));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        final ChannelServer ch = WorldServer.getInstance().getChannel(c.getChannel());

        chr.changeRemoval();

        if (chr.getMessenger() != null) {
            var participant = new MapleMessengerCharacter(chr);
            MessengerManager.leaveMessenger(chr.getMessenger().getId(), participant);
        }

        ch.removePlayer(chr);
        chr.saveToDB(false, false);
        chr.getMap().removePlayer(chr);
        c.setPlayer(null);

        ServerMigration entry = new ServerMigration(chr.getId(), c.getAccountData(), c.getSessionIPAddress());
        entry.setCharacterTransfer(new CharacterTransfer(chr));
        entry.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        entry.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
        entry.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
        WorldServer.getInstance().getMigrationService().putMigrationEntry(entry);

        c.getSession()
                .write(MaplePacketCreator.getChannelChange(Integer.parseInt(
                        CashShopServer.getInstance().getPublicAddress().split(":")[1])));
    }
}
