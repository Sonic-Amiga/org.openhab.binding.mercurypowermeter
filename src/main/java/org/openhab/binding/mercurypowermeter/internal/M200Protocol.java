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
package org.openhab.binding.mercurypowermeter.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class M200Protocol {
    public static class Command {
        public static final byte READ_POWER = 0x26;
        public static final byte READ_COUNTERS = 0x27;
        public static final byte READ_TARIFFS = 0x2E;
        public static final byte READ_UIP = 0x63;
    }

    public static class Packet {
        public static final int MIN_LENGTH = 7;

        private ByteBuffer m_Buffer;
        private int m_DataLength;

        public Packet(byte[] data) {
            m_Buffer = ByteBuffer.wrap(data);
            m_Buffer.order(ByteOrder.BIG_ENDIAN);
            m_DataLength = data.length - 2;
        }

        Packet(int address, byte command) {
            m_Buffer = ByteBuffer.allocate(MIN_LENGTH);
            m_Buffer.order(ByteOrder.BIG_ENDIAN);
            m_DataLength = MIN_LENGTH - 2;

            m_Buffer.putInt(address);
            m_Buffer.put(command);
            m_Buffer.putShort(crc16(m_DataLength));
        }

        public byte[] getBuffer() {
            return m_Buffer.array();
        }

        public boolean isValid() {
            return crc16(m_DataLength) == m_Buffer.getShort(m_DataLength);
        }

        public int getAddress() {
            return m_Buffer.getInt(0);
        }

        public int getCommand() {
            return m_Buffer.get(4);
        }

        public int getInt(int offset) {
            return m_Buffer.getInt(5 + offset);
        }

        // Mercury uses modbus variant of CRC16
        // Code adapted from https://habr.com/ru/post/418209/
        private short crc16(int length) {
            int crc = 0xFFFF;
            for (int i = 0; i < length; i++) {
                crc = crc ^ Byte.toUnsignedInt(m_Buffer.get(i));
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
