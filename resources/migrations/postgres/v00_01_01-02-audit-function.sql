/*
 * audit_table
 *
 * Use in an UPDATE trigger to write to an audit table.
 * Assumes:
 *  - There exists a table named <TABLE_NAME>_audit
 *  - There exists a column in table named <TABLE_NAME>_id containing a bigint
 *  - There exists a column in table named last_modified_by
 */
create or replace function audit_table () returns trigger as $audit_table$
    DECLARE
        audit_tablename text;   -- The audit table to update
        col_name        text;   -- Used to check each column in the record
        new_val         text;   -- The new value for col_name in the record
        old_val         text;   -- The previous value for col_name in the record
        op_id           bigint; -- The operation ID
        record_id       bigint; -- The ID for the record being updated
        table_id        text;   -- The ID column in the record being updated
    BEGIN
        audit_tablename = quote_ident(TG_TABLE_NAME || '_audit');

        -- Get a new operation ID
        execute 'select coalesce(max(operation_id), 0) + 1 from ' || audit_tablename
            into op_id;

        table_id = quote_ident(TG_TABLE_NAME || '_id');

        execute 'select $1.' || table_id using NEW into record_id;

        -- Loop over every column and save an audit record for any that were changed
        for col_name in select json_object_keys(row_to_json(OLD)) loop
            execute 'select cast($1.' || quote_ident(col_name) || ' as text)'
                using OLD into old_val;
            execute 'select cast($1.' || quote_ident(col_name) || ' as text)'
                using NEW into new_val;

            if (old_val is null and new_val is not null) or
                    (old_val is not null and new_val is null) or
                    (old_val is not null and new_val is not null and old_val <> new_val) then
                execute format($$
                    insert into %s (operation_id,
                                    %s,
                                    modified,
                                    modified_by,
                                    field_name,
                                    old_value,
                                    new_value)
                    values ($1, $2, $3, $4, $5, $6, $7);
                $$, audit_tablename, table_id)
                using op_id,
                      record_id,
                      NEW.last_modified,
                      NEW.last_modified_by,
                      col_name,
                      old_val,
                      new_val;
            end if;
        end loop;
        return NEW;
    END;
$audit_table$ language plpgsql;
