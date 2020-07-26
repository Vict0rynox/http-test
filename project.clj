(defproject http-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :source-paths #{"src/clj"}
  :java-source-paths #{"src/java"}
  :test-paths #{"test"}
  :resource-paths #{"resources"}
  :dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                 ;; [clj-http "3.10.0"]
                 ;; [cheshire "5.9.0"]

                 ;;http client
                 ;;[aleph "0.4.6"]
                 [org.martinklepsch/clj-http-lite "0.4.3"]

                 ;;cli-tool
                 [org.clojure/tools.cli "1.0.194"]

                 ;;parser
                 [instaparse "1.4.10"]

                 ;;match
                 [org.clojure/core.match "1.0.0"]]
  :main http-test.core
  :target-path "target/%s"
  :aot [http-test.engine.types]

  :profiles {:uberjar {:aot :all}
             :dev     {:plugins      [[lein-cljfmt "0.5.4"]
                                      [lein-shell "0.5.0"]]
                       :cljfmt       {:indentation?                    false
                                      :file-pattern                    #"\.clj$"
                                      :remove-consecutive-blank-lines? false}
                       :dependencies [[flames "0.4.0"]
                                      [pjstadig/humane-test-output "0.10.0"]]
                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]}}
  :aliases {"native" ["shell"
                      "native-image" "--report-unsupported-elements-at-runtime"
                      "--initialize-at-build-time"
                      "--no-server" "-jar" "./target/uberjar//${:uberjar-name:-${:name}-${:version}-standalone.jar}"
                      "-H:EnableURLProtocols=https"
                      "-H:EnableURLProtocols=http"
                      "--language:js"
                      "-H:ReflectionConfigurationFiles=./resources/reflectconfig.json"
                      "-H:Name=./${:name}"]})
