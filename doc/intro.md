# Hierarchy Nodes

Based on https://arxiv.org/pdf/0806.3115.pdf

You can create a new hierarchy with:

```clojure
(require '[task-tracker.hierarchy :as tracker :refer [create-root add-child]])

(let [root (tracker/create-root 3)]
      ; #task_tracker.core.HierarchyNode{:this-numerator 3,
      ;                                  :this-denominator 1,
      ;                                  :next-numerator 4,
      ;                                  :next-denominator 1,
      ;                                  :num-children 0}
  (tracker/add-child root))
; {:root #task_tracker.core.HierarchyNode{:this-numerator 3,
;                                         :this-denominator 1,
;                                         :next-numerator 4,
;                                         :next-denominator 1,
;                                         :num-children 1},
;  :child #task_tracker.core.HierarchyNode{:this-numerator 7,
;                                          :this-denominator 2,
;                                          :next-numerator 11,
;                                          :next-denominator 3,
;                                          :num-children 0}}
```

# Tasks

You can create a new task with:

```clojure
(require '[task-tracker.hierarchy :as hierarchy :refer [create-root]]
         '[task-tracker.task :as task :refer [create-task]])

(task/create-task "This is an issue link" (hierarchy/create-root 3))
; #task_tracker.task.Task{:task-id nil,
;                         :hierarchy-node #task_tracker.hierarchy.Node{
;                           :hierarchy-id nil,
;                           :this-numerator 3,
;                           :this-denominator 1,
;                           :next-numerator 4,
;                           :next-denominator 1,
;                           :num-children 0},
;                         :issue-link "This is an issue link",
;                         :estimated-time-minutes nil,
;                         :actual-time-minutes nil}
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
