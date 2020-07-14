package com.httptest.engine;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

final public class Engine {

    private final ScriptEngine scriptEngine;

    public Engine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public static Engine ByName(String engineName) {
        return new Engine(new ScriptEngineManager().getEngineByName(engineName));
    }

    public final Object eval(String script, Bindings bindings) throws ScriptException {
        return scriptEngine.eval(script, bindings);
    }

    public final Object eval(String script) throws ScriptException {
        return scriptEngine.eval(script);
    }

    public final Bindings getBindings(int scope) throws ScriptException {
        return this.scriptEngine.getBindings(scope);
    }

    public final void setBindings(Bindings bindings, int scope) throws ScriptException {
        this.scriptEngine.setBindings(bindings, scope);
    }

}
