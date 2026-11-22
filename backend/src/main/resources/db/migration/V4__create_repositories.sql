CREATE TABLE IF NOT EXISTS public.repositories
(
    id                 UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    owner              VARCHAR(100) NOT NULL,
    name               VARCHAR(200) NOT NULL,
    default_branch     VARCHAR(255),
    indexed_commit_sha VARCHAR(40),
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message      TEXT,
    java_files         INTEGER      NOT NULL DEFAULT 0,
    chunk_count        INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_repositories_owner_name UNIQUE (owner, name),
    CONSTRAINT chk_repositories_status
        CHECK (status IN ('PENDING', 'INDEXING', 'READY', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_repositories_status
    ON public.repositories (status);