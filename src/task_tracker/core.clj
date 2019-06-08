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

(ns task-tracker.core
  (:gen-class)
  (:require [task-tracker.hierarchy :as hierarchy]
            [task-tracker.persistence :as persistence]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [db-config (persistence/get-config
                    (persistence/get-secret-from-aws
                      (persistence/get-credentials-secret)))
        this-numerator (persistence/get-next-root db-config)
        next-root (hierarchy/create-root this-numerator)
        task {:hierarchy-node next-root
              :issue-link (str "Hello " this-numerator " World")
              :estimated-time-minutes 30}]
    (persistence/save-task db-config task (System/getenv "USER"))))
