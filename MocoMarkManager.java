package vpn.team09.hax;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public final class MocoMarkManager {
    private static final Object LOCK = new Object();
    private static final LinkedHashSet<Long> entities = new LinkedHashSet<>();
    private static final byte[] CRC_TABLE = buildCrcTable();
    private static volatile boolean enabled;
    private static volatile boolean clearPending;
    private static volatile long localPlayerId;
    private static volatile long lastInjection;
    private static volatile long lastSkipLog;

    private static volatile int selectedSkillId = 1;

    private MocoMarkManager() {
    }

    public static void setEnabled(boolean value) {
        if (enabled && !value) clearPending = true;
        enabled = value;
        NetworkLog.state("Moco enabled=" + value);
    }

    public static void setSelectedSkillId(int id) {
        selectedSkillId = id;
        NetworkLog.state("Moco selected skill ID=" + id);
    }

    public static int getSelectedSkillId() {
        return selectedSkillId;
    }

    public static void reset() {
        synchronized (LOCK) {
            entities.clear();
        }
        localPlayerId = 0;
        lastInjection = 0;
        lastSkipLog = 0;
        clearPending = false;
    }

    public static void handleGameCommand(int command, byte[] body, boolean downlink) {
        if (body == null) return;
        if (downlink && command == 101) {
            Pair<Long, Integer> first = readVarint(body, 0);
            Pair<Long, Integer> second = first == null ? null : readVarint(body, first.second);
            Pair<Long, Integer> third = second == null ? null : readVarint(body, second.second);
            if (third != null && third.first != 0) {
                synchronized (LOCK) {
                    entities.add(third.first & 0xffff_ffffL);
                }
                NetworkLog.state("Moco entity add count=" + entities.size());
            }
        } else if (downlink && command == 102) {
            Pair<Long, Integer> value = readVarint(body, 0);
            if (value != null && value.first != 0) {
                synchronized (LOCK) { entities.remove(value.first); }
                NetworkLog.state("Moco entity remove");
            }
        } else if (downlink && command == 103) {
            synchronized (LOCK) {
                entities.clear();
            }
            NetworkLog.state("Moco match end");
        } else if (command == 104 || command == 105 || command == 2001) {
            Pair<Long, Integer> value = readVarint(body, 0);
            if (value != null && isEntityId(value.first)) {
                localPlayerId = value.first;
                NetworkLog.state("Moco local player learned");
            }
        }
    }

    public static List<byte[]> buildPackets(String flowId) {
        if (!enabled && !clearPending) {
            skip("disabled");
            return Collections.emptyList();
        }
        long now = SystemClock.uptimeMillis();
        if (!clearPending && now - lastInjection < 2000) return Collections.emptyList();

        GameProtocolEngine protocol = GameProtocolEngine.INSTANCE;
        if (!protocol.hasLearnedHeaderOrder()) {
            skip("noorder");
            return Collections.emptyList();
        }
        byte[] key = protocol.getSessionKey(flowId);
        int xor = protocol.getXorKey(flowId);
        if (key == null || xor < 0 || localPlayerId <= 0) {
            skip("missing key/xor/player key=" + (key != null) + " xor=" + xor
                    + " player=" + localPlayerId);
            return Collections.emptyList();
        }

        ArrayList<Long> targets = new ArrayList<>();
        if (enabled) {
            synchronized (LOCK) {
                for (Long id : entities) if (id != localPlayerId) targets.add(id);
            }
            if (targets.isEmpty() && !clearPending) {
                skip("no entities");
                return Collections.emptyList();
            }
        }

        byte[] raw = new byte[16 + targets.size() * 16];
        int offset = writeVarint(raw, 0, targets.isEmpty() ? localPlayerId : targets.get(0));
        offset = writeVarint(raw, offset, targets.size() << 1);
        int skillId = selectedSkillId;
        for (Long id : targets) {
            offset = writeVarint(raw, offset, id);
            raw[offset++] = (byte) skillId;
            offset = writeVarint(raw, offset, 65535);
            offset = writeVarint(raw, offset, 0);
        }
        byte[] plain = java.util.Arrays.copyOf(raw, offset);
        byte[] encrypted = TeaCipher.encryptCbc(plain, key);
        if (encrypted == null) return Collections.emptyList();

        lastInjection = now;
        clearPending = false;
        NetworkLog.state("Moco inject entities=" + targets.size() + " skillId=" + skillId
                + " len=" + (encrypted.length + 8) + " xor=" + xor + " flow=" + flowId);
        Log.i("09Team", "Tạo gói mark cho " + targets.size() + " thực thể (skillId=" + skillId + ")");
        return Collections.singletonList(protocol.wrapHeader(flowId, encrypted, xor));
    }

    private static void skip(String reason) {
        long now = SystemClock.uptimeMillis();
        if (now - lastSkipLog >= 2000L) {
            lastSkipLog = now;
            NetworkLog.state("Moco skip " + reason);
        }
    }

    static byte[] crcTable() {
        return CRC_TABLE;
    }

    private static byte[] buildCrcTable() {
        return new byte[]{0,9,18,27,36,45,54,63,72,65,90,83,108,101,126,119,
                25,16,11,2,61,52,47,38,81,88,67,74,117,124,103,110,
                50,59,32,41,22,31,4,13,122,115,104,97,94,87,76,69,
                43,34,57,48,15,6,29,20,99,106,113,120,71,78,85,92,
                100,109,118,Byte.MAX_VALUE,64,73,82,91,44,37,62,55,
                8,1,26,19,125,116,111,102,89,80,75,66,53,60,39,46,
                17,24,3,10,86,95,68,77,114,123,96,105,30,23,12,5,
                58,51,40,33,79,70,93,84,107,98,121,112,7,14,21,28,
                35,42,49,56,65,72,83,90,101,108,119,126,9,0,27,18,
                45,36,63,54,88,81,74,67,124,117,110,103,16,25,2,11,
                52,61,38,47,115,122,97,104,87,94,69,76,59,50,41,32,
                31,22,13,4,106,99,120,113,78,71,92,85,34,43,48,57,
                6,15,20,29,37,44,55,62,1,8,19,26,109,100,Byte.MAX_VALUE,118,
                73,64,91,82,60,53,46,39,24,17,10,3,116,125,102,111,
                80,89,66,75,23,30,5,12,51,58,33,40,95,86,77,68,
                123,114,105,96,14,7,28,21,42,35,56,49,70,79,84,93,
                98,107,112,121};
    }

    private static Pair<Long, Integer> readVarint(byte[] data, int offset) {
        long value = 0;
        int shift = 0;
        int index = offset;
        while (index < data.length && shift <= 63) {
            int current = data[index++] & 255;
            value |= (long) (current & 127) << shift;
            if ((current & 128) == 0) return new Pair<>(value, index);
            shift += 7;
        }
        return null;
    }

    /** Exact entity-id shape used by Hb.c() in crg_mod_1/classes.dex. */
    private static boolean isEntityId(long value) {
        if (value <= 0 || value > 0x7fffffffL) return false;
        int low = (int) (value & 0xffL);
        int high = (int) ((value >>> 24) & 0xffL);
        long middle = (value >>> 8) & 0xffffL;
        return low >= 1 && low < 81 && high >= 1 && high < 128 && middle == 0;
    }

    private static int writeVarint(byte[] data, int offset, long value) {
        long remaining = value;
        while ((remaining & ~127L) != 0) {
            data[offset++] = (byte) ((remaining & 127) | 128);
            remaining >>>= 7;
        }
        data[offset++] = (byte) remaining;
        return offset;
    }
}
