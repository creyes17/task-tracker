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
    [clojure.set]
    [clojure.test :refer :all]
    [dev.chrisreyes.task-tracker.core :refer :all]
    [dev.chrisreyes.task-tracker.persistence :as persistence]))

(defn api-call
  "Executes a get-request against the given Compojure API"
  ([api]
   (api-call api "/" :get nil))
  ([api resource]
   (api-call api resource :get nil))
  ([api resource method]
   (api-call api resource method nil))
  ([api resource method params]
   (api {:request-method method
         :uri resource
         :params params})))

(deftest backend-api-test
  (testing "Defines a backend api"
    (is (some? backend-api) "Core should define the backend API"))
  (testing "Has a health check"
    (let [response (api-call backend-api "/.internal/is_healthy")
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
      (testing "GET /project route"
        (let [response (api-call backend-api "/v1.0.0/project")
              status (:status response)
              body (:body response)]
          (is (<= 200 status 299) "Should return a 2XX status without trailing slash")
          (is (= body (json/write-str [root-task]))
              "Should have returned a json string with the response of persistence/get-all-roots")))
      (testing "trailing slash should work"
        (let [response (api-call backend-api "/v1.0.0/project/")
              status (:status response)
              body (:body response)]
          (is (<= 200 status 299) "Should return a 2XX status with trailing slash")
          (is (= body (json/write-str [root-task]))
              "Should have returned a json string with the response of persistence/get-all-roots")))
      (testing "versioning"
        (let [response (api-call backend-api "/v1.0.0/project")
              status (:status response)]
          (is (<= 200 status 299) "Should return a 2XX status for version 1.0.0"))
        (let [response (api-call backend-api "/v0.9.9/project")
              status (:status response)]
          (is (= status 404)
              "Should return a 404 status for versions before 1.0.0"))
        (let [response (api-call backend-api "/v9000.0.1/project")
              status (:status response)]
          (is (= status 404)
              ; Note: This test will spuriously fail after the release of version 9000.0.1 (if that ever happens).
              "Should return a 404 status for future versions."))))))

(deftest get-project-id-endpoint
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
      (testing "GET /project/:id route"
        (let [response (api-call backend-api (str "/v1.0.0/project/" specific-task-id))
              status (:status response)
              body (:body response)]
          (is (<= 200 status 299) "Should return a 2XX status without a trailing slash")
          (is (= body (json/write-str specific-task))
              "Should have returned a json string with the response of persistence/load-task-by-id")))
      (testing "route should work with trailing slash"
        (let [response (api-call backend-api (str "/v1.0.0/project/" specific-task-id "/"))
              status (:status response)
              body (:body response)]
          (is (<= 200 status 299) "Should return a 2XX status with a trailing slash")
          (is (= body (json/write-str specific-task))
              "Should have returned a json string with the response of persistence/load-task-by-id")))
      (testing "input validation"
        (let [status-for-input #(:status (api-call backend-api (str "/v1.0.0/project/" %)))]
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
        (let [response (api-call backend-api (str "/v1.0.0/project/" specific-task-id))
              status (:status response)]
          (is (<= 200 status 299) "Endpoint was introduced in v1.0.0. That version should still work"))
        (let [response (api-call backend-api (str "/v0.9.0/project/" specific-task-id))
              status (:status response)]
          (is (= 404 status) "Endpoint was introduced in v1.0.0. Earlier versions should not work"))))))

(deftest post-project-id-endpoint
  (let [test-key "post-project-id-endpoint-test"]
    (with-redefs [persistence/save-task (fn [config task username]
                                          (if (= config "TEST-CONFIG")
                                            (assoc task
                                                   :_test-key test-key
                                                   :_test-user username)
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
      (testing "POST /project/:id route"
        (let [username "dev.chrisreyes.task-tracker.core-test.test-user"
              specific-task-id 17
              specific-task {:task-id specific-task-id
                             :issue-link "Updated task"
                             :hierarchy-node {:this-numerator 3
                                              :this-denominator 5
                                              :hierarchy-id 8}
                             :estimated-time-minutes 45
                             :actual-time-minutes 30}
              response (api-call backend-api
                                 (str "/v1.0.0/project/" specific-task-id)
                                 :post
                                 {:task specific-task
                                  :username username})
              body (json/read-str (:body response)
                                  :key-fn keyword)]
          (is (<= 200 (:status response) 299)
              "Should have returned the saved task")
          (is (= (get body :_test-user) username)
              (str "Should have passed username through to the database. "
                   "Expected result to have _test-user and for the value "
                   "to be " username " like we supplied. Instead got:\n"
                   body))
          (is (= (get body :_test-key) test-key)
              (str "Should have called persistence/save-task to save "
                   "to the database. Expected result to have _test-key "
                   "and for the value to be " test-key ". Instead got:\n"
                   body))
          (is (= (dissoc body :_test-key :_test-user) specific-task)
              (str "Should have saved the whole task to the database.\n"
                   "Expected " specific-task ".\nInstead got: "
                   (dissoc body :_test-key :_test-user)))))
      (testing "input validation"
        ; TODO: Validate fields. (What's required, what's not? Avoid SQL injection)
        ; TODO: Figure out how to check that another function was called
        ; (Assume the application already has the hierarchy data)
        (is (= 1 1))))))

(deftest validate-task-test
  (testing "Prevents invalid IDs"
    (let [valide-hierarchy-node {:hierarchy-id 25
                                 :next-denominator 1
                                 :next-numerator 7
                                 :num-children 4
                                 :this-denominator 1
                                 :this-numerator 6}
          valid-task {:task-id "22"
                      :issue-link "Some issue"
                      :hierarchy-node valide-hierarchy-node
                      :estimated-time-minutes 99
                      :actual-time-minutes 100}
          valid-after-modification? (fn [property value]
                                      (let [updated-task (assoc valid-task
                                                                property value)]
                                        (= updated-task (validate-task updated-task))))
          invalid-after-modification? (fn [property value]
                                        (= nil
                                           (validate-task (assoc valid-task
                                                                 property value))))]

      (is (= valid-task (validate-task valid-task))
          "Should accept a valid task")
      (is (invalid-after-modification? :task-id "alpha")
          "Should reject tasks with alphabetical task IDs")
      (is (valid-after-modification? :task-id (str (inc (Integer/MAX_VALUE))))
          "Should accept task with ID greater than the max integer value")
      (is (invalid-after-modification? :task-id "1%3B%20drop%20table%20task%3B")
          "Should reject attempted SQL injection in the task ID")
      (is (valid-after-modification? :task-id "9223372036854775806")
          "Should accept task when using an ID less than the max value
           for the bigint type in postgres")
      (is (valid-after-modification? :task-id "9223372036854775807")
          "Should accept task when using an ID equal to the max value for
          the bigint type in postgres")
      (is (invalid-after-modification? :task-id "9223372036854775808")
          "Should reject task when using an ID greater than the max value
           for the bigint type in postgres")
      (is (invalid-after-modification? :task-id "-3")
          "Should reject tasks with negative task IDs"))))

(deftest main-test
  (testing "Has main function"
    (is (some? -main) "Core should define a -main function")))
