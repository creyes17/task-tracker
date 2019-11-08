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

(defn- find-closest-version
  "Finds the closest valid-version to version without going past.
   Assumes valid-versions is sorted from newest to oldest."
  [valid-versions version]
  (loop [new-index 0
         old-index (dec (count valid-versions))]
    (if (> new-index old-index)
      (if (>= (inc old-index) (count valid-versions))
        nil
        (nth valid-versions (inc old-index)))
      (let [mid-index (quot (+ new-index old-index) 2)
            mid-version (nth valid-versions mid-index)]
        (cond (semver.core/equal? version mid-version) mid-version

              (semver.core/older? version mid-version) (recur
                                                         (inc mid-index)
                                                         old-index)

              (semver.core/newer? version mid-version) (recur
                                                         new-index
                                                         (dec mid-index)))))))

(defn- versioned-route
  "Prefixes a route with a version and distributes to the correct
   implementation based on version number"
  [versions]
  (fn [version & args]
    (if (= \v (first version))
      (let [semantic-version (subs version 1)
            valid-versions (semver.core/sorted (keys versions))
            newest-version (first valid-versions)
            oldest-version (last valid-versions)]
        (cond (contains? versions semantic-version)
              (apply (get versions semantic-version) version args)

              (semver.core/newer? semantic-version newest-version)
              {:status 404
               :headers {}
               :body {:message (str "Latest version is v" newest-version)}}

              (semver.core/older? semantic-version oldest-version)
              {:status 404
               :headers {}
               :body {:message "NOT FOUND"}}

              ; At this point, the version is in range.
              ; Figure out which is the newest version that
              ; is still less than the semantic-version.
              :else (apply (get versions
                                (find-closest-version valid-versions semantic-version))
                           version
                           args)))
      {:status 404
       :headers {}
       :body {:message "NOT FOUND"}})))

(defn- get-all-projects-v-1-0-0
  "Implementation V1.0.0 of the /project endpoint"
  ([version]
   (get-all-projects-v-1-0-0))
  ([]
   (json/write-str
     (persistence/get-all-roots
       (persistence/get-config
         (persistence/get-secret-from-aws
           (persistence/get-credentials-secret)))))))


(compojure.core/defroutes backend-api
  (compojure.core/GET "/.internal/is_healthy"
                      []
                      (json/write-str {:healthy true}))
  ; TODO: Something that wraps compojure.core/GET so we don't need this weird syntax
  (compojure.core/GET "/:version/project"
                      [version]
                      ((versioned-route {"1.0.0" get-all-projects-v-1-0-0}) version)))

(defn -main
  "Starts a backend webserver on port 5000 to handle API requests for working with tasks"
  [& args]
  ; Note: Chose port 5000 arbitrarily based on a tutorial I was
  ;       following. It shouldn't really matter assuming we host this in
  ;       docker-compose because we can remap the port to anything.
  (run-server backend-api {:port 5000}))
