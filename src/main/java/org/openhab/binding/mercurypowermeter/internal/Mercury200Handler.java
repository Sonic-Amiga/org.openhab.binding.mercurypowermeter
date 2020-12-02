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

import static org.openhab.binding.mercurypowermeter.internal.MercuryBindingConstants.CH_COUNT;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.mercurypowermeter.internal.M200Protocol.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Mercury200Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Pavel Fedin - Initial contribution
 */
@NonNullByDefault
public class Mercury200Handler extends BaseThingHandler {
    private static final Set<String> CH_COUNT_SET = new HashSet<>(Arrays.asList(CH_COUNT));
    private final Logger logger = LoggerFactory.getLogger(Mercury200Handler.class);

    private static enum DataItem {

        COUNTER(M200Protocol.Command.READ_COUNTERS);

        private byte command;

        DataItem(byte command) {
            this.command = command;
        }

        public byte getCommand() {
            return command;
        }
    }

    private MercuryConfiguration config = new MercuryConfiguration();
    private @Nullable ScheduledFuture<?> pollFuture;
    private @Nullable SerialBusHandler bus;
    private Set<DataItem> pollSet = EnumSet.noneOf(DataItem.class);

    public Mercury200Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String ch = channelUID.getId();

        if (command instanceof RefreshType) {
            if (CH_COUNT_SET.contains(ch)) {
                addPolledItem(DataItem.COUNTER);
            }
        }
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();

        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, "Bridge not present");
            return;
        }

        BridgeHandler handler = bridge.getHandler();

        if (handler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, "Bridge has no handler");
            return;
        }

        bus = (SerialBusHandler) handler;
        config = getConfigAs(MercuryConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);
        logger.trace("Successfully initialized, starting poll");
        pollFuture = scheduler.scheduleWithFixedDelay(this::poll, 1, config.poll_interval, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        stopPoll();
    }

    private void stopPoll() {
        if (pollFuture != null) {
            pollFuture.cancel(true);
            pollFuture = null;
        }
    }

    private void addPolledItem(DataItem item) {
        logger.debug("Adding polling for {}", item);
        synchronized (pollSet) {
            pollSet.add(item);
        }
    }

    private synchronized void poll() {
        SerialBusHandler bus = this.bus;

        if (bus == null) {
            // This is an impossible situation but Eclipse forces us to handle it
            logger.warn("No Bridge while polling");
            stopPoll();
            return;
        }

        try {
            boolean ok = true;
            Set<DataItem> localSet;

            synchronized (pollSet) {
                if (pollSet.isEmpty()) {
                    return;
                }
                localSet = EnumSet.copyOf(pollSet);
            }

            for (DataItem item : localSet) {
                Packet pkt = new Packet(config.address, item.getCommand());
                Packet reply = bus.doPacket(pkt);

                if (reply == null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                    return;
                }

                ok = reply.isValid();

                if (ok) {
                    switch (reply.getCommand()) {
                        case M200Protocol.Command.READ_COUNTERS:
                            // Reply contains four 32-bit BCD values, unit is tenth of wt*h.
                            // Report it as Kwt*h for simplicity and usability
                            for (int i = 0; i < CH_COUNT.length; i++) {
                                double kwt_h = Util.BCDToInt(reply.getInt(i * 4)) * 0.01;
                                updateState(CH_COUNT[i], new DecimalType(kwt_h));
                            }
                            break;
                    }
                } else {
                    logger.warn("Invalid reply received: {}", DatatypeConverter.printHexBinary(reply.getBuffer()));
                }
            }

            if (ok) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Invalid response received");
            }
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }

    }
}
