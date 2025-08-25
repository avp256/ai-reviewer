# AI‑Reviewer

This repository contains a prototype implementation of the **AI‑Reviewer** described in the
provided technical specification. The application is written in Java 21 using
Spring Boot 3 and demonstrates a modular, agent‑driven approach to automating
parts of the code review process.

## Features

* Accepts GitLab merge request webhook events via `/webhook/gitlab`.
* Parses the merge request metadata and extracts the Jira key from the title.
* Retrieves basic issue information from Jira to provide business context.
* Runs a series of stateless “agents” to analyse different aspects of the change:
  * `AnalystAgent` – attaches Jira context and an initial positive remark.
  * `CodeAgent` – performs simple static heuristics on file names and diff length.
  * `TestAgent` – suggests where unit tests might be needed.
  * `ArchitectureAgent` – warns about the presence of deprecated components.
* Aggregates the results from all agents into a single Markdown comment and posts it back to the merge request.
* Provides a `/health` endpoint for monitoring and liveness checks.

## Running locally

To run the application locally you will need Java 21 and Maven 3.6+.

```bash
mvn spring-boot:run
```

The service will start on port 8080. Use `ngrok` or similar if you wish to
expose the webhook endpoint publicly.

### Configuration

All integration credentials are provided via environment variables. The
following variables are recognised:

* `GITLAB_BASE_URL` – base URL to your GitLab instance, e.g. `https://gitlab.example.com`
* `GITLAB_API_TOKEN` – personal access token with API scope
* `JIRA_BASE_URL` – base URL to your Jira instance, e.g. `https://jira.example.com`
* `JIRA_USERNAME` – your Jira username (often an email)
* `JIRA_API_TOKEN` – API token or password for Jira

Example launch command:

```bash
GITLAB_BASE_URL=https://gitlab.example.com \
GITLAB_API_TOKEN=xxxxx \
JIRA_BASE_URL=https://jira.example.com \
JIRA_USERNAME=user@example.com \
JIRA_API_TOKEN=yyyyy \
mvn spring-boot:run
```

## Running with Docker

Build and run via Docker (Java 21 minimal image):

```bash
docker compose build
docker compose up -d
```

The service will be available on port 8080. Configure environment variables in docker-compose.yml or override at runtime:

```bash
GITLAB_BASE_URL=https://gitlab.example.com \
GITLAB_API_TOKEN=xxxxx \
JIRA_BASE_URL=https://jira.example.com \
JIRA_USERNAME=user@example.com \
JIRA_API_TOKEN=yyyyy \
SMTP_HOST=smtp.example.com \
SMTP_PORT=587 \
SMTP_USERNAME=smtp-user \
SMTP_PASSWORD=smtp-pass \
SMTP_AUTH=true \
SMTP_STARTTLS=true \
docker compose up -d --build
```

Logs are written to /var/log/ai-reviewer inside the container. A named volume (ai-reviewer-logs) is used to persist logs across restarts.

## Limitations & next steps

This MVP is intentionally minimal and contains many simplifications:

* It does not fetch the actual diff; instead it uses the list of changed file
  names as a proxy.
* Agents implement only rudimentary heuristics. In a real system they would
  leverage static analysis, language models and project‑specific guidelines.
* Errors are logged but not reported to administrators via email or
  messaging platforms.
* No integration tests are included. See the specification for recommended
  test cases.

Despite these limitations the structure is ready for enhancement. New agents
can be added easily by implementing the `Agent` interface and annotating the
class with `@Component`. The aggregator will automatically pick them up.
