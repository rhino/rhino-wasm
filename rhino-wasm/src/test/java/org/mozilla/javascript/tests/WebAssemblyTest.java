/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.typedarrays.ByteIo;
import org.mozilla.javascript.typedarrays.NativeArrayBuffer;
import org.mozilla.javascript.typedarrays.NativeUint8Array;
import org.rhino.community.wasm.WebAssembly;

public class WebAssemblyTest {

    private Object testEval(String script) {
        try (Context cx = Context.enter()) {
            Scriptable jsScope = cx.initStandardObjects();
            WebAssembly.init(cx, jsScope, false);
//            NativeUint8Array.init();
            return cx.evaluateString(jsScope, script, "web-assembly-inline.js", 1, null);
        }
    }

    @Test
    public void instantiateAWebAssemblyModule() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable jsScope = cx.initStandardObjects();
            WebAssembly.init(cx, jsScope, false);
            byte[] wasmBytes = this.getClass().getResourceAsStream("/wasm/add.wasm").readAllBytes();
            StringBuilder sb = new StringBuilder();
            sb.append("new Uint8Array([");
            boolean first = true;
            for (var b: wasmBytes) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(b);
            }
            sb.append("])");
            // finish with this:
            // const { add } = wasmModule.instance.exports; add(5, 6);
            Object result = cx.evaluateString(jsScope, "const wasmModule = WebAssembly.instantiate(" + sb + "); wasmModule.instance", "ex.js", 1, null);
            assertEquals(11, result);
        }
    }
}
