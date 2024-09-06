/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.rhino.community.wasm;

import java.util.ArrayList;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Module;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptRuntimeES6;
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

        wasm.defineProperty(scope, "instantiate", 2, WebAssembly::instantiate, DONTENUM, DONTENUM | READONLY);

        ScriptableObject.defineProperty(scope, WEBASSEMBLY_TAG, wasm, DONTENUM);
        if (sealed) {
            wasm.sealObject();
        }
    }

    // WebAssembly.instantiate
    private static Object instantiate(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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
        var result = new ResultObject(new Module(wasmModule), new Instance(wasmInstance));
        // TODO: should be in a Promise
        //        var promise = new NativePromise();
        //        promise.put(0, scope, new ResultObject(new Module(wasmModule), new Instance(wasmInstance)));
        return result;
    }

    public static class ResultObject extends ScriptableObject {
        private Module module;
        private Instance instance;

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
