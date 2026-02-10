/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC
 * http://www.gov4j.it/govpay
 *
 * Copyright (c) 2014-2025 Link.it srl (http://www.link.it).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3, as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.govpay.common.utils;

import java.util.Map;
import java.util.Optional;

/**
 * Utility per la lettura tipizzata dei valori da una mappa connettore
 * restituita da {@code ConnettoreService.getConnettoreAsMap()}.
 */
public final class ConnettoreMapUtils {

    private ConnettoreMapUtils() {}

    public static Optional<String> getString(Map<String, String> map, String key) {
        return Optional.ofNullable(map.get(key));
    }

    public static String getString(Map<String, String> map, String key, String defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    public static Optional<Boolean> getBoolean(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val == null) {
            return Optional.empty();
        }
        return Optional.of(Boolean.parseBoolean(val));
    }

    public static boolean getBoolean(Map<String, String> map, String key, boolean defaultValue) {
        String val = map.get(key);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

    public static Optional<Integer> getInteger(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(val.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static int getInteger(Map<String, String> map, String key, int defaultValue) {
        return getInteger(map, key).orElse(defaultValue);
    }

    public static Optional<Long> getLong(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(val.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static long getLong(Map<String, String> map, String key, long defaultValue) {
        return getLong(map, key).orElse(defaultValue);
    }

    public static Optional<Double> getDouble(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(val.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static double getDouble(Map<String, String> map, String key, double defaultValue) {
        return getDouble(map, key).orElse(defaultValue);
    }
}
