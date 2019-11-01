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
 * Sets up a trigger so that whenever a task is deleted, the associated hierarchy node is also deleted.
 */

create or replace function delete_related_hierarchy () returns trigger as $delete_related_hierarchy$
    BEGIN
        delete from
            hierarchy
        where
            hierarchy.hierarchy_id = OLD.hierarchy_id;

        return OLD;
    END;
$delete_related_hierarchy$ language plpgsql;

create trigger tg_task_delete_cascade
    after delete on task
    for each row
    execute function delete_related_hierarchy();
