/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/UnsignedUUID.java
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.mcres.karlatemp.yop.modesl;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

@JsonAdapter(UnsignedUUID.Serializer.class)
public final class UnsignedUUID {
    private static final long[] ff = new long[128];

    static {
        int x = 0;
        for (int i = '0'; i <= '9'; i++, x++) {
            ff[i] = x;
        }
        int rd = 'A' - 'a';
        for (int i = 'a'; i <= 'f'; i++, x++) {
            ff[i] = ff[i + rd] = x;
        }
    }

    public static long dec(String st) {
        long rq = 0;
        for (char c : st.toCharArray()) {
            rq <<= 4;
            rq |= ff[c];
        }
        return rq;
    }

    public static UnsignedUUID parse(String uid) {
//        UUID.fromString(uid);
        if (uid.length() != 32) {
            throw new IllegalArgumentException("Invalid UnsignedUUID string: " + uid);
        }
        return new UnsignedUUID(dec(uid.substring(0, 16)),
                dec(uid.substring(16, 32))
        );
    }

    private final UUID uuid;

    public UnsignedUUID(UUID uid) {
        uuid = uid;
    }

    public UnsignedUUID(long mostSigBits, long leastSigBits) {
        this(new UUID(mostSigBits, leastSigBits));
    }

    public UUID getUUID() {
        return uuid;
    }

    public long getMostSignificantBits() {
        return uuid.getMostSignificantBits();
    }

    public long getLeastSignificantBits() {
        return uuid.getLeastSignificantBits();
    }

    public String toString() {
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }

    public static final class Serializer extends TypeAdapter<UnsignedUUID> {
        @Override
        public void write(JsonWriter out, UnsignedUUID value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public UnsignedUUID read(JsonReader in) throws IOException {
            return parse(in.nextString());
        }
    }
}
