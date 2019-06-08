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

(ns dev.chrisreyes.task-tracker.persistence-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [dev.chrisreyes.task-tracker.persistence :as persistence]))

(deftest filter-nil-values-test
  (testing "Edge cases for filter-nil-values"
    (is (= (@#'persistence/filter-nil-values {}) {})
        "filter-nil-values should leave an empty map alone")
    (let [only-1-nil {:a nil}
          only-2-nils {:duck nil :goose nil}]
      (is (= (@#'persistence/filter-nil-values only-1-nil) {})
          "fitler-nil-values should return an empty map if only value is nil")
      (is (= (@#'persistence/filter-nil-values only-2-nils) {})
          "filter-nil-values should return an empty map if all values are nil"))
    (let [only-value {:a "duck"}
          all-values {:duck "duck" :goose "!!!"}]
      (is (= (@#'persistence/filter-nil-values only-value) only-value)
          "filter-nil-values should return a map unchanged with no nil values"))
    (let [happy "clojure"
          some-values {:happy happy :sad nil}
          some-truths {:learning true :bored false :tired nil}]
      (is (= (@#'persistence/filter-nil-values some-values) {:happy happy})
          "filter-nil-values should return a map with only non-nil values")
      (is (= (@#'persistence/filter-nil-values some-truths) {:learning true
                                                             :bored false})))))

(deftest get-config-test
  (testing "Can get the config from a secret JSON dict"
    (let [secret {}
          config (persistence/get-config secret)]
      (is (= (:dbtype config) "postgres")
          (str "Configuration should be for a postgres database, "
               "even from empty secret")))
    (let [secret {:dbname "somedatabase"
                  :host "https://u.r.l"
                  :password "s0Secr3t"
                  :port "1234"
                  :username "security"
                  :ignore-me "Woo!"}
          config (persistence/get-config secret)]
      (is (= (:dbtype config) "postgres")
          "Configuration should be for a postgres database")
      (doseq [property [:dbname :host :password :port]]
        (is (= (get config property) (get secret property))
            (str "Should preserve the " property " key")))
      (is (= (:user config) (:username secret))
          "Should rename :username to :user")
      (is (not (contains? config :ignore-me))
          "Should ignore irrelevant keys"))
    (let [secret {:dbtype "mysql"}
          config (persistence/get-config secret)]
      (is (= (:dbtype config) "postgres")
          (str "Should always have config for postgres database, "
               "ignoring secret :dbtype")))))

(deftest task->db-row-test
  (testing "Can convert a task to the appropriate database schema"
    (let [hierarchy-id 3
          issue-link "Something"
          estimated-time-minutes 2
          actual-time-minutes 1
          task {:actual-time-minutes actual-time-minutes
                :estimated-time-minutes estimated-time-minutes
                :hierarchy-node {:hierarchy-id hierarchy-id}
                :issue-link issue-link}
          db-row (persistence/task->db-row task)]
      (is (= (:actual_time_minutes db-row) actual-time-minutes)
          "Should put actual time into actual_time_minutes column")
      (is (= (:estimated_time_minutes db-row) estimated-time-minutes)
          "Should put estimated time into estimated_time_minutes column")
      (is (= (:issue_link db-row) issue-link)
          "Should put issue link into issue_link column")
      (is (= (:hierarchy_id db-row) hierarchy-id)
          "Should pull out hierarchy ID into hierarchy_id column"))))

(deftest hierarchy-node->db-row-test
  (testing "Can convert a hierarchy-node to the approriate database schema"
    (let [hierarchy-id 4
          this-numerator 5
          this-denominator 6
          next-sibling-numerator 7
          next-sibling-denominator 8
          hierarchy-node {:hierarchy-id hierarchy-id
                          :this-numerator this-numerator
                          :this-denominator this-denominator
                          :next-numerator next-sibling-numerator
                          :next-denominator next-sibling-denominator}
          db-row (persistence/hierarchy-node->db-row hierarchy-node)]
      (is (= (:hierarchy_id db-row) hierarchy-id)
          "Should put hierarchy ID into hierarchy_id column")
      (is (= (:numerator db-row) this-numerator)
          "Should put own numerator into numerator column")
      (is (= (:denominator db-row) this-denominator)
          "Should put own denominator into denominator column")
      (is (= (:next_sibling_numerator db-row) next-sibling-numerator)
          "Should put numerator of next sibling into next_sibling_numerator")
      (is (= (:next_sibling_denominator db-row) next-sibling-denominator)
          (str "Should put denominator of next sibling into "
               "next_sibling_denominator")))))

(deftest db-row->task-test
  (testing (str "Can convert a database record into the "
                "canonical task representation")
    (let [actual-time 9
          estimated-time 10
          hierarchy-id 11
          issue-link "http://some.url/1234"
          task-id 12
          db-row {:actual_time_minutes actual-time
                  :estimated_time_minutes estimated-time
                  :hierarchy_id hierarchy-id
                  :issue_link issue-link
                  :task_id task-id}
          task (persistence/db-row->task db-row)]
      (is (= (:actual-time-minutes task) actual-time)
          (str "Should have preserved the actual_time_minutes "
               "into the actual-time-minutes field"))
      (is (= (:estimated-time-minutes task) estimated-time)
          (str "Should have preserved the estimated_time_minutes "
               "into the estimated-time-minutes field"))
      (is (= (get-in task [:hierarchy-node :hierarchy-id])
             hierarchy-id)
          "Should load the hierarchy_id into the hierarchy-node")
      (is (= (:issue-link task) issue-link)
          "Should have preserved the task_id into the task-id field")
      (is (= (:task-id task) task-id)
          "Should have preserved the task_id into the task-id field"))))

(deftest db-row->hierarchy-node-test
  (testing (str "Can convert a database record into the canonical "
                "hierarchy-node representation.")
    (let [hierarchy-id 13
          this-numerator 14
          this-denominator 15
          next-sibling-numerator 16
          next-sibling-denominator 17
          num-children 18
          db-row {:hierarchy_id hierarchy-id
                  :denominator this-denominator
                  :numerator this-numerator
                  :next_sibling_denominator next-sibling-denominator
                  :next_sibling_numerator next-sibling-numerator
                  :num_children num-children}
          hierarchy-node (persistence/db-row->hierarchy-node db-row)]
      (is (= (:hierarchy-id hierarchy-node) hierarchy-id)
          "Should preserve hierarchy_id in hierarchy-id field")
      (is (= (:this-denominator hierarchy-node) this-denominator)
          "Should preserve denominator in this-denominator field")
      (is (= (:this-numerator hierarchy-node) this-numerator)
          "Should preserve numerator in this-numerator field")
      (is (= (:next-denominator hierarchy-node) next-sibling-denominator)
          (str "Should preserve next sibling's denominator "
               "in next-denominator field"))
      (is (= (:next-numerator hierarchy-node) next-sibling-numerator)
          (str "Should preserve next sibling's numerator "
               "in next-numerator field"))
      (is (= (:num-children hierarchy-node) num-children)
          (str "Should preserve number of children "
               "in num-children field")))))

(deftest get-or-create-hierarchy-id-test
  (testing "Can get hierarchy-id from hierarchy-node"
    (let [db-id 42
          new-id 4092
          saved-id 1024
          new-hierarchy {}
          saved-hierarchy {:hierarchy-id saved-id}]
      (with-redefs [persistence/get-hierarchy-id-from-db (constantly
                                                           db-id)
                    persistence/insert-hierarchy-node (constantly
                                                        new-id)]
        (is (= (persistence/get-or-create-hierarchy-id
                 nil
                 new-hierarchy)
               db-id)
            "If hierarchy-node doesn't already have ID, query for match")
        (is (= (persistence/get-or-create-hierarchy-id
                 nil
                 saved-hierarchy)
               saved-id)
            (str "Should prioritize ID from already saved "
                 "hierarchy-node before querying for match")))
      (with-redefs [persistence/get-hierarchy-id-from-db (constantly
                                                           nil)
                    persistence/insert-hierarchy-node (constantly
                                                        new-id)]
        (is (= (persistence/get-or-create-hierarchy-id
                 nil
                 new-hierarchy)
               new-id)
            (str "If hierarchy-node doesn't already have ID, and "
                 "no match is found, save the hierarchy to the "
                 "DB and use that ID"))
        (is (= (persistence/get-or-create-hierarchy-id
                 nil
                 saved-hierarchy)
               saved-id)
            (str "Should prioritize ID from already saved "
                 "hierarchy-node before saving to the database"))))))

(deftest save-task-test
  (testing "Can save a task to persistent storage"
    (let [hierarchy-id 3]
      (with-redefs [jdbc/db-transaction* (fn [config f] (f config))
                    persistence/get-or-create-hierarchy-id (constantly hierarchy-id)
                    persistence/insert-task (fn [_ task _] task)]
        (let [task {:hierarchy-node {:this-numerator 40
                                     :this-denominator 1
                                     :next-numerator 41
                                     :next-denominator 1}
                    :issue-link "http://issue.tracker/system/43110"
                    :estimated-time-minutes 90
                    :actual-time-minutes 358}
              saved-task (persistence/save-task nil task nil)]
          (is (= (get-in saved-task [:hierarchy-node :hierarchy-id]) hierarchy-id)
              "Should have saved the hierarchy-id into the hierarchy node")
          (doseq [prop [:this-numerator
                        :this-denominator
                        :next-numerator
                        :next-denominator]]
            (is (= (get-in saved-task [:hierarchy-node prop])
                   (get-in task [:hierarchy-node prop]))
                (str "Should have preserved the "
                     prop
                     " property from the hierarchy-node")))
          (doseq [prop [:issue-link
                        :estimated-time-minutes
                        :actual-time-minutes]]
            (is (= (get saved-task prop) (get task prop))
                (str "Should have preserved the "
                     prop
                     " property in the original task"))))))))
