/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.nodes.extended;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

/**
 * Loads a method from the virtual method table of a given hub.
 */
public final class LoadMethodNode extends FixedWithNextNode implements Lowerable {

    @Input private ValueNode hub;
    private final ResolvedJavaMethod method;
    private final ResolvedJavaType receiverType;

    public ValueNode getHub() {
        return hub;
    }

    public LoadMethodNode(ResolvedJavaMethod method, ResolvedJavaType receiverType, ValueNode hub, Kind kind) {
        super(kind == Kind.Object ? StampFactory.objectNonNull() : StampFactory.forKind(kind));
        this.receiverType = receiverType;
        this.hub = hub;
        this.method = method;
        assert !method.isAbstract() : "Cannot load abstract method from a hub";
        assert !method.isStatic() : "Cannot load a static method from a hub";
        assert method.isInVirtualMethodTable(receiverType);
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    public ResolvedJavaMethod getMethod() {
        return method;
    }

    public ResolvedJavaType getReceiverType() {
        return receiverType;
    }
}