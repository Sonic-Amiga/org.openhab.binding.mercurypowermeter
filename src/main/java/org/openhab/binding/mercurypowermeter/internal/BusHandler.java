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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.openhab.binding.mercurypowermeter.internal.dto.M200Protocol;
import org.openhab.binding.mercurypowermeter.internal.dto.M200Protocol.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BusHandler} is a handy base class, implementing data communication with Herzborg devices.
 *
 * @author Pavel Fedin - Initial contribution
 */
@NonNullByDefault
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

        int readLength;

        // Reply length depends on the command
        switch (pkt.getCommand()) {
            case M200Protocol.Command.READ_TIME:
                readLength = 7;
                break;
            case M200Protocol.Command.READ_POWER:
                readLength = 4;
                break;
            case M200Protocol.Command.READ_COUNTERS:
                readLength = 16;
                break;
            case M200Protocol.Command.READ_BATTERY:
                readLength = 2;
                break;
            case M200Protocol.Command.READ_TARIFFS:
                readLength = 1;
                break;
            case M200Protocol.Command.READ_TARIFF:
                readLength = 1;
                break;
            case M200Protocol.Command.READ_UIP:
                readLength = 7;
                break;
            case M200Protocol.Command.READ_LINE_PARAMS:
                readLength = 10;
                break;
            default:
                throw new IllegalStateException("Unknown command code");
        }

        logger.trace("Sending command {}; reply data length = {}", Byte.toUnsignedInt(pkt.getCommand()), readLength);

        readLength += Packet.MIN_LENGTH;

        dataOut.write(pkt.getBuffer());

        int readOffset = 0;
        byte[] readBuffer = new byte[readLength];

        while (readLength > 0) {
            int n = dataIn.read(readBuffer, readOffset, readLength);

            if (n < 0) {
                throw new IOException("EOF from serial port");
            } else if (n == 0) {
                logger.trace("Reply timeout");
                throw new IOException("Serial read timeout");
            }

            readOffset += n;
            readLength -= n;
        }

        return new Packet(readBuffer);
    }
}
