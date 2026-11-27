/** Types mirroring the backend DTOs. Keep in sync with the Spring records. */

export type RepositoryStatus = 'PENDING' | 'INDEXING' | 'READY' | 'FAILED';

export type ChatProvider = 'OLLAMA' | 'GEMINI';

/** RepositoryResponse */
export interface Repository {
  id: string;
  owner: string;
  /** "owner/name" */
  fullName: string;
  defaultBranch: string | null;
  indexedCommitSha: string | null;
  status: RepositoryStatus;
  errorMessage: string | null;
  javaFiles: number;
  chunkCount: number;
  /** ISO-8601 instant */
  createdAt: string;
  updatedAt: string;
}

/** POST /api/repositories body (RepositoryController.IndexRequest) */
export interface IndexRepositoryRequest {
  url: string;
}

/** SourceReference */
export interface SourceReference {
  filePath: string;
  startLine: number;
  endLine: number;
  githubUrl: string;
}

/** POST /api/repositories/{owner}/{name}/chat body (ChatRequest) */
export interface ChatRequest {
  question: string;
  /** Omit to let the backend use its default provider. */
  provider?: ChatProvider;
}

/** ChatResponse */
export interface ChatResponse {
  answer: string;
  sources: SourceReference[];
}

/** CodeSearchResult (the backend field is spelled `store`, not `score`). */
export interface CodeSearchResult {
  filePath: string;
  startLine: number;
  endLine: number;
  content: string;
  store: number | null;
  githubUrl: string;
}

/** GET /api/ai/providers (ProviderController.ProvidersResponse) */
export interface ProvidersResponse {
  defaultProvider: ChatProvider;
  providers: ChatProvider[];
}

/** Error codes the backend can return in `{ error, message }`. */
export type ApiErrorCode =
  | 'VALIDATION_FAILED'
  | 'MALFORMED_REQUEST'
  | 'UNSUPPORTED_PROVIDER'
  | 'BAD_REQUEST'
  | 'CONFLICT'
  | 'GITHUB_NOT_FOUND'
  | 'GITHUB_RATE_LIMITED'
  | 'GITHUB_ERROR'
  | 'REPOSITORY_NOT_FOUND'
  | 'REPOSITORY_NOT_READY'
  | 'INDEXING_QUEUE_FULL'
  | 'PROVIDER_UNAVAILABLE'
  | 'AI_SERVICE_UNAVAILABLE'
  | 'SERVICE_UNREACHABLE'
  | 'INTERNAL_ERROR'
  /** Frontend-only: the backend could not be reached at all. */
  | 'NETWORK_ERROR'
  /** Frontend-only: an error response without a recognisable body. */
  | 'UNKNOWN';

/** Raw error body returned by the backend. */
export interface ApiErrorBody {
  error: string;
  message: string;
  timestamp?: string;
  /** Present on VALIDATION_FAILED: field name -> message. */
  fields?: Record<string, string>;
}
