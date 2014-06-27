/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_GRAAL_GRAAL_RUNTIME_HPP
#define SHARE_VM_GRAAL_GRAAL_RUNTIME_HPP

#include "interpreter/interpreter.hpp"
#include "memory/allocation.hpp"
#include "runtime/deoptimization.hpp"

class GraalRuntime: public CHeapObj<mtCompiler> {
 private:

  static jobject _HotSpotGraalRuntime_instance;
  static address _external_deopt_i2c_entry;

  /**
   * Reads the OptionValue object from a specified static field.
   *
   * @throws LinkageError if the field could not be resolved
   */
  static Handle get_OptionValue(const char* declaringClass, const char* fieldName, const char* fieldSig, TRAPS);

  /**
   * Parses the string form of a numeric, float or double option into a jlong (using raw bits for floats/doubles).
   *
   * @param spec 'i', 'f' or 'd' (see HotSpotOptions.setOption())
   * @param name option name
   * @param name_len length of option name
   * @param value string value to parse
   * @throws InternalError if value could not be parsed according to spec
   */
  static jlong parse_primitive_option_value(char spec, const char* name, int name_len, const char* value, TRAPS);

  /**
   * Loads default option value overrides from a <jre_home>/lib/graal.options if it exists. Each
   * line in this file must have the format of a Graal command line option without the
   * leading "-G:" prefix. These option values are set prior to processing of any Graal
   * options present on the command line.
   */
  static void parse_graal_options_file(KlassHandle hotSpotOptionsClass, TRAPS);

  /**
   * Parses a given argument and sets the denoted Graal option.
   *
   * @throws InternalError if there was a problem parsing or setting the option
   */
  static void parse_argument(KlassHandle hotSpotOptionsClass, char* arg, TRAPS);

  /**
   * Searches for a Graal option denoted by a given name and sets it value.
   *
   * The definition of this method is in graalRuntime.inline.hpp
   * which is generated by com.oracle.graal.hotspot.sourcegen.GenGraalRuntimeInlineHpp.
   *
   * @param hotSpotOptionsClass the HotSpotOptions klass or NULL if only checking for valid option
   * @param name option name
   * @param name_len length of option name
   * @returns true if the option was found
   * @throws InternalError if there was a problem setting the option's value
   */
  static bool set_option(KlassHandle hotSpotOptionsClass, char* name, int name_len, const char* value, TRAPS);

  /**
   * Raises an InternalError for an option that expects a value but was specified without a "=<value>" prefix.
   */
  static void check_required_value(const char* name, int name_len, const char* value, TRAPS);

  /**
   * Java call to HotSpotOptions.setOption(String name, OptionValue<?> option, char spec, String stringValue, long primitiveValue)
   *
   * @param name option name
   * @param name_len length of option name
   */
  static void set_option_helper(KlassHandle hotSpotOptionsClass, char* name, int name_len, Handle option, jchar spec, Handle stringValue, jlong primitiveValue);

  /**
   * Instantiates a service object, calls its default constructor and returns it.
   *
   * @param name the name of a class implementing com.oracle.graal.api.runtime.Service
   */
  static Handle create_Service(const char* name, TRAPS);

 public:

  static void initialize_natives(JNIEnv *env, jclass c2vmClass);

  static bool is_HotSpotGraalRuntime_initialized() { return _HotSpotGraalRuntime_instance != NULL; }

  /**
   * Gets the singleton HotSpotGraalRuntime instance, initializing it if necessary
   */
  static Handle get_HotSpotGraalRuntime();

  static jobject get_HotSpotGraalRuntime_jobject() {
    get_HotSpotGraalRuntime();
    return _HotSpotGraalRuntime_instance;
  }

  static void shutdown();

  /**
   * Given an interface representing a Graal service (i.e. sub-interface of
   * com.oracle.graal.api.runtime.Service), gets an array of objects, one per
   * known implementation of the service.
   *
   * The definition of this method is in graalRuntime.inline.hpp
   * which is generated by com.oracle.graal.hotspot.sourcegen.GenGraalRuntimeInlineHpp.
   */
  static Handle get_service_impls(KlassHandle serviceKlass, TRAPS);

  /**
   * Aborts the VM due to an unexpected exception.
   */
  static void abort_on_pending_exception(Handle exception, const char* message, bool dump_core = false);

  /**
   * Calls Throwable.printStackTrace() on a given exception.
   */
  static void call_printStackTrace(Handle exception, Thread* thread);

#define GUARANTEE_NO_PENDING_EXCEPTION(error_message) do { \
    if (HAS_PENDING_EXCEPTION) { \
      GraalRuntime::abort_on_pending_exception(PENDING_EXCEPTION, error_message); \
    } \
  } while (0);

  static Klass* load_required_class(Symbol* name);

  static BufferBlob* initialize_buffer_blob();

  /**
   * Checks that all Graal specific VM options presented by the launcher are recognized
   * and formatted correctly. To set relevant Java fields from the option, parse_arguments()
   * must be called. This method makes no Java calls apart from creating exception objects
   * if there is an errors in the Graal options.
   */
  static jint check_arguments(TRAPS);

  /**
   * Parses the Graal specific VM options that were presented by the launcher and sets
   * the relevants Java fields.
   */
  static bool parse_arguments(KlassHandle hotSpotOptionsClass, TRAPS);

  static BasicType kindToBasicType(jchar ch);
  static address create_external_deopt_i2c();
  static address get_external_deopt_i2c_entry() {return _external_deopt_i2c_entry;}

  // The following routines are all called from compiled Graal code

  static void new_instance(JavaThread* thread, Klass* klass);
  static void new_array(JavaThread* thread, Klass* klass, jint length);
  static void new_multi_array(JavaThread* thread, Klass* klass, int rank, jint* dims);
  static void dynamic_new_array(JavaThread* thread, oopDesc* element_mirror, jint length);
  static void dynamic_new_instance(JavaThread* thread, oopDesc* type_mirror);
  static jboolean thread_is_interrupted(JavaThread* thread, oopDesc* obj, jboolean clear_interrupted);
  static void vm_message(jboolean vmError, jlong format, jlong v1, jlong v2, jlong v3);
  static jint identity_hash_code(JavaThread* thread, oopDesc* obj);
  static address exception_handler_for_pc(JavaThread* thread);
  static void monitorenter(JavaThread* thread, oopDesc* obj, BasicLock* lock);
  static void monitorexit (JavaThread* thread, oopDesc* obj, BasicLock* lock);
  static void create_null_exception(JavaThread* thread);
  static void create_out_of_bounds_exception(JavaThread* thread, jint index);
  static void vm_error(JavaThread* thread, jlong where, jlong format, jlong value);
  static oopDesc* load_and_clear_exception(JavaThread* thread);
  static void log_printf(JavaThread* thread, oopDesc* format, jlong v1, jlong v2, jlong v3);
  static void log_primitive(JavaThread* thread, jchar typeChar, jlong value, jboolean newline);
  // Note: Must be kept in sync with constants in com.oracle.graal.replacements.Log
  enum {
    LOG_OBJECT_NEWLINE = 0x01,
    LOG_OBJECT_STRING  = 0x02,
    LOG_OBJECT_ADDRESS = 0x04
  };
  static void log_object(JavaThread* thread, oopDesc* msg, jint flags);
  static void write_barrier_pre(JavaThread* thread, oopDesc* obj);
  static void write_barrier_post(JavaThread* thread, void* card);
  static jboolean validate_object(JavaThread* thread, oopDesc* parent, oopDesc* child);
  static void new_store_pre_barrier(JavaThread* thread);
};

// Tracing macros

#define IF_TRACE_graal_1 if (!(TraceGraal >= 1)) ; else
#define IF_TRACE_graal_2 if (!(TraceGraal >= 2)) ; else
#define IF_TRACE_graal_3 if (!(TraceGraal >= 3)) ; else
#define IF_TRACE_graal_4 if (!(TraceGraal >= 4)) ; else
#define IF_TRACE_graal_5 if (!(TraceGraal >= 5)) ; else

// using commas and else to keep one-instruction semantics

#define TRACE_graal_1 if (!(TraceGraal >= 1 && (tty->print("TraceGraal-1: "), true))) ; else tty->print_cr
#define TRACE_graal_2 if (!(TraceGraal >= 2 && (tty->print("   TraceGraal-2: "), true))) ; else tty->print_cr
#define TRACE_graal_3 if (!(TraceGraal >= 3 && (tty->print("      TraceGraal-3: "), true))) ; else tty->print_cr
#define TRACE_graal_4 if (!(TraceGraal >= 4 && (tty->print("         TraceGraal-4: "), true))) ; else tty->print_cr
#define TRACE_graal_5 if (!(TraceGraal >= 5 && (tty->print("            TraceGraal-5: "), true))) ; else tty->print_cr

#endif // SHARE_VM_GRAAL_GRAAL_RUNTIME_HPP
