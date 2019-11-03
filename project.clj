;; Copyright (C) 2019  Christopher R. Reyes
;;
;; This file is part of Task Tracker.
;;
;; Task Tracker is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.
;;
;; Task Tracker is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with Task Tracker.  If not, see <https://www.gnu.org/licenses/>.

(defproject task-tracker "0.1.0-SNAPSHOT"
  :description "Keeps track of task estimates and subtasks."
  :url "http://www.github.com/creyes17/task-tracker"
  :license {:name "GNU General Public License v3.0"
            :url "none"
            :year 2019
            :key "gpl-3.0"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.8.1"]
                 [com.cognitect.aws/api "0.8.391"]
                 [com.cognitect.aws/endpoints "1.1.11.549"]
                 [com.cognitect.aws/secretsmanager "707.2.405.0"]
                 [compojure "1.6.1"]
                 [http-kit "2.3.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [org.clojure/test.check "0.9.0"]
                 [org.postgresql/postgresql "42.2.5.jre7"]]
  :main ^:skip-aot dev.chrisreyes.task-tracker.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
