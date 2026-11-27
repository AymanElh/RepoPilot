# RepoPilot

RepoPilot helps you explore a Java codebase on GitHub. Give it a repository URL, wait for indexing to finish, then search the source code or ask a question about how it works. Results link to specific lines at the indexed GitHub commit so you can verify what you find.

## Local configuration

You’ll need Java 21, Docker running with Compose, Node.js 24.15 or later in the 24.x series, npm 11, and a Gemini API key. [Create a Gemini API key](https://aistudio.google.com/api-keys)

From the project root, copy the [environment template](backend/.env.example) using your shell.

```bash
cp backend/.env.example backend/.env
```

```powershell
Copy-Item backend/.env.example backend/.env
```

Open `backend/.env` in a text editor and fill in `DB_PASSWORD` and `GOOGLE_API_KEY`. Use the current password if you already initialized PostgreSQL. Keep each setting on one plain `NAME=value` line without surrounding quotes, inline comments, or `export`. For a new database, use a password with letters, digits, hyphens, and underscores so Compose and Spring read it the same way. The example sets `GEMINI_CHAT_MODEL` to `gemini-3.1-flash-lite`. `GITHUB_TOKEN` is optional. Git ignores the filled-in `.env` file. [Gemini model reference](https://aistudio.google.com/docs/models/gemini-3.1-flash-lite)

### How the settings reach the app

- **Docker Compose** reads `backend/.env` to fill in `DB_PASSWORD` when started from `backend/`. It passes that password to PostgreSQL as `POSTGRES_PASSWORD`. [Compose configuration](backend/compose.yaml)
- **Spring Boot** imports `backend/.env` as properties when launched from the project root or `backend/`. Placeholders in `application.yaml` use those values to configure PostgreSQL and Google. Variables set in the Spring process environment take precedence. [Spring configuration](backend/src/main/resources/application.yaml)
- **IntelliJ** passes the variables in your Spring Boot run configuration to the Spring process. Your existing IntelliJ setup still works. You can also let Spring read `backend/.env` by setting the run configuration’s working directory to the project root or `backend/`. Compose runs as a separate process and reads its own environment or `backend/.env`.
- **Angular** calls Spring through the development proxy. The Gemini key stays on the backend.

The old Bash command `read -rsp` prompted for a password without displaying it. `printf '\n'` added a newline after the hidden input. Filling in `.env` replaces those steps in the manual setup. The launcher still prompts if the required values are missing from the file and environment.

### Optional GitHub token

RepoPilot calls GitHub’s REST API during indexing to look up the default branch, resolve its latest commit, and download the source archive. You can leave `GITHUB_TOKEN` blank for public repositories. If you index repositories frequently or hit a GitHub rate-limit error, a personal access token raises the usual primary limit from 60 requests per hour per IP address to 5,000 requests per hour for the authenticated user. [GitHub API rate limits](https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api#about-primary-rate-limits)

1. In GitHub, open **Settings → Developer settings → Personal access tokens → Fine-grained tokens**. Create a token with read-only **Contents** access for the repositories it needs. [Create a fine-grained token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-fine-grained-personal-access-token) · [Archive permissions](https://docs.github.com/en/rest/repos/contents#download-a-repository-archive-zip)
2. Set `GITHUB_TOKEN` in `backend/.env` or in the Spring Boot run configuration in IntelliJ. Restart Spring and retry indexing.

`GITHUB_TOKEN` authenticates requests to GitHub. `GOOGLE_API_KEY` is the separate credential for Gemini chat and embeddings.

## Quick start with the launcher

On Linux or macOS with Bash 4.3 or later and `curl`, run this from the project root. Windows users can run it inside WSL with Docker integration and the prerequisites installed there, or use the native PowerShell steps below.

```bash
./start.sh
```

The script starts PostgreSQL through Compose, installs the locked npm dependencies, and starts Spring Boot through the Maven Wrapper. Once Spring is healthy, it starts Angular and prints the URL.

Open [RepoPilot’s repository library](http://localhost:4200/repositories). Press **Ctrl+C** in the launcher terminal to stop Angular and Spring Boot. PostgreSQL stays running with your indexed repositories. Stop it later with `docker stop repopilot-postgres`.

## Manual setup

### 1. Start PostgreSQL and Spring Boot

After filling in `backend/.env`, run the commands for your shell from the project root.

**Linux, macOS, or WSL**

```bash
cd backend
docker compose up -d --wait postgres
./mvnw spring-boot:run
```

**Windows PowerShell**

```powershell
Set-Location backend
docker compose up -d --wait postgres
.\mvnw.cmd spring-boot:run
```

Spring runs on port `8080`. Compose exposes PostgreSQL with pgvector on port `5433`, and Flyway applies the database migrations when Spring starts.

### 2. Start Angular

Open a second terminal at the project root.

**Linux, macOS, or WSL**

```bash
cd frontend
npm ci
npm start
```

**Windows PowerShell**

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd start
```

The Angular development proxy forwards `/api` requests to Spring on port `8080`. Open [RepoPilot’s repository library](http://localhost:4200/repositories).

## Index and explore a repository

1. Paste a public repository’s main GitHub URL, such as `https://github.com/AymanElh/energytracker-springboot-microservices`, and click **Index repository**.
2. Wait for the status to reach **Ready**, then select **Open chat**.
3. Under **Search indexed code**, enter a class name such as `ParallelDataSimulator` or describe the code you want to find. Open a result to see the matching lines on GitHub.
4. Under **Ask about this repository**, select a provider and ask a question such as “Which Java class generates simulated energy readings?” Read the answer and check its source links.

## AI providers

| Task | Provider | Configuration |
| --- | --- | --- |
| Generate answers | Gemini, the default chat provider | `GOOGLE_API_KEY` and `GEMINI_CHAT_MODEL` |
| Generate answers locally | Ollama | `http://localhost:11434` and `qwen2.5-coder:1.5b` by default |
| Embed Java source and queries | Gemini | `gemini-embedding-001` with 1,536 dimensions, using `GOOGLE_API_KEY` |

To use Ollama, start its local server and download the configured model. Then choose **Ollama** in the provider dropdown on the repository page. [Ollama model page](https://ollama.com/library/qwen2.5-coder:1.5b)

```bash
ollama pull qwen2.5-coder:1.5b
```

The chat provider controls answer generation. Indexing, search, and source retrieval for chat still use Google embeddings, so those steps require a Gemini key with either chat provider. Set the Spring property `repopilot.ai.default-provider` to `OLLAMA` if you want Ollama preselected. [Spring configuration](backend/src/main/resources/application.yaml)

## How it works

- **Indexing.** Spring resolves the repository’s default branch to a commit, downloads its archive, extracts `.java` files, and splits them into chunks. Spring AI generates embeddings and stores the chunks, vectors, file paths, and line ranges in PostgreSQL with pgvector. Angular polls the repository status while indexing runs in the background.
- **Search.** Spring embeds the query and retrieves similar chunks from the selected repository. The results include the code snippets and links to the indexed commit.
- **Chat.** Spring retrieves relevant chunks and passes them with the question to Gemini or Ollama. Angular renders the returned answer as Markdown and lists the source links.

The [Spring backend](backend/) serves the API and handles indexing and retrieval. The [Angular frontend](frontend/) provides the repository library and the chat and search page.

## API reference

With Spring running, open [Swagger UI](http://localhost:8080/swagger-ui/index.html) or read the [OpenAPI JSON](http://localhost:8080/v3/api-docs).

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/repositories` | Start indexing. Send `{"url":"https://github.com/owner/repository"}`. |
| `GET` | `/api/repositories` | List indexed repositories and their statuses. |
| `GET` | `/api/repositories/{owner}/{name}` | Get one repository’s status and indexed commit. |
| `GET` | `/api/repositories/{owner}/{name}/search?q=...` | Search indexed code. |
| `POST` | `/api/repositories/{owner}/{name}/chat` | Ask a `question`. Optionally send `provider` as `GEMINI` or `OLLAMA`. |
| `GET` | `/api/ai/providers` | Get the available chat providers and the default. |

## Troubleshooting

- **The frontend cannot load repositories.** Check [Spring health](http://localhost:8080/actuator/health) and verify Angular is running on port `4200`.
- **Indexing failed.** Read the error on the repository row. Check that the URL points to a repository with Java source, the database is running, and the Google key has embedding quota. After resolving the error, submit the repository URL again.
- **Chat reports a provider error.** For Gemini, check the key, model ID, and quota. For Ollama, check that the server is running and the model was pulled.

## Contributing

Before submitting a change, run the backend tests from `backend/` and the frontend tests and production build from `frontend/`. For UI or API changes, repeat the indexing, search, and chat flow locally.

```bash
# backend/
./mvnw test
```

```bash
# frontend/
npm test -- --watch=false
npm run build
```
