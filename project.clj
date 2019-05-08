(defproject task-tracker "0.1.0-SNAPSHOT"
  :description "Keeps track of task estimates and subtasks."
  :url "http://www.github.com/creyes17/task-tracker"
  :license {:name "GNU General Public License v3.0"
            :url "none"
            :year 2019
            :key "gpl-3.0"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.postgresql/postgresql "42.2.5.jre7"]
                 [org.clojure/java.jdbc "0.7.9"]]
  :main ^:skip-aot task-tracker.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
