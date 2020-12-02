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

@NonNullByDefault
public class Util {
    public static int BCDToInt(int bcd) {
        int result = 0;
        int multiplier = 1;

        for (int i = 0; i < 8; i++) {
            int digit = (bcd >> (i * 4)) & 0x0F;
            result += digit * multiplier;
            multiplier *= 10;
        }

        return result;
    }
}
