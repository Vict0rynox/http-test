(ns http-test.parser
  (:require [clojure.string :as str]
            [instaparse.core :as insta :refer [defparser]]))


;;TODO: add annotation `//@ ...`
;;TODO: add template vars `{{ ... }}`
;;TODO: add data from file inject ` < {path} `
;;TODO: add multipart/data support

(defparser http-scenario
           "REQUESTS = {REQUEST <END>?}
            REQUEST = <COMMENTS> METHOD <SPACES> URL <LINE_BREAK> [HEADERS [<LINE_BREAK> BODY?]]
            COMMENTS ={COMMENT? LINE_BREAK}
            COMMENT = &'#' #'.*'
            HCOMMENT = &'#' !'###' #'.*'
            SPACES = #'\\s+'
            LINE_BREAK = '\n'
            END = LINE_BREAK '###' '#'* LINE_BREAK?
            SCRIPT_START = '> {%'
            SCRIPT_END = '%}' LINE_BREAK*
            METHOD = 'GET'|'HEAD'|'POST'|'PUT'|'DELETE'|'CONNECT'|'OPTIONS'|'TRACE'|'PATCH'
            URL = #\"https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9]{1,6}\\b([-a-zA-Z0-9\\(\\)@:%_\\+.~#?&//=]*)\"
            HEADERS = {HEADER | <HCOMMENT> <LINE_BREAK>}
            HEADER = HEADER_KEY <':'> <SPACES>? HEADER_VALS?
            HEADER_KEY = #'[-a-zA-Z_]+'
            HEADER_VALS = HEADER_VAL <SPACES>? {<';'> <SPACES>? HEADER_VAL} <HCOMMENT>?
            HEADER_VAL = #'[-a-zA-Z0-9\\(\\)@:%_\\+.~#?&//=]*'
            DATA = #'(?s)((?!\\n###).)'+
            SCRIPT = <SCRIPT_START> #'(?s)((?!%}).)'+ <SCRIPT_END>
            BODY = !END ([!SCRIPT_START DATA ] SCRIPT?)
           ")


(def request-transform
  {:REQUESTS vector
   :REQUEST     (fn [& r]
                  (let [scenario (apply merge r)]
                    {:request (select-keys scenario [:request-method :url :headers :body])
                     :script (:script scenario)}))
   :METHOD      (fn [method]
                  {:request-method (keyword method)})
   :URL         (fn [url]
                  {:url (str/trim url)})
   :HEADERS     (fn [& headers]
                  {:headers (into {} headers)})
   :HEADER      (fn [& [hkey hval]]
                  [hkey hval])
   :HEADER_KEY  str/trim
   :HEADER_VAL  str/trim
   :HEADER_VALS (fn [& vals]
                  (str/join ";" vals))
   :DATA        (comp str/trim str)
   :SCRIPT      (comp str/trim str)
   :BODY        (fn [& [body script]]
                  {:body   body
                   :script script})})


(defn string->scenarios [string]
  (->> (http-scenario string)
       (insta/transform request-transform)))