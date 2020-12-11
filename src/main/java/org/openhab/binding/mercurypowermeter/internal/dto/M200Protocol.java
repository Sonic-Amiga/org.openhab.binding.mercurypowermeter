/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mercurypowermeter.internal.dto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mercurypowermeter.internal.Util;

/**
 * Mercury 20x binary protocol
 *
 * @author Pavel Fedin - Initial contribution
 *
 */
@NonNullByDefault
public class M200Protocol {
    public static class Command {
        public static final byte READ_TIME = 0x21;
        public static final byte READ_POWER = 0x26;
        public static final byte READ_COUNTERS = 0x27;
        public static final byte READ_BATTERY = 0x29;
        public static final byte READ_TARIFFS = 0x2E;
        public static final byte READ_TARIFF = 0x60;
        public static final byte READ_UIP = 0x63;
        public static final byte READ_LINE_PARAMS = (byte) 0x81;
    }

    public static class Packet {
        private static final int HEADER_LENGTH = 5;
        public static final int MIN_LENGTH = HEADER_LENGTH + 2;

        private ByteBuffer dataBuffer;
        private int dataLength;

        public Packet(byte[] data) {
            dataBuffer = ByteBuffer.wrap(data);
            dataBuffer.order(ByteOrder.BIG_ENDIAN);
            dataLength = data.length - 2;
        }

        public Packet(int address, byte command) {
            dataBuffer = ByteBuffer.allocate(MIN_LENGTH);
            dataBuffer.order(ByteOrder.BIG_ENDIAN);
            dataLength = HEADER_LENGTH;

            dataBuffer.putInt(address);
            dataBuffer.put(command);
            dataBuffer.putShort(crc16(dataLength));
        }

        public byte[] getBuffer() {
            return dataBuffer.array();
        }

        public boolean isValid() {
            return crc16(dataLength) == dataBuffer.getShort(dataLength);
        }

        public int getAddress() {
            return dataBuffer.getInt(0);
        }

        public byte getCommand() {
            return dataBuffer.get(4);
        }

        public byte getByte(int offset) {
            return dataBuffer.get(HEADER_LENGTH + offset);
        }

        public short getShort(int offset) {
            return dataBuffer.getShort(HEADER_LENGTH + offset);
        }

        public int getTriple(int offset) {
            int v1 = getShort(offset);
            int v2 = getByte(offset + 2);

            return v1 | (v2 << 16);
        }

        public int getInt(int offset) {
            return dataBuffer.getInt(HEADER_LENGTH + offset);
        }

        public ZonedDateTime getDateTime() {
            // 0th byte is day of week, ignore it
            int hh = Util.BCDToInt(dataBuffer.get(HEADER_LENGTH + 1));
            int mm = Util.BCDToInt(dataBuffer.get(HEADER_LENGTH + 2));
            int ss = Util.BCDToInt(dataBuffer.get(HEADER_LENGTH + 3));
            int dd = Util.BCDToInt(dataBuffer.get(HEADER_LENGTH + 4));
            int mon = Util.BCDToInt(dataBuffer.get(HEADER_LENGTH + 5)) - 1;
            int yy = Util.BCDToInt(dataBuffer.get(HEADER_LENGTH + 6)) + 2000;

            return ZonedDateTime.of(yy, mon, dd, hh, mm, ss, 0, ZoneId.systemDefault());
        }

        // Mercury uses modbus variant of CRC16
        // Code adapted from https://habr.com/ru/post/418209/
        private short crc16(int length) {
            int crc = 0xFFFF;
            for (int i = 0; i < length; i++) {
                crc = crc ^ Byte.toUnsignedInt(dataBuffer.get(i));
                for (int j = 0; j < 8; j++) {
                    int mask = ((crc & 0x1) != 0) ? 0xA001 : 0x0000;
                    crc = ((crc >> 1) & 0x7FFF) ^ mask;
                }
            }
            // Our buffer is bigendian, but apparently CRC is little, make up for that
            return Short.reverseBytes((short) crc);
        }
    }
}
