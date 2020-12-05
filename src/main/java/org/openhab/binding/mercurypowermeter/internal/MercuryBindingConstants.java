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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link MercuryBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Pavel Fedin - Initial contribution
 */
@NonNullByDefault
public class MercuryBindingConstants {
    private static final String BINDING_ID = "mercurypowermeter";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_M200 = new ThingTypeUID(BINDING_ID, "mercury200");
    public static final ThingTypeUID THING_TYPE_SERIAL = new ThingTypeUID(BINDING_ID, "serial_bus");

    // List of all Channel ids
    public static final String CH_ENERGY1 = "energy1";
    public static final String CH_ENERGY2 = "energy2";
    public static final String CH_ENERGY3 = "energy3";
    public static final String CH_ENERGY4 = "energy4";
    public static final String CH_ENERGY[] = { CH_ENERGY1, CH_ENERGY2, CH_ENERGY3, CH_ENERGY4 };
    public static final String CH_BATTERY = "battery";
    public static final String CH_NUM_TARIFFS = "num_tariffs";
    public static final String CH_TARIFF = "tariff";
    public static final String CH_U = "voltage";
    public static final String CH_I = "current";
    public static final String CH_P = "power";
}
