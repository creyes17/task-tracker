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

(ns
  ^{:author "Christopher R. Reyes"}
  dev.chrisreyes.task-tracker.core
  "Here we coordinate between the different layers of the app.
  Eventually, this namespace will be responsible for allowing
  end-users to interact with the system by creating, reading,
  updating, and deleting tasks."
  (:gen-class)
  (:require
    [clojure.data.json :as json]
    [compojure.core]
    [dev.chrisreyes.task-tracker.persistence :as persistence]
    [org.httpkit.server :refer [run-server]]
    [semver.core]))

(compojure.core/defroutes backend-api
  (compojure.core/GET "/.internal/is_healthy"
                      []
                      (json/write-str {:healthy true}))
  (compojure.core/GET "/:version/project"
                      [version]
                      (if (= \v (first version))
                        (if (semver.core/equal? (subs version 1) "1.0.0")
                          (json/write-str
                            (persistence/get-all-roots
                              (persistence/get-config
                                (persistence/get-secret-from-aws
                                  (persistence/get-credentials-secret)))))
                          (if (semver.core/newer? (subs version 1) "1.0.0")
                            ; Requested version is newer than our latest version
                            {:status 404
                             :headers {}
                             :body {:message "Latest version is v1.0.0"}}
                            ; Requested version is older than our first published version
                            {:status 404
                             :headers {}
                             :body {:message "NOT FOUND"}}))
                        ; Version is invalid
                        {:status 404
                         :headers {}
                         :body {:message "NOT FOUND"}})))

(defn -main
  "Starts a backend webserver on port 5000 to handle API requests for working with tasks"
  [& args]
  ; Note: Chose port 5000 arbitrarily based on a tutorial I was
  ;       following. It shouldn't really matter assuming we host this in
  ;       docker-compose because we can remap the port to anything.
  (run-server backend-api {:port 5000}))
