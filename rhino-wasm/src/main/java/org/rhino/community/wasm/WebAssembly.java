/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.rhino.community.wasm;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.typedarrays.NativeArrayBuffer;
import org.mozilla.javascript.typedarrays.NativeTypedArrayView;

public class WebAssembly extends ScriptableObject {

    private static final String WEBASSEMBLY_TAG = "WebAssembly";

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        WebAssembly wasm = new WebAssembly();
        wasm.setPrototype(getObjectPrototype(scope));
        wasm.setParentScope(scope);

        wasm.defineProperty(
                scope, "instantiate", 2, WebAssembly::instantiate, DONTENUM, DONTENUM | READONLY);

        ScriptableObject.defineProperty(scope, WEBASSEMBLY_TAG, wasm, DONTENUM);
        if (sealed) {
            wasm.sealObject();
        }
    }

    // WebAssembly.instantiate
    private static Object instantiate(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // TODO: implement the other constructors
        assert (args.length == 1);
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }
        Object arg0 = (args.length > 0 ? args[0] : Undefined.instance);
        Object arg1 = (args.length > 1 ? args[1] : Undefined.instance);
        return instantiateInternal(cx, scope, thisObj, arg0, arg1);
    }

    // WebAssemblyInstantiate abstract operation
    private static Object instantiateInternal(
            Context cx, Scriptable scope, Object constructor, Object arg0, Object arg1) {
        byte[] bytes;
        if (arg0 instanceof NativeArrayBuffer) {
            bytes = ((NativeArrayBuffer) arg0).getBuffer();
        } else if (arg0 instanceof NativeTypedArrayView<?>) {
            bytes = ((NativeTypedArrayView) arg0).getBuffer().getBuffer();
        } else {
            // TODO: check error messages
            throw ScriptRuntime.typeError("not supported " + arg0.getClass());
        }
        var wasmModule = com.dylibso.chicory.runtime.Module.builder(bytes).build();
        var wasmInstance = wasmModule.instantiate();
        var module = new Module(wasmModule);
        var instance = new Instance(wasmInstance);
        var result = new ResultObject(module, new Instance(wasmInstance));

        var exports = new Exports(wasmInstance);
        ScriptableObject.defineProperty(result.instance, "exports", exports, DONTENUM | READONLY);
        for (var export : wasmModule.exports().keySet()) {
            exports.defineProperty(
                    export,
                    (Callable)
                            (context, scriptable, scriptable1, objects) -> {
                                // TODO: this is just a "demo implementation" that only works with
                                // the "add" function
                                var op1 = Value.i32((int) objects[0]);
                                var op2 = Value.i32((int) objects[1]);
                                var res = wasmInstance.export(export).apply(op1, op2);
                                return res[0].asInt();
                            },
                    DONTENUM | READONLY);
        }

        ScriptableObject.defineProperty(result, "instance", result.instance, DONTENUM | READONLY);
        ScriptableObject.defineProperty(result, "module", result.module, DONTENUM | READONLY);

        return result;
    }

    private static class Exports extends ScriptableObject {
        com.dylibso.chicory.runtime.Instance instance;

        public Exports(com.dylibso.chicory.runtime.Instance instance) {
            this.instance = instance;
        }

        @Override
        public String getClassName() {
            return "WebAssembly.Exports";
        }
    }

    private static class ResultObject extends ScriptableObject {
        Module module;
        Instance instance;

        public ResultObject(Module module, Instance instance) {
            this.module = module;
            this.instance = instance;
        }

        @Override
        public String getClassName() {
            return "WebAssembly.ResultObject";
        }
    }

    public static class Module extends ScriptableObject {
        private com.dylibso.chicory.runtime.Module wasmModule;

        public Module(com.dylibso.chicory.runtime.Module wasmModule) {
            this.wasmModule = wasmModule;
        }

        @Override
        public String getClassName() {
            return "WebAssembly.Module";
        }
    }

    public static class Instance extends ScriptableObject {
        private com.dylibso.chicory.runtime.Instance wasmInstance;

        public Instance(com.dylibso.chicory.runtime.Instance wasmInstance) {
            this.wasmInstance = wasmInstance;
        }

        @Override
        public String getClassName() {
            return "WebAssembly.Instance";
        }
    }

    @Override
    public String getClassName() {
        return "WebAssembly";
    }
}
