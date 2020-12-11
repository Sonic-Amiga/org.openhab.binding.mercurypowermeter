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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.openhab.binding.mercurypowermeter.internal.dto.M200Protocol;
import org.openhab.binding.mercurypowermeter.internal.dto.M200Protocol.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BusHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(BusHandler.class);

    protected @Nullable InputStream dataIn;
    protected @Nullable OutputStream dataOut;

    public BusHandler(Bridge bridge) {
        super(bridge);
    }

    private void safeClose(@Nullable Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                logger.warn("Error closing I/O stream: {}", e.getMessage());
            }
        }
    }

    @Override
    public void dispose() {
        safeClose(dataOut);
        safeClose(dataIn);

        dataOut = null;
        dataIn = null;
    }

    public synchronized @Nullable Packet doPacket(Packet pkt) throws IOException {
        OutputStream dataOut = this.dataOut;
        InputStream dataIn = this.dataIn;

        if (dataOut == null || dataIn == null) {
            return null;
        }

        int read_length;

        // Reply length depends on the command
        switch (pkt.getCommand()) {
            case M200Protocol.Command.READ_TIME:
                read_length = 7;
                break;
            case M200Protocol.Command.READ_POWER:
                read_length = 4;
                break;
            case M200Protocol.Command.READ_COUNTERS:
                read_length = 16;
                break;
            case M200Protocol.Command.READ_BATTERY:
                read_length = 2;
                break;
            case M200Protocol.Command.READ_TARIFFS:
                read_length = 1;
                break;
            case M200Protocol.Command.READ_TARIFF:
                read_length = 1;
                break;
            case M200Protocol.Command.READ_UIP:
                read_length = 7;
                break;
            case M200Protocol.Command.READ_LINE_PARAMS:
                read_length = 10;
                break;
            default:
                throw new IllegalStateException("Unknown command code");
        }

        logger.trace("Sending command {}; reply data length = {}", Byte.toUnsignedInt(pkt.getCommand()), read_length);

        read_length += Packet.MIN_LENGTH;

        dataOut.write(pkt.getBuffer());

        int read_offset = 0;
        byte[] in_buffer = new byte[read_length];

        while (read_length > 0) {
            int n = dataIn.read(in_buffer, read_offset, read_length);

            if (n < 0) {
                throw new IOException("EOF from serial port");
            } else if (n == 0) {
                logger.trace("Reply timeout");
                throw new IOException("Serial read timeout");
            }

            read_offset += n;
            read_length -= n;
        }

        return new Packet(in_buffer);
    }
}
