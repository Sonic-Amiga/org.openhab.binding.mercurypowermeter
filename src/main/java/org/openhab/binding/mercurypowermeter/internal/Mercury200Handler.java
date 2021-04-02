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

import static org.openhab.binding.mercurypowermeter.internal.MercuryBindingConstants.*;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.mercurypowermeter.internal.dto.M200Protocol;
import org.openhab.binding.mercurypowermeter.internal.dto.M200Protocol.Packet;
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
    private final Logger logger = LoggerFactory.getLogger(Mercury200Handler.class);
    private MercuryConfiguration config = new MercuryConfiguration();
    private @Nullable ScheduledFuture<?> pollFuture;
    private @Nullable SerialBusHandler bus;

    public Mercury200Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO
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

    private void poll() {
        try {
            boolean ok = true;

            // Our serial bus is slow (9600 bps max), so we are polling only for used channels
            if (isLinked(CH_DATETIME)) {
                Packet reply = doPacket(M200Protocol.Command.READ_TIME);

                if (reply != null) {
                    updateState(CH_DATETIME, new DateTimeType(reply.getDateTime()));
                } else {
                    ok = false;
                }
            }
            if (isLinked(CH_ENERGY1) || isLinked(CH_ENERGY2) || isLinked(CH_ENERGY3) || isLinked(CH_ENERGY4)) {
                Packet reply = doPacket(M200Protocol.Command.READ_COUNTERS);

                if (reply != null) {
                    // Reply contains four 32-bit BCD values, unit is tenth of Wt*H.
                    // Report it as KWt*H for simplicity and usability
                    for (int i = 0; i < CH_ENERGY.length; i++) {
                        double kwt_h = Util.BCDToInt(reply.getInt(i * 4)) * 0.01;
                        updateState(CH_ENERGY[i], new DecimalType(kwt_h));
                    }
                } else {
                    ok = false;
                }
            }
            if (isLinked(CH_BATTERY)) {
                Packet reply = doPacket(M200Protocol.Command.READ_BATTERY);

                if (reply != null) {
                    // Reply contains 16-bit BCD value in format VV.VV
                    double volts = Util.BCDToInt(reply.getShort(0)) * 0.01;
                    updateState(CH_BATTERY, new DecimalType(volts));
                } else {
                    ok = false;
                }
            }
            if (isLinked(CH_NUM_TARIFFS)) {
                Packet reply = doPacket(M200Protocol.Command.READ_TARIFFS);

                if (reply != null) {
                    // One byte - number of tariffs
                    updateState(CH_NUM_TARIFFS, new DecimalType(reply.getByte(0)));
                } else {
                    ok = false;
                }
            }
            if (isLinked(CH_TARIFF)) {
                Packet reply = doPacket(M200Protocol.Command.READ_TARIFF);

                if (reply != null) {
                    // One byte - number of current tariff starting from 0
                    updateState(CH_TARIFF, new DecimalType(reply.getByte(0) + 1));
                } else {
                    ok = false;
                }
            }
            if (isLinked(CH_U) || isLinked(CH_I) || isLinked(CH_P)) {
                Packet reply = doPacket(M200Protocol.Command.READ_UIP);

                if (reply != null) {
                    // 2 bytes - BCD voltage
                    // 2 bytes - BCD current
                    // 3 bytes - BCD power
                    // Multipliers are obtained experimentally by comparing values with
                    // ones reported by official Configurator software.
                    // Thanks Incotex for so crappy protocol doc!
                    updateState(CH_U, new DecimalType(Util.BCDToInt(reply.getShort(0)) * 0.1));
                    updateState(CH_I, new DecimalType(Util.BCDToInt(reply.getShort(2)) * 0.01));
                    updateState(CH_P, new DecimalType(Util.BCDToInt(reply.getTriple(4)) * 0.001));
                } else {
                    ok = false;
                }
            }

            if (ok) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Invalid response received");
            }
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (BridgeOfflineException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    private @Nullable Packet doPacket(byte command) throws IOException, BridgeOfflineException {
        SerialBusHandler bus = this.bus;

        if (bus == null) {
            // This is an impossible situation but Eclipse forces us to handle it
            throw new IllegalStateException("No Bridge while polling");
        }

        Packet pkt = new Packet(config.address, command);
        return bus.doPacket(pkt);
    }
}
