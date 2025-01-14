package handling.cashshop;

import client.inventory.IItem;
import client.inventory.Item;
import database.DatabaseConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import tools.collection.Pair;

@Slf4j
public class CashShopCoupon {

    public static boolean getCouponCodeValid(String code) {
        boolean isValidCoupon = false;
        try (var con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT `used` FROM `coupons` WHERE `code` = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        isValidCoupon = (rs.getByte("used") == 0);
                    }
                }
            }
        } catch (SQLException e) {
            log.info("Error getting NX Code type " + e);
        }
        return isValidCoupon;
    }

    public static List<CashCouponData> getCouponData(final String code) {
        List<CashCouponData> all = new ArrayList<>();
        try (var con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT `type`, `itemData`, `quantity` FROM `coupons_data` WHERE `code`" + " = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final byte type = rs.getByte("type");
                        final int itemdata = rs.getInt("itemData");
                        final int quantity = rs.getInt("quantity");
                        all.add(new CashCouponData(type, itemdata, quantity));
                    }
                }
            }
        } catch (SQLException e) {
            log.info("Error getting Coupon Data " + e);
        }
        return all;
    }

    public static void setCouponCodeUsed(String name, String code) {
        try (var con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps =
                    con.prepareStatement("UPDATE `coupons` SET `character` = ?, `used` = 1 WHERE code = ?")) {
                ps.setString(1, name);
                ps.setString(2, code);
                ps.execute();
            }
        } catch (SQLException e) {
            log.info("Error getting Coupon Data " + e);
        }
    }

    public static Pair<Pair<Integer, Integer>, Pair<List<IItem>, Integer>> getSize(List<CashCouponData> ccd) {
        int MaplePoints = 0, mesos = 0, Cashsize = 0;
        final List<IItem> togiveII = new ArrayList<>();
        for (CashCouponData hmm : ccd) {
            switch (hmm.getType()) {
                case 0: // Maple Points
                    if (hmm.getData() > 0) {
                        MaplePoints += hmm.getData();
                    }
                    break;
                case 1: // Cash Items
                    Cashsize++;
                    break;
                case 2: // Normal items
                    if (hmm.getQuantity() <= Short.MAX_VALUE && hmm.getQuantity() > 0) {
                        togiveII.add(new Item(hmm.getData(), (short) 0, (short) hmm.getQuantity(), (byte) 0));
                    }
                    break;
                case 3: // Mesos
                    if (hmm.getData() > 0) {
                        mesos += hmm.getData();
                    }
                    break;
            }
        }
        return new Pair<>(new Pair<>(MaplePoints, Cashsize), new Pair<>(togiveII, mesos));
    }
}
