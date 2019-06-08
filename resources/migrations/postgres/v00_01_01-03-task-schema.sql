/* Copyright (C) 2019  Christopher R. Reyes
 *
 * This file is part of Task Tracker.
 *
 * Task Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Task Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Task Tracker.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    created_by text not null,
    last_modified timestamp(2) with time zone not null default current_timestamp(2),
    last_modified_by text not null,
    deleted timestamp(2) with time zone,
    deleted_by text
);

create table if not exists task_audit (
    operation_id bigint not null,
    task_id bigint not null references task (task_id),
    modified timestamp(2) not null default current_timestamp(2),
    modified_by text not null,
    field_name varchar(64) not null,
    old_value text,
    new_value text
);

create trigger tg_task_audit
    after update on task
    for each row
    execute function audit_table();

create trigger tg_task_last_modified
    before update on task
    for each row
    execute function set_last_modified();
