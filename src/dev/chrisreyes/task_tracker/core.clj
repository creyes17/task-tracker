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
    [compojure.route]
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

(defn user-error-response
  "Generates a 400 response indicating that the request is invalid."
  [reason]
  {:status 400
   :headers {}
   :body {:message reason}})

(defn not-found-response
  "Generates a 404 response indicating the endpoint doesn't exist"
  ([]
   (not-found-response "NOT FOUND"))
  ([message]
   {:status 404
    :headers {}
    :body (json/write-str {:message message})}))

(defn- versioned-route
  "Prefixes a route with a version and distributes to the correct
   implementation based on version number"
  [method path versions]
  (compojure.core/make-route
    method
    (str "/:version" path)
    (fn [request]
      (let [version (get (:params request) :version)]
        (if (= \v (first version))
          (let [semantic-version (subs version 1)
                valid-versions (semver.core/sorted (keys versions))
                newest-version (first valid-versions)
                oldest-version (last valid-versions)]
            (cond (contains? versions semantic-version)
                  ((get versions semantic-version) request)

                  (semver.core/newer? semantic-version newest-version)
                  (not-found-response (str "Latest version is v"
                                           newest-version))

                  (semver.core/older? semantic-version oldest-version)
                  (not-found-response)

                  ; At this point, the version is in range.
                  ; Figure out which is the newest version that
                  ; is still less than the semantic-version.
                  :else ((get versions
                              (find-closest-version valid-versions
                                                    semantic-version))
                         request)))
          (not-found-response))))))

(defn- get-all-projects-v-1-0-0
  "Implementation V1.0.0 of the /project endpoint"
  [request]
  (json/write-str
    (persistence/get-all-roots
      (persistence/get-config
        (persistence/get-secret-from-aws
          (persistence/get-credentials-secret))))))

(defn- validate-object-id
  "Validates and normalizes a string request parameter representing an
   object ID (like a task ID or hierarchy ID). If the request is valid,
   returns the ID as a number"
  [requested-id]
  (let [parsedId (try (Long/parseLong requested-id)
                      (catch java.lang.NumberFormatException e nil))]
    (cond (nil? parsedId) nil
          (>= parsedId 0) parsedId
          :else nil)))

(defn- get-project-id-v-1-0-0
  "Implementation V1.0.0 of the /project/:id endpoint"
  [request]
  (let [raw-task-id (get (:params request) :id)
        validated-task-id (validate-object-id raw-task-id)]
    (if (some? validated-task-id)
      (let [config (persistence/get-config
                     (persistence/get-secret-from-aws
                       (persistence/get-credentials-secret)))]
        (json/write-str (persistence/load-task-by-id config validated-task-id)))
      (user-error-response (str "Invalid Task ID [" raw-task-id "].")))))

(compojure.core/defroutes backend-api
  (compojure.core/GET "/.internal/is_healthy"
                      []
                      (json/write-str {:healthy true}))
  (versioned-route :get
                   "/project"
                   {"1.0.0" get-all-projects-v-1-0-0})
  (versioned-route :get
                   "/project/:id"
                   {"1.0.0" get-project-id-v-1-0-0})
  (compojure.route/not-found (not-found-response)))

(defn -main
  "Starts a backend webserver on port 5000 to handle API requests for working with tasks"
  [& args]
  ; Note: Chose port 5000 arbitrarily based on a tutorial I was
  ;       following. It shouldn't really matter assuming we host this in
  ;       docker-compose because we can remap the port to anything.
  (run-server backend-api {:port 5000}))
