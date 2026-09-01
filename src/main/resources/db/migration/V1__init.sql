-- Migrations are append-only: never edit this file once it has run anywhere —
-- add V2__<description>.sql instead (springboot-data §2).

create table books (
    id         uuid         primary key,
    title      varchar(200) not null,
    isbn       varchar(17)  not null,
    copies     integer      not null,
    version    bigint       not null default 0,
    created_at timestamptz  not null,
    updated_at timestamptz  not null,

    -- The ISBN is the natural key of a book, so it is unique at the database
    -- level (springboot-data §3); BookEntity mirrors this with @UniqueConstraint.
    constraint uk_books_isbn unique (isbn)
);

create table loans (
    id         uuid        primary key,
    book_id    uuid        not null,
    member_id  uuid        not null,
    due_date   date        not null,
    status     varchar(20) not null,
    version    bigint      not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,

    constraint fk_loans_book foreign key (book_id) references books (id)
);

-- Supports the availability count and the duplicate-active-loan check in LoanService.
create index idx_loans_book_id_status on loans (book_id, status);
create index idx_loans_member_id_book_id_status on loans (member_id, book_id, status);
