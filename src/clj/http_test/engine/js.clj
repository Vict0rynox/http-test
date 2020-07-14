(ns http-test.engine.js
  (:refer-clojure :exclude [eval])
  (:require [clojure.java.io :as io])
  (:import (javax.script SimpleBindings ScriptException ScriptContext)
           (com.httptest.engine Engine)
           (java.util.function Predicate)))


(def ^Engine engine (Engine/ByName "JavaScript"))


(def ^:private bindings (atom (SimpleBindings.)))


(def ^:private js-http-response-fn (slurp (io/resource "js/interop.js")))


(defn- init-env []
  (.setBindings engine @bindings ScriptContext/ENGINE_SCOPE)
  (.put @bindings "polyglot.js.allowAllAccess" true)
  (.put @bindings "polyglot.js.allowHostAccess" true)
  (.put @bindings "polyglot.js.allowHostClassLookup" (reify Predicate (test [_ _] true)))
  (.eval engine js-http-response-fn)
  :ok)


(def ^:private init-memo (memoize init-env))


(defn clear-env
  "Clear engine env"
  []
  (reset! bindings (SimpleBindings.)))


(defn eval [script http-response]
  (init-memo)
  (.put @bindings "response" http-response)
  (.eval engine "response = new HttpResponse(response);")
  (try
    (let [r (.eval engine script)]
      [:ok r])
    (catch ScriptException se
      [:error (.getMessage se)])
    (catch Throwable t
      [:error t])))


(defn failure? [[status _]]
  (= status :error))

