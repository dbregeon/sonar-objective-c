/*
 * Sonar Objective-C Plugin
 * Copyright (C) 2012 François Helg, Cyril Picat and OCTO Technology
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.objectivec.preprocessor;

import java.util.HashMap;
import java.util.Map;

public final class MapChain<K, V> {
    private final Map<K, V> highPrioMap = new HashMap<K, V>();
    private final Map<K, V> lowPrioMap = new HashMap<K, V>();
    private final Map<K, V> highPrioDisabled = new HashMap<K, V>();
    private final Map<K, V> lowPrioDisabled = new HashMap<K, V>();

    public V get(final Object key) {
        final V value = highPrioMap.get(key);
        return value != null ? value : lowPrioMap.get(key);
    }

    public V putHighPrio(final K key, final V value) {
        return highPrioMap.put(key, value);
    }

    public V putLowPrio(final K key, final V value) {
        return lowPrioMap.put(key, value);
    }

    public V removeLowPrio(final K key) {
        return lowPrioMap.remove(key);
    }

    public void clearLowPrio() {
        lowPrioMap.clear();
    }

    public void disable(final K key) {
        move(key, lowPrioMap, lowPrioDisabled);
        move(key, highPrioMap, highPrioDisabled);
    }

    public void enable(final K key) {
        move(key, lowPrioDisabled, lowPrioMap);
        move(key, highPrioDisabled, highPrioMap);
    }

    private void move(final K key, final Map<K, V> from, final Map<K, V> to) {
        final V value = from.remove(key);
        if (value != null) {
            to.put(key, value);
        }
    }
}
