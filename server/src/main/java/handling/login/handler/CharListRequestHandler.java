package handling.login.handler;

import client.MapleClient;
import client.inventory.IItem;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import database.CharacterData;
import database.CharacterListResult;
import database.LoginService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import networking.data.input.InPacket;
import networking.data.output.OutPacket;
import networking.packet.AbstractMaplePacketHandler;
import networking.packet.SendPacketOpcode;

@Slf4j
public class CharListRequestHandler extends AbstractMaplePacketHandler {

    public static final int SECOND_PASSWORD_REQUEST = 2;

    @Override
    public void handlePacket(InPacket packet, MapleClient c) {
        packet.readByte();
        final int server = packet.readByte();
        final int channel = packet.readByte() + 1;

        c.setWorld(server);
        c.setChannel(channel);

        CharacterListResult list =
                LoginService.loadCharacterList(c.getAccountData().getId(), 0);
        if (list.getCharacters() != null) {
            c.getSession().write(getCharList(list.getCharacters(), c.getCharacterSlots()));
        } else {
            c.getSession().close();
        }
    }

    private static byte[] getCharList(final List<CharacterData> chars, int charslots) {
        final var packet = new OutPacket();

        packet.writeShort(SendPacketOpcode.CHARLIST.getValue());
        packet.write(0);
        packet.write(chars.size()); // 1

        for (final CharacterData chr : chars) {
            addCharEntry(packet, chr, false);
        }
        packet.write(SECOND_PASSWORD_REQUEST);
        packet.writeLong(charslots);

        return packet.getPacket();
    }

    private static void addCharEntry(final OutPacket packet, final CharacterData chr, boolean ranking) {
        addCharStats(packet, chr);
        addCharLook(packet, chr, true);
        packet.write(0);
        packet.write(ranking ? 1 : 0);
        if (ranking) {
            packet.writeInt(0);
            packet.writeInt(0);
            packet.writeInt(0);
            packet.writeInt(0);
        }
    }

    private static void addCharStats(final OutPacket packet, final CharacterData chr) {
        packet.writeInt(chr.getId()); // character id
        packet.writeAsciiString(chr.getName(), 13);
        packet.write(chr.getGender()); // gender (0 = male, 1 = female)
        packet.write(chr.getSkinColor()); // skin color
        packet.writeInt(chr.getFace()); // face
        packet.writeInt(chr.getHair()); // hair
        for (int i = 0; i < 3; i++) {
            packet.writeLong(0);
        }
        packet.write(chr.getLevel()); // level
        packet.writeShort(chr.getJob()); // job
        chr.connectData(packet);
        packet.writeShort(Math.min(199, chr.getAp())); // Avoid Popup
        if (chr.getJob() == 2001 || chr.isEvan()) {
            packet.write(0);
        } else {
            packet.writeShort(chr.getSp()); // remaining sp
        }
        packet.writeInt(chr.getExp()); // exp
        packet.writeShort(chr.getFame()); // fame
        packet.writeInt(0); // Gachapon exp
        packet.writeInt(chr.getMap()); // current map id
        packet.write(chr.getSpawnPoint()); // spawnpoint
        packet.writeInt(0);
        packet.writeShort(chr.getSubCategory()); // 1 here = db
    }

    private static void addCharLook(final OutPacket packet, final CharacterData chr, final boolean mega) {
        packet.write(chr.getGender());
        packet.write(chr.getSkinColor());
        packet.writeInt(chr.getFace());
        packet.write(mega ? 0 : 1);
        packet.writeInt(chr.getHair());

        final Map<Byte, Integer> myEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> maskedEquip = new LinkedHashMap<>();
        MapleInventory equip = chr.getInventory(MapleInventoryType.EQUIPPED);

        for (final IItem item : equip.list()) {
            if (item.isNotVisible()) { // not visible
                continue;
            }
            byte pos = (byte) (item.getPosition() * -1);

            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getItemId());
            } else if ((pos > 100 || pos == -128) && pos != 111) {
                pos = (byte) (pos == -128 ? 28 : pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getItemId());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getItemId());
            }
        }
        for (final Map.Entry<Byte, Integer> entry : myEquip.entrySet()) {
            packet.write(entry.getKey());
            packet.writeInt(entry.getValue());
        }

        packet.write(0xFF);

        // end of visible itens
        // masked itens
        for (final Map.Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            packet.write(entry.getKey());
            packet.writeInt(entry.getValue());
        }
        packet.write(0xFF); // ending markers

        final IItem cWeapon = equip.getItem((byte) -111);
        packet.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
        for (int i = 0; i < 3; i++) {
            packet.writeInt(0);
        }
    }
}
