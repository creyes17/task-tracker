(ns task-tracker.core
  (:gen-class))

(defprotocol Persistence
  "The interface between this task tracker app and long-term storage"
  (list-projects [this user] "Lists the top-level projects.")
  (create-task [this user task] "Creates a new task, persisting it to long-term storage.")
  (read-task [this user task-id] "Retrieves a task from long-term storage along with any associated subtasks.")
  (update-task [this user task-id task] "Updates a task that already exists in long-term storage.")
  (delete-task [this user task-id] "Removes a task from long-term storage."))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
