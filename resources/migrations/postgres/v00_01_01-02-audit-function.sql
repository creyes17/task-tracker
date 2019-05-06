/*
 * Creates the auditing functions used in triggers
 */

create or replace function audit_table () returns trigger as $audit_table$
    DECLARE
        op_id bigint;
        audit_tablename text;
        table_id text;
    BEGIN
        audit_tablename = TG_TABLE_NAME || '_audit';
        table_id = TG_TABLE_NAME || '_id';

        EXECUTE $op-id$
            select max(operation_id) + 1 into op_id from %s;
        $op-id$
        using audit_tablename;

        EXECUTE $$
            insert into %s (operation_id,
                            %s,
                            modified_by,
                            field_name,
                            old_value,
                            new_value)
            values (
                %d,
                %s,
                current_user(),
                %s,
                %s,
                %s
            );
        $$
        using audit_tablename,
              table_id,
              op_id,
              OLD.; -- How do I get the table_id from OLD?
    END;
$audit_table$ language plpgsql;
