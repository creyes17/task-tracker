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

# Postgres

You can start the postgres instance via `docker-compose up`.

You can connect to the running postgres instance via `docker-compose exec postgres psql`.

The schema (so far) is as follows:

```
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
```
