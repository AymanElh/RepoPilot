CREATE TABLE IF NOT EXISTS public.vector_store (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding VECTOR(1536)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_hnsw_idx
ON public.vector_store
USING hnsw (embedding vector_cosine_ops);
