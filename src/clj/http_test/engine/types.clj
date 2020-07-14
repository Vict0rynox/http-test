(ns http-test.engine.types
  (:refer-clojure :exclude [get set test assert])
  (:require [clojure.string :as str]))


(defprotocol StateAccess
  (set [this name value])
  (get [this name])
  (isEmpty [this])
  (clear [this name])
  (clearAll [this]))


(defprotocol HttpTest
  (test [this test-name func])
  (assert [this test msg]))


(defprotocol ResponseHeadersAccess
  (valueOf [this name])
  (valuesOf [this name]))


(deftype ContentType [mimeType charset])


(deftype HttpResponse [body headers status contentType])


(deftype ResponseHeaders [headers]
  ResponseHeadersAccess

  (valueOf [this name]
    (first (clojure.core/get headers name)))

  (valuesOf [this name]
    (clojure.core/get headers name [])))


(defn headers->ResponseHeaders [headers]
  (ResponseHeaders. (reduce-kv #(assoc %1 %2 (str/split %3 #";")) {} headers)))


(defn resp->HttpResponse [{:keys [status headers request-time body] :as resp}]
  (let [[mimeType charset] (map str/trim (str/split (clojure.core/get headers "Content-Type" "text/plain; charset=utf-8") #";" 2))
        charset (if (str/starts-with? charset "charset=")
                  (subs charset 8) charset)]
    (HttpResponse. body
                   (headers->ResponseHeaders headers)
                   status
                   (ContentType. mimeType charset))))
