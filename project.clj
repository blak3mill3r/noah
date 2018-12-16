(def kafka-version "2.0.0")

(defproject blak3mill3r/noah "0.0.0"
  :description "Kafka Streams in Clojure"
  :url "https://github.com/blak3mill3r/noah"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [camel-snake-kebab "0.4.0"]
                 [clj-time "0.15.1"]
                 [potemkin "0.4.5"]

                 [net.cgrand/xforms "0.18.2"]
                 [ymilky/franzy-nippy "0.0.1"]
                 [com.taoensso/nippy "2.14.0"]
                 [org.apache.kafka/kafka-streams ~kafka-version :scope "provided"]
                 [org.apache.kafka/kafka-clients ~kafka-version :scope "provided"]]
  :plugins [[lein-cloverage "1.0.13"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]]
  :profiles {:dev {:dependencies [[com.rpl/specter "1.1.2"]
                                  ;; TODO extract javadoc comments, and method parameter names, for clojure docstrings
                                  [com.github.javaparser/javaparser-core "3.7.1"]]
                   :source-paths ["src" "dev"]}
             :test {:dependencies [[org.apache.kafka/kafka-streams-test-utils ~kafka-version]]}}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"update-readme-version" ["shell" "sed" "-i" "s/\\\\[noah \"[0-9.]*\"\\\\]/[noah \"${:version}\"]/" "README.md"]}
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["changelog" "release"]
                  ["update-readme-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["vcs" "push"]])
