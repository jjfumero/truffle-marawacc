/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.hotspot.replacements;

import static com.oracle.graal.hotspot.replacements.HotSpotReplacementsUtil.*;
import static com.oracle.graal.nodes.PiNode.*;

import com.oracle.graal.api.replacements.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.hotspot.word.*;
import com.oracle.graal.word.*;

@ClassSubstitution(Node.class)
public class HotSpotNodeSubstitutions {

    /**
     * Gets the value of the {@code InstanceKlass::_graal_node_class} field from the InstanceKlass
     * pointed to by {@code node}'s header.
     */
    @MethodSubstitution(isStatic = false)
    public static NodeClass<?> getNodeClass(final Node node) {
        // HotSpot creates the NodeClass for each Node subclass while initializing it
        // so we are guaranteed to read a non-null value here. As long as NodeClass
        // is final, the stamp of the PiNode below will automatically be exact.
        KlassPointer klass = loadHub(node);
        return piCastNonNull(klass.readObject(Word.signed(instanceKlassNodeClassOffset()), KLASS_NODE_CLASS), NodeClass.class);
    }
}
