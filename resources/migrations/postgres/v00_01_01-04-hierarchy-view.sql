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
 * Defines a function to determine if one task is a subtask of another
 */
create or replace function is_subtask (
    parent hierarchy,
    subtask hierarchy
) returns boolean as $is_subtask$
    DECLARE
        parent_val          bigint;
        parent_sibling_val  bigint;
        subtask_val         bigint;
        subtask_sibling_val bigint;
    BEGIN
        parent_val = parent.numerator * subtask.denominator;
        subtask_val = subtask.numerator * parent.denominator;

        parent_sibling_val = parent.next_sibling_numerator * subtask.denominator;
        subtask_sibling_val = subtask.numerator * parent.next_sibling_denominator;

        return (parent_val < subtask_val and
                subtask_sibling_val < parent_sibling_val);
    END;
$is_subtask$ language plpgsql
