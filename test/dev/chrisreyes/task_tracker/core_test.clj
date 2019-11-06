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
  dev.chrisreyes.task-tracker.core-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [dev.chrisreyes.task-tracker.persistence :as persistence]
    [dev.chrisreyes.task-tracker.core :refer :all]))

(defn api-get
  "Executes a get-request against the given Compojure API"
  ([api]
   (api-get api "/"))
  ([api resource]
   (api-get api resource nil))
  ([api resource params]
   (api {:request-method :get
         :uri resource
         :params params})))

(deftest backend-api-test
  (testing "Defines a backend api"
    (is (some? backend-api) "Core should define the backend API"))
  (testing "Has a health check"
    (let [response (api-get backend-api "/.internal/is_healthy")
          status (:status response)
          body (:body response)]
      (is (<= 200 status 299) "Should return a 2XX status")
      (is (= true (get (json/read-str body) "healthy"))
          "Should report to be healthy"))))

(deftest project-endpoint
  (let [root-task {:task-id 1
                   :issue-link "Project root"
                   :hierarchy-node {:this-numerator 1
                                    :this-denominator 1
                                    :hierarchy-id 1}}]
    (with-redefs [persistence/get-all-roots (fn [config]
                                              (if (= config "TEST-CONFIG")
                                                [root-task]
                                                "Did not stub out config!"))
                  persistence/get-config (fn [secret]
                                           (if (= secret "TEST-SECRET")
                                             "TEST-CONFIG"
                                             "Did not stub out secret!"))
                  persistence/get-secret-from-aws (fn [secret-name]
                                                    (if (= secret-name "TEST-SECRET-NAME")
                                                      "TEST-SECRET"
                                                      "Did not stub out secret name!"))
                  persistence/get-credentials-secret (constantly "TEST-SECRET-NAME")]
      (testing "/project route"
        (let [response (api-get backend-api "/v1.0.0/project")
              status (:status response)
              body (:body response)]
          (is (<= 200 status 299) "Should return a 2XX status")
          (is (= body (json/write-str [root-task]))
              "Should have returned a json string with the response of persistence/get-all-roots")))
      (testing "versioning"
        (let [response (api-get backend-api "/v1.0.0/project")
              status (:status response)]
          (is (<= 200 status 299) "Should return a 2XX status for version 1.0.0"))
        (let [response (api-get backend-api "/v0.9.9/project")
              status (:status response)]
          (is (= status 404)
              "Should return a 404 status for versions before 1.0.0"))
        (let [response (api-get backend-api "/v1.0.1/project")
              status (:status response)]
          (is (= status 404)
              "Should return a 404 status for versions after 1.0.0"))))))

(deftest main-test
  (testing "Has main function"
    (is (some? -main) "Core should define a -main function")))
