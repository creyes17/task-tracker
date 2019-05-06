/*
 * Creates the schema for tasks (along with an associated audit table)
 */

create table if not exists task (
    task_id bigint generated always as identity primary key,
    hierarchy_id bigint not null unique references hierarchy (hierarchy_id),
    estimated_time_minutes bigint not null default 0,
    actual_time_minutes bigint not null default 0,
    issue_link text,
    /* Auditing columns */
    created timestamp(2) with time zone not null default current_timestamp(2),
    created_by text not null default current_user,
    last_modified timestamp(2) with time zone not null default current_timestamp(2),
    last_modified_by text not null default current_user,
    deleted timestamp(2) with time zone,
    deleted_by text
);

create table if not exists task_audit (
    operation_id bigint not null,
    task_id bigint not null references task (task_id),
    modified timestamp(2) not null default current_timestamp(2),
    modified_by text not null default current_user,
    field_name varchar(64) not null,
    old_value text,
    new_value text
);
