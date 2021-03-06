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
          (is (<= 200 status 299) "Should return a 2XX status without trailing slash")
          (is (= body (json/write-str [root-task]))
              "Should have returned a json string with the response of persistence/get-all-roots")))
      (testing "trailing slash should work"
        (let [response (api-get backend-api "/v1.0.0/project/")
              status (:status response)
              body (:body response)]
          (is (<= 200 status 299) "Should return a 2XX status with trailing slash")
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
        (let [response (api-get backend-api "/v9000.0.1/project")
              status (:status response)]
          (is (= status 404)
              ; Note: This test will spuriously fail after the release of version 9000.0.1 (if that ever happens).
              "Should return a 404 status for future versions."))))))

(deftest project-id-endpoint
  (let [specific-task-id 17
        specific-task {:task-id specific-task-id
                       :issue-link "Some task"
                       :hierarchy-node {:this-numerator 3
                                        :this-denominator 5
                                        :hierarchy-id 8}}]
    (with-redefs [persistence/load-task-by-id (fn [config task-id]
                                                (cond (not (= config "TEST-CONFIG"))
                                                      "Did not stub out config!"
                                                      (= task-id specific-task-id)
                                                      specific-task
                                                      :else nil))
                  persistence/get-config (fn [secret]
                                           (if (= secret "TEST-SECRET")
                                             "TEST-CONFIG"
                                             "Did not stub out secret!"))
                  persistence/get-secret-from-aws (fn [secret-name]
                                                    (if (= secret-name "TEST-SECRET-NAME")
                                                      "TEST-SECRET"
                                                      "Did not stub out secret name!"))
                  persistence/get-credentials-secret (constantly "TEST-SECRET-NAME")]
      (testing "/project/:id route"
        (let [response (api-get backend-api (str "/v1.0.0/project/" specific-task-id))
              status (:status response)
              body (:body response)]
          (is (<= 200 status 299) "Should return a 2XX status without a trailing slash")
          (is (= body (json/write-str specific-task))
              "Should have returned a json string with the response of persistence/load-task-by-id")))
      (testing "route should work with trailing slash"
        (let [response (api-get backend-api (str "/v1.0.0/project/" specific-task-id "/"))
              status (:status response)
              body (:body response)]
          (is (<= 200 status 299) "Should return a 2XX status with a trailing slash")
          (is (= body (json/write-str specific-task))
              "Should have returned a json string with the response of persistence/load-task-by-id")))
      (testing "input validation"
        (let [status-for-input #(:status (api-get backend-api (str "/v1.0.0/project/" %)))]
          (is (= 400 (status-for-input "alpha"))
              "Should return a 400 response for alphabetical task IDs")
          (is (= 400 (status-for-input "1%3B%20drop%20table%20task%3B"))
              "Should return a 400 response for attempted SQL injection")
          (is (<= 200
                  (status-for-input (str (inc (Integer/MAX_VALUE))))
                  299)
              "Should return a 2XX response even when using an ID greater
               than the max integer value")
          (is (<= 200
                  (status-for-input "9223372036854775806")
                  299)
              "Should return a 2XX response when using an ID less
               than the max value for the bigint type in postgres")
          (is (<= 200
                  (status-for-input "9223372036854775807")
                  299)
              "Should return a 2XX response even when using an ID equal
               to the max value for the bigint type in postgres")
          (is (= 400 (status-for-input "9223372036854775808"))
              "Should return a 400 response when using an ID greater
               than the max value for the bigint type in postgres")
          (is (= 400 (status-for-input "-3"))
              "Should return a 400 response for negative task IDs")))
      (testing "versioning"
        (let [response (api-get backend-api (str "/v1.0.0/project/" specific-task-id))
              status (:status response)]
          (is (<= 200 status 299) "Endpoint was introduced in v1.0.0. That version should still work"))
        (let [response (api-get backend-api (str "/v0.9.0/project/" specific-task-id))
              status (:status response)]
          (is (= 404 status) "Endpoint was introduced in v1.0.0. Earlier versions should not work"))))))

(deftest main-test
  (testing "Has main function"
    (is (some? -main) "Core should define a -main function")))
