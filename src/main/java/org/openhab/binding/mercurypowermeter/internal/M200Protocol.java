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
            m_Buffer.order(ByteOrder.LITTLE_ENDIAN);
            m_DataLength = data.length - 2;
        }

        Packet(int address, byte command) {
            m_Buffer = ByteBuffer.allocate(MIN_LENGTH);
            m_Buffer.order(ByteOrder.LITTLE_ENDIAN);
            m_DataLength = MIN_LENGTH - 2;

            m_Buffer.putInt(address);
            m_Buffer.put(command);
            m_Buffer.putShort(calculate_crc(m_DataLength));
        }

        public byte[] getBuffer() {
            return m_Buffer.array();
        }

        public boolean isValid() {
            return calculate_crc(m_DataLength) == m_Buffer.getShort(m_DataLength);
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

        private short calculate_crc(int length) {
            int crc_value = 0;

            for (int len = 0; len < length; len++) {
                for (int i = 0x80; i != 0; i >>= 1) {
                    if ((crc_value & 0x8000) != 0) {
                        crc_value = (crc_value << 1) ^ 0x8005;
                    } else {
                        crc_value = crc_value << 1;
                    }
                    if ((m_Buffer.get(len) & i) != 0) {
                        crc_value ^= 0x8005;
                    }
                }
            }
            return (short) crc_value;
        }
    }
}
