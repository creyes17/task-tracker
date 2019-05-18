(defproject task-tracker "0.1.0-SNAPSHOT"
  :description "Keeps track of task estimates and subtasks."
  :url "http://www.github.com/creyes17/task-tracker"
  :license {:name "GNU General Public License v3.0"
            :url "none"
            :year 2019
            :key "gpl-3.0"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.8.1"]
                 [com.cognitect.aws/api "0.8.305"]
                 [com.cognitect.aws/endpoints "1.1.11.549"]
                 [com.cognitect.aws/secretsmanager "707.2.405.0"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [org.postgresql/postgresql "42.2.5.jre7"]]
  :main ^:skip-aot task-tracker.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
