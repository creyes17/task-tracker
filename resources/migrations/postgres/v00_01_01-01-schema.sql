/* 
 * Defines the schema for the task-tracker.
 * Written for PostgreSQL
 */
create table if not exists hierarchy (
    hierarchy_id bigint generated always as identity primary key,
    numerator bigint not null,
    denominator bigint not null,
    next_sibling_numerator bigint not null,
    next_sibling_denominator bigint not null,
    unique(numerator, denominator)
)
