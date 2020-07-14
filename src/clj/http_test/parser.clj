(ns http-test.parser
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.core.match :as m])
  (:import (java.net MalformedURLException)))


(def START-SCRIPT-ANCHOR "> {%")

(def END-SCRIPT-ANCHOR "%}")

(def COMMENT-ANCHOR "#")

(def END-REQUEST-ANCHOR "\n###")

(def ALLOW-HTTP-METHODS
  #{:get
    :head
    :post
    :put
    :delete
    :connect
    :options
    :trace
    :patch})

(defn- token [type value]
  #_{:type type :value value}
  [type value])


(defn- skip-comment
  "drop strings started with # or empty (\\n)"
  [string]
  (let [s (str/trim string)]
    (if (and (string? s) (or (str/starts-with? s "#")
                           (str/starts-with? s "\n")))
      [nil (second (str/split s #"\n" 2))]
      [nil s])))


(defn- method-valid? [method]
  (contains? ALLOW-HTTP-METHODS method))


(defn- read-method [string]
  (let [[method string] (str/split string #"\s+" 2)
        method (-> method
                   (str/trim)
                   (str/lower-case)
                   (keyword))]
    (when-not (method-valid? method)
      (throw (IllegalArgumentException. (format "`%s` is not valid http method" method))))
    [(token :method method) string]))


(defn- url? [url]
  (try
    (io/as-url url) true
    (catch MalformedURLException e
      (println (.getMessage e))
      false)))


(defn- read-uri [string]
  (let [[uri string] (str/split string #"\s+" 2)
        uri (str/trim uri)]
    (when-not (url? uri)
      (throw (IllegalArgumentException. (format "`%s` is not valid http uri" uri))))
    [(token :uri uri) string]))


(defn- str->header [string]
  (when-not (str/starts-with? string COMMENT-ANCHOR)             ;;skip comment
    (let [[name value] (str/split string #":" 2)
          value (first (str/split value #" #" 2))]
      [(str/lower-case (str/trim name)) (str/trim value)])))


(defn- read-headers [string]
  (let [[headers-str string] (str/split string #"\n{2}" 2)
        headers (keep str->header (str/split headers-str #"\n"))]
    [(token :headers headers) string]))


;;отправляет только если в заголовках есть Content-Type, иначе скпи
(defn- read-body [string]
  (let [script? (str/index-of string START-SCRIPT-ANCHOR)
        end?    (str/index-of string END-REQUEST-ANCHOR)
        limit   (cond
                  (and script? end?
                    (> script? end?)) end?

                  (and script? end?
                    (< script? end?)) script?

                  script? script?
                  end? end?)
        [body string] (if limit [(subs string 0 limit) (subs string limit)]
                                [string nil])]
    [(token :body (str/trim body)) string]))


(defn- read-script [string]
  (let [start      (+ (str/index-of string START-SCRIPT-ANCHOR) (count START-SCRIPT-ANCHOR))
        end-script (str/index-of string END-SCRIPT-ANCHOR)
        end-string (str/index-of string END-REQUEST-ANCHOR)

        [script string]
        (m/match [end-script end-string]
          [nil nil] [(subs string start) nil]

          [_ nil] [(subs string start end-script) nil]

          [nil _] [(subs string start end-string)
                   (subs string end-string)]

          [_ _] [(subs string start end-script)
                 (subs string end-string)])]
    [(token :script (str/trim script)) string]))


(defn- read-end [string]
  (let [end-string (str/index-of string END-REQUEST-ANCHOR)
        string     (when end-string (subs string (+ end-string (count END-REQUEST-ANCHOR))))]
    [(token :end nil) string]))


(defn- tokenize
  ([string] (tokenize string []))
  ([string tokens]
   (let [[token string]
         (m/match [tokens string]
           [(:or [] [[:end _] & _]) (s :guard #(or (str/starts-with? % "#") (str/starts-with? % "\n")))]
           (skip-comment string)

           [(:or [] [[:end _] & _]) (s :guard #(not (empty? %)))]
           (read-method string)

           [[[:method _] & _] _]
           (read-uri string)

           [[[:uri _] & _] _]
           (read-headers string)

           [[[:headers _] & _] _]
           (read-body string)

           [[[:body _] & _] (s :guard #(str/starts-with? % "> {%"))]
           (read-script string)

           [[[(:or :headers :body :script :uri) _] & _] _] (read-end string)

           :else (throw (IllegalArgumentException. "Request string is invalid.")))
         tokens (if token (into [token] tokens) tokens)]
     (if (empty? string)
       tokens
       (tokenize string tokens)))))


;;todo not add body, when `content-type` header not setup
(defn- token-apply [scenario [type value]]
  (case type
    :method
    (update scenario :request assoc :request-method value)
    :uri
    (update scenario :request assoc :url value)
    :headers
    (update scenario :request assoc :headers (into {} value))
    :body
    (update scenario :request assoc :body value)
    :script
    (assoc scenario :script value)
    scenario))

;;todo refactor double reverse
(defn- tokens->scenarios [tokens]
  (reverse (filter some?
                   (reduce (fn [scenarios token]
                             (let [s    (or (first scenarios) {})
                                   curr (token-apply s token)]
                               (apply conj (rest scenarios) (if (= (first token) :end) (list curr nil) (list curr)))))
                           []
                           (reverse tokens)))))


(defn string->scenarios [string]
  (tokens->scenarios (tokenize string)))



