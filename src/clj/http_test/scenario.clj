(ns http-test.scenario
  (:refer-clojure :exclude [run!])
  (:require [http-test.engine.js :as js]
    ;;[aleph.http :as http]
            [clj-http.lite.client :as http]
            [clojure.string :as str]

            [http-test.engine.types :as engine.t])
  (:import (java.text SimpleDateFormat)
           (java.util Locale Date)))


(defn- request->string [req]
  (format "%s %s"
          (-> req :request-method name str/upper-case)
          (-> req :url)))


(defn- response->string [{:keys [status headers request-time body] :as resp}]
  (let [body        (if (some? body) body "")
        headers-str (reduce-kv #(str %1 %2 ": " %3 "\n") "" headers)
        date        (.format (SimpleDateFormat. "E, dd MMM Y HH:mm:ss z" Locale/ENGLISH) (Date.))]
    (str "HTTP/1.1 " status "\n"
         ;;"Date: " date "\n"
         headers-str "\n"
         body "\n\n"
         "Response code:" status "; "
         "Time:" request-time "ms; "
         "Content length: " (count body) " bytes")))


(defn- print-response? [print-status scenario-status]
  (or (= print-status :always)
    (and (= print-status :on-error) (= scenario-status :error))))


(defn scenario-print
  "Print scenario output.

  When print http response
  :resp #{:on-error, :none, :always}
 "
  [{:keys [request response test-result]} {:keys [resp] :or {resp :on-error}}]

  (let [[status msg] test-result
        string (cond-> (str (request->string request) "\n\n")

                 (and (some? response) (print-response? resp status))
                 (str (response->string response) "\n\n")

                 (= status :ok)
                 ;; add count passed tests
                 (str "Test passed.")

                 (= status :error)
                 (str "Test failure.")

                 (= status nil)
                 (str "Ok.")

                 :always (str "\n"))]

    (println string)
    (when (= status :error)
      (binding [*out* *err*]
        (println msg)))
    (flush)))


(defn- send-request [req]
  (let [req (cond-> req
              :always (assoc :method (:request-method req)
                             :throw-exceptions false
                             :follow-redirects false
                             :insecure? true)
              :always (dissoc :request-method)
              (-> req :body empty?) (dissoc :body)
              (nil? (get-in req [:headers "content-type"])) (dissoc :body))]
    (try
      [:ok (http/request req)]
      (catch Exception e
        (let [resp (ex-data e)]
          (if resp [:ok resp] [:error e]))))))


(defn run!
  "Send Http request, and validate response with script"
  [{:keys [request script] :as scenario}]

  (let [[status resp] (send-request request)
        test-result (if (= status :ok)
                      (when script
                        (js/eval script (engine.t/resp->HttpResponse resp)))
                      [status resp])]
    (merge scenario
           {:response    (if (= status :ok) resp nil)
            :failure     (js/failure? test-result)
            :test-result test-result})))


(defn run-scenarios! [scenarios options]
  (println "Count: " (count scenarios))
  (doall (map #(let [s (run! %)]
                 (scenario-print s (:print options))
                 s) scenarios)))


(defn failure? [scenarios]
  (some :failure scenarios))