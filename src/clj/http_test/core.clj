(ns http-test.core
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]

            [http-test.engine.types :as _]                  ;;need for success build

            [http-test.parser :as parser]
            [http-test.scenario :as scenario]
            [clojure.java.io :as io])
  (:gen-class))


(defn exit [code message]
  (println message)
  (System/exit code))


(def cli-options
  [["-h" "--help" "show help"]
   ["-f" "--file PATH" "Http Request file"
    :validate [#(.exists (io/file %)) "Specified file does not exist"]]
   [nil "--response INDEX" "Print response level. Has one of `on-error` `none` `always`."
    :default :on-error
    :parse-fn keyword
    :validate [#(contains? #{:on-error, :none, :always} (keyword %))]]])


(defn start! [strs opt]
  (let [result (-> strs
                   (parser/string->scenarios)
                   (scenario/run-scenarios! opt))]
    (when (scenario/failure? result)
      ;;TODO: add count failure tests
      (exit 1 "Tests: Failure"))))


(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))


(defn validate-args [args]
  (let [{:keys [options arguments summary errors]}
        (cli/parse-opts args cli-options)]

    (cond
      (:help options)
      {:exit-message summary :ok? true}

      errors
      {:exit-message (error-msg errors) :ok? true}

      (not (empty? arguments))
      {:ok true :data (first arguments)}

      (:file options)
      {:ok true
       :data (slurp (:file options))
       :options {:print {:resp (:response options)}}}

      :else
      {:exit-message summary :ok? true})))


(defn -main [& args]
  (let [{:keys [data options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (start! data options))))
