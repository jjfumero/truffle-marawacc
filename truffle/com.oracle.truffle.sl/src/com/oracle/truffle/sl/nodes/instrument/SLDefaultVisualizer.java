/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.sl.nodes.instrument;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.instrument.ASTPrinter;
import com.oracle.truffle.api.instrument.impl.DefaultVisualizer;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.sl.nodes.SLRootNode;
import com.oracle.truffle.sl.runtime.SLNull;

/**
 * SLDefaultVisualizer provides methods to get the names of SL's internal Truffle AST nodes.
 *
 */
public class SLDefaultVisualizer extends DefaultVisualizer {

    private final SLASTPrinter astPrinter;

    public SLDefaultVisualizer() {
        this.astPrinter = new SLASTPrinter();
    }

    @Override
    public ASTPrinter getASTPrinter() {
        return astPrinter;
    }

    @Override
    public String displayMethodName(Node node) {

        if (node == null) {
            return null;
        }
        RootNode root = node.getRootNode();
        if (root instanceof SLRootNode) {
            SLRootNode slRootNode = (SLRootNode) root;
            return slRootNode.getName();

        }
        return "unknown";
    }

    @Override
    public String displayCallTargetName(CallTarget callTarget) {
        if (callTarget instanceof RootCallTarget) {
            final RootCallTarget rootCallTarget = (RootCallTarget) callTarget;
            SLRootNode slRootNode = (SLRootNode) rootCallTarget.getRootNode();
            return slRootNode.toString();
        }
        return callTarget.toString();
    }

    @Override
    public String displayValue(Object value, int trim) {
        if (value == null || value == SLNull.SINGLETON) {
            return "null";
        }
        return trim(value.toString(), trim);
    }

    @Override
    public String displayIdentifier(FrameSlot slot) {

        final Object id = slot.getIdentifier();
        return id.toString();
    }
}
