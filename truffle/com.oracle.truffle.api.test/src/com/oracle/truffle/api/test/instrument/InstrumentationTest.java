/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.api.test.instrument;

import static com.oracle.truffle.api.test.instrument.InstrumentationTestingLanguage.ADD_TAG;
import static com.oracle.truffle.api.test.instrument.InstrumentationTestingLanguage.VALUE_TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.Test;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.ASTProber;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.instrument.Instrumenter;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeListener;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.instrument.SimpleInstrumentListener;
import com.oracle.truffle.api.instrument.StandardInstrumentListener;
import com.oracle.truffle.api.instrument.SyntaxTag;
import com.oracle.truffle.api.instrument.impl.DefaultProbeListener;
import com.oracle.truffle.api.instrument.impl.DefaultSimpleInstrumentListener;
import com.oracle.truffle.api.instrument.impl.DefaultStandardInstrumentListener;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.test.instrument.InstrumentationTestNodes.TestAdditionNode;
import com.oracle.truffle.api.test.instrument.InstrumentationTestNodes.TestLanguageNode;
import com.oracle.truffle.api.test.instrument.InstrumentationTestNodes.TestValueNode;
import com.oracle.truffle.api.vm.TruffleVM;

/**
 * <h3>AST Instrumentation</h3>
 *
 * Instrumentation allows the insertion into Truffle ASTs language-specific instances of
 * {@link WrapperNode} that propagate execution events through a {@link Probe} to any instances of
 * {@link Instrument} that might be attached to the particular probe by tools.
 * <ol>
 * <li>Creates a simple add AST</li>
 * <li>Verifies its structure</li>
 * <li>"Probes" the add node by adding a {@link WrapperNode} and associated {@link Probe}</li>
 * <li>Attaches a simple {@link Instrument} to the node via the Probe's {@link ProbeNode}</li>
 * <li>Verifies the structure of the probed AST</li>
 * <li>Verifies the execution of the probed AST</li>
 * <li>Verifies the results observed by the instrument.</li>
 * </ol>
 * To do these tests, several required classes have been implemented in their most basic form, only
 * implementing the methods necessary for the tests to pass, with stubs elsewhere.
 */
public class InstrumentationTest {

    @Test
    public void testProbing() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
        final TruffleVM vm = TruffleVM.newVM().build();
        final Field field = TruffleVM.class.getDeclaredField("instrumenter");
        field.setAccessible(true);
        final Instrumenter instrumenter = (Instrumenter) field.get(vm);
        final Source source = Source.fromText("testProbing text", "testProbing").withMimeType("text/x-instTest");

        final Probe[] probes = new Probe[3];
        instrumenter.addProbeListener(new ProbeListener() {

            public void startASTProbing(Source s) {
            }

            public void newProbeInserted(Probe probe) {
            }

            public void probeTaggedAs(Probe probe, SyntaxTag tag, Object tagValue) {
                if (tag == ADD_TAG) {
                    assertEquals(probes[0], null);
                    probes[0] = probe;
                } else if (tag == VALUE_TAG) {
                    if (probes[1] == null) {
                        probes[1] = probe;
                    } else if (probes[2] == null) {
                        probes[2] = probe;
                    } else {
                        fail("Should only be three probes");
                    }
                }
            }

            public void endASTProbing(Source s) {
            }

        });
        assertEquals(vm.eval(source).get(), 13);
        assertNotNull("Add node should be probed", probes[0]);
        assertNotNull("Value nodes should be probed", probes[1]);
        assertNotNull("Value nodes should be probed", probes[2]);
        // Check instrumentation with the simplest kind of counters.
        // They should all be removed when the check is finished.
        checkCounters(probes[0], vm, source, new TestSimpleInstrumentCounter(), new TestSimpleInstrumentCounter(), new TestSimpleInstrumentCounter());

        // Now try with the more complex flavor of listener
        checkCounters(probes[0], vm, source, new TestStandardInstrumentCounter(), new TestStandardInstrumentCounter(), new TestStandardInstrumentCounter());

    }

    @Test
    public void testTagging() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {

        final TruffleVM vm = TruffleVM.newVM().build();
        final Field field = TruffleVM.class.getDeclaredField("instrumenter");
        field.setAccessible(true);
        final Instrumenter instrumenter = (Instrumenter) field.get(vm);
        final Source source = Source.fromText("testTagging text", "testTagging").withMimeType("text/x-instTest");

        // Applies appropriate tags
        final TestASTProber astProber = new TestASTProber(instrumenter);
        instrumenter.registerASTProber(astProber);

        // Listens for probes and tags being added
        final TestProbeListener probeListener = new TestProbeListener();
        instrumenter.addProbeListener(probeListener);

        assertEquals(13, vm.eval(source).get());

        // Check that the prober added probes to the tree
        assertEquals(probeListener.probeCount, 3);
        assertEquals(probeListener.tagCount, 3);

        assertEquals(instrumenter.findProbesTaggedAs(InstrumentationTestingLanguage.ADD_TAG).size(), 1);
        assertEquals(instrumenter.findProbesTaggedAs(VALUE_TAG).size(), 2);
    }

    private static void checkCounters(Probe probe, TruffleVM vm, Source source, TestCounter counterA, TestCounter counterB, TestCounter counterC) throws IOException {

        // Attach a counting instrument to the probe
        counterA.attach(probe);

        // Attach a second counting instrument to the probe
        counterB.attach(probe);

        // Run it again and check that the two instruments are working
        assertEquals(13, vm.eval(source).get());
        assertEquals(counterA.enterCount(), 1);
        assertEquals(counterA.leaveCount(), 1);
        assertEquals(counterB.enterCount(), 1);
        assertEquals(counterB.leaveCount(), 1);

        // Remove counterA
        counterA.dispose();

        // Run it again and check that instrument B is still working but not A
        assertEquals(13, vm.eval(source).get());
        assertEquals(counterA.enterCount(), 1);
        assertEquals(counterA.leaveCount(), 1);
        assertEquals(counterB.enterCount(), 2);
        assertEquals(counterB.leaveCount(), 2);

        // Attach a second instrument to the probe
        counterC.attach(probe);

        // Run the original and check that instruments B,C working but not A
        assertEquals(13, vm.eval(source).get());
        assertEquals(counterA.enterCount(), 1);
        assertEquals(counterA.leaveCount(), 1);
        assertEquals(counterB.enterCount(), 3);
        assertEquals(counterB.leaveCount(), 3);
        assertEquals(counterC.enterCount(), 1);
        assertEquals(counterC.leaveCount(), 1);

        // Remove instrumentC
        counterC.dispose();

        // Run the original and check that instrument B working but not A,C
        assertEquals(13, vm.eval(source).get());
        assertEquals(counterA.enterCount(), 1);
        assertEquals(counterA.leaveCount(), 1);
        assertEquals(counterB.enterCount(), 4);
        assertEquals(counterB.leaveCount(), 4);
        assertEquals(counterC.enterCount(), 1);
        assertEquals(counterC.leaveCount(), 1);

        // Remove instrumentB
        counterB.dispose();

        // Check that no instruments working
        assertEquals(13, vm.eval(source).get());
        assertEquals(counterA.enterCount(), 1);
        assertEquals(counterA.leaveCount(), 1);
        assertEquals(counterB.enterCount(), 4);
        assertEquals(counterB.leaveCount(), 4);
        assertEquals(counterC.enterCount(), 1);
        assertEquals(counterC.leaveCount(), 1);
    }

    private interface TestCounter {

        int enterCount();

        int leaveCount();

        void attach(Probe probe);

        void dispose();
    }

    /**
     * A counter for the number of times execution enters and leaves a probed AST node.
     */
    private class TestSimpleInstrumentCounter implements TestCounter {

        public int enterCount = 0;
        public int leaveCount = 0;
        public final Instrument instrument;

        public TestSimpleInstrumentCounter() {
            this.instrument = Instrument.create(new SimpleInstrumentListener() {

                public void enter(Probe probe) {
                    enterCount++;
                }

                public void returnVoid(Probe probe) {
                    leaveCount++;
                }

                public void returnValue(Probe probe, Object result) {
                    leaveCount++;
                }

                public void returnExceptional(Probe probe, Exception exception) {
                    leaveCount++;
                }

            }, "Instrumentation Test Counter");
        }

        @Override
        public int enterCount() {
            return enterCount;
        }

        @Override
        public int leaveCount() {
            return leaveCount;
        }

        @Override
        public void attach(Probe probe) {
            probe.attach(instrument);
        }

        @Override
        public void dispose() {
            instrument.dispose();
        }
    }

    /**
     * A counter for the number of times execution enters and leaves a probed AST node.
     */
    private class TestStandardInstrumentCounter implements TestCounter {

        public int enterCount = 0;
        public int leaveCount = 0;
        public final Instrument instrument;

        public TestStandardInstrumentCounter() {
            this.instrument = Instrument.create(new StandardInstrumentListener() {

                public void enter(Probe probe, Node node, VirtualFrame vFrame) {
                    enterCount++;
                }

                public void returnVoid(Probe probe, Node node, VirtualFrame vFrame) {
                    leaveCount++;
                }

                public void returnValue(Probe probe, Node node, VirtualFrame vFrame, Object result) {
                    leaveCount++;
                }

                public void returnExceptional(Probe probe, Node node, VirtualFrame vFrame, Exception exception) {
                    leaveCount++;
                }

            }, "Instrumentation Test Counter");
        }

        @Override
        public int enterCount() {
            return enterCount;
        }

        @Override
        public int leaveCount() {
            return leaveCount;
        }

        @Override
        public void attach(Probe probe) {
            probe.attach(instrument);
        }

        @Override
        public void dispose() {
            instrument.dispose();
        }
    }

    /**
     * Tags selected nodes on newly constructed ASTs.
     */
    private static final class TestASTProber implements NodeVisitor, ASTProber {

        private final Instrumenter instrumenter;

        TestASTProber(Instrumenter instrumenter) {
            this.instrumenter = instrumenter;
        }

        @Override
        public boolean visit(Node node) {
            if (node instanceof TestLanguageNode) {

                final TestLanguageNode testNode = (TestLanguageNode) node;

                if (node instanceof TestValueNode) {
                    instrumenter.probe(testNode).tagAs(VALUE_TAG, null);

                } else if (node instanceof TestAdditionNode) {
                    instrumenter.probe(testNode).tagAs(ADD_TAG, null);

                }
            }
            return true;
        }

        @Override
        public void probeAST(Instrumenter inst, Node node) {
            node.accept(this);
        }
    }

    /**
     * Counts the number of "enter" events at probed nodes using the simplest AST listener.
     */
    static final class TestSimpleInstrumentListener extends DefaultSimpleInstrumentListener {

        public int counter = 0;

        @Override
        public void enter(Probe probe) {
            counter++;
        }
    }

    /**
     * Counts the number of "enter" events at probed nodes using the AST listener.
     */
    static final class TestASTInstrumentListener extends DefaultStandardInstrumentListener {

        public int counter = 0;

        @Override
        public void enter(Probe probe, Node node, VirtualFrame vFrame) {
            counter++;
        }
    }

    private static final class TestProbeListener extends DefaultProbeListener {

        public int probeCount = 0;
        public int tagCount = 0;

        @Override
        public void newProbeInserted(Probe probe) {
            probeCount++;
        }

        @Override
        public void probeTaggedAs(Probe probe, SyntaxTag tag, Object tagValue) {
            tagCount++;
        }
    }
}
