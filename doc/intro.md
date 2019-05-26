# Hierarchy Nodes

Based on https://arxiv.org/pdf/0806.3115.pdf

You can create a new hierarchy with:

```clojure
(require '[task-tracker.hierarchy :as hierarchy :refer [create-root add-child]])

; Create the root hierarchy node of a project:
(def root (hierarchy/create-root 3))
; {:this-numerator 3,
;  :this-denominator 1,
;  :next-numerator 4,
;  :next-denominator 1,
;  :num-children 0}

; Create the hierarchy node representing a subtask of that root
(hierarchy/add-child root)
; {:root {:this-numerator 3,
;         :this-denominator 1,
;         :next-numerator 4,
;         :next-denominator 1,
;         :num-children 1},
;  :child {:this-numerator 7,
;          :this-denominator 2,
;          :next-numerator 11,
;          :next-denominator 3,
;          :num-children 0}}
```

# Tasks

You can create a new task with:

```clojure
(require '[task-tracker.hierarchy :as hierarchy :refer [create-root]])

; Assume you're using the "root" defined above.
(def task {:actual-time-minutes 10
           :estimated-time-minutes 30
           :hierarchy-node root
           :issue-link "This is an issue link"})
```

# Persistence

To save a task, you'll need to:

1. Get the database configuration so you can connect

   ```clojure
   ; A database configuration is a map with the following keys:
   {:dbname   "The name of the database within postgres"
    :dbtype   "Currently the only supported type is 'postgres'"
    :host     "The URL of the machine hosting the postgres instance"
    :password "The password for the :username of the database"
    :port     "The port number on the host machine to use to connect"
    :user     "The log-in name for a user of the database"}

   ; You can create a secret in AWS Secrets Manager to hold the
   ; configuration. If you do, set up your AWS credentials in your
   ; environment and then run:
   (require '[task-tracker.persistence :as persistence
                                       :refer [get-config
                                               get-secret-from-aws])

   ; (Assuming you named the secret "secret-name" in AWS)
   (def config (persistence/get-config
                 (persistence/get-secret-from-aws "secret-name")))
   ```

1. Create the task map (see above)
   - Note: The only required key from the task for saving a task is `:hierarchy-node`.
   - Default values for `:actual-time-minutes` and `:estimated-time-minutes` are both `0`.
   - The schema doesn't require an `:issue-link`
1. Use the `task-tracker.persistence/save-task` function

   ```clojure
   (require '[task-tracker.persistence :as persistence :refer [save-task])

   ; Assume you're using the "task" defined above
   ; Assume you're using the "config" defined above
   ; The "username" will be used for auditing purposes
   (persistence/save-task config task "username")
   ;{:hierarchy {:this-numerator 3
   ;             :this-denominator 1
   ;             :next-numerator 4
   ;             :next-denominator 1
   ;             :num-children 0
   ;             :hierarchy-id 1}
   ; :task {:last_modified #inst "2019-05-26T15:02:55.310000000-00:00"
   ;        :deleted nil
   ;        :issue-link "This is an issue link"
   ;        :task-id 1
   ;        :estimated-time-minutes 30
   ;        :created #inst "2019-05-26T15:02:55.310000000-00:00"
   ;        :actual-time-minutes 10
   ;        :last_modified_by "username"
   ;        :hierarchy_id 1
   ;        :deleted_by nil
   ;        :hierarchy-node {:this-numerator 3
   ;                         :this-denominator 1
   ;                         :next-numerator 4
   ;                         :next-denominator 1
   ;                         :num-children 0
   ;                         :hierarchy-id 1}
   ;        :created_by "username"}}
   ```

   ```postgres
   dev_chrisreyes_tasktracker=# select * from task where task.task_id = 1;
     task_id | hierarchy_id | estimated_time_minutes | actual_time_minutes |      issue_link       |          created          | created_by |       last_modified       | last_modified_by | deleted | deleted_by
    ---------+--------------+------------------------+---------------------+-----------------------+---------------------------+------------+---------------------------+------------------+---------+------------
           1 |            1 |                     30 |                  10 | This is an issue link | 2019-05-26 15:02:55.31+00 | username   | 2019-05-26 15:02:55.31+00 | username         |         |

   dev_chrisreyes_tasktracker=# select * from hierarchy where hierarchy.hierarchy_id = 1;
     hierarchy_id | numerator | denominator | next_sibling_numerator | next_sibling_denominator
    --------------+-----------+-------------+------------------------+--------------------------
                1 |         3 |           1 |                      4 |                        1
   ```

# Postgres

You can start the postgres instance via `docker-compose up`.

You can connect to the running postgres instance via `docker-compose exec postgres psql`.

The schema (so far) is as follows:

```
dev_chrisreyes_tasktracker=# \dt
          List of relations
 Schema |    Name    | Type  | Owner
--------+------------+-------+--------
 public | hierarchy  | table | creyes
 public | task       | table | creyes
 public | task_audit | table | creyes

dev_chrisreyes_tasktracker=# \d hierarchy
                                Table "public.hierarchy"
          Column          |  Type  | Collation | Nullable |           Default
--------------------------+--------+-----------+----------+------------------------------
 hierarchy_id             | bigint |           | not null | generated always as identity
 numerator                | bigint |           | not null |
 denominator              | bigint |           | not null |
 next_sibling_numerator   | bigint |           | not null |
 next_sibling_denominator | bigint |           | not null |
Indexes:
    "hierarchy_pkey" PRIMARY KEY, btree (hierarchy_id)
    "hierarchy_numerator_denominator_key" UNIQUE CONSTRAINT, btree (numerator, denominator)

dev_chrisreyes_tasktracker=# \d task
                                            Table "public.task"
         Column         |            Type             | Collation | Nullable |           Default
------------------------+-----------------------------+-----------+----------+------------------------------
 task_id                | bigint                      |           | not null | generated always as identity
 hierarchy_id           | bigint                      |           | not null |
 estimated_time_minutes | bigint                      |           | not null | 0
 actual_time_minutes    | bigint                      |           | not null | 0
 issue_link             | text                        |           |          |
 created                | timestamp(2) with time zone |           | not null | CURRENT_TIMESTAMP(2)
 created_by             | text                        |           | not null |
 last_modified          | timestamp(2) with time zone |           | not null | CURRENT_TIMESTAMP(2)
 last_modified_by       | text                        |           | not null |
 deleted                | timestamp(2) with time zone |           |          |
 deleted_by             | text                        |           |          |
Indexes:
    "task_pkey" PRIMARY KEY, btree (task_id)
    "task_hierarchy_id_key" UNIQUE CONSTRAINT, btree (hierarchy_id)
Foreign-key constraints:
    "task_hierarchy_id_fkey" FOREIGN KEY (hierarchy_id) REFERENCES hierarchy(hierarchy_id)
Referenced by:
    TABLE "task_audit" CONSTRAINT "task_audit_task_id_fkey" FOREIGN KEY (task_id) REFERENCES task(task_id)
Triggers:
    tg_task_audit AFTER UPDATE ON task FOR EACH ROW EXECUTE PROCEDURE audit_table()
    tg_task_last_modified BEFORE UPDATE ON task FOR EACH ROW EXECUTE PROCEDURE set_last_modified()

dev_chrisreyes_tasktracker=# \d task_audit
                                  Table "public.task_audit"
    Column    |              Type              | Collation | Nullable |       Default
--------------+--------------------------------+-----------+----------+----------------------
 operation_id | bigint                         |           | not null |
 task_id      | bigint                         |           | not null |
 modified     | timestamp(2) without time zone |           | not null | CURRENT_TIMESTAMP(2)
 modified_by  | text                           |           | not null |
 field_name   | character varying(64)          |           | not null |
 old_value    | text                           |           |          |
 new_value    | text                           |           |          |
Foreign-key constraints:
    "task_audit_task_id_fkey" FOREIGN KEY (task_id) REFERENCES task(task_id)
```
