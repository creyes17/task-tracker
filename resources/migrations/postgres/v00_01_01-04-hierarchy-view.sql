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
