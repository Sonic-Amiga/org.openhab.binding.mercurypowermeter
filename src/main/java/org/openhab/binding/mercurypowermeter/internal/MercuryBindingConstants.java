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
    public static final String CH_COUNT[] = { "count1", "count2", "count3", "count4" };
}
