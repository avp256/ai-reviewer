# AI‑Reviewer — Task Decomposition Checklist

This document tracks implementation progress for the MVP, derived from docs/specification.md and docs/requirements.md. It is organized in a sensible sequence (phases) and can be used to mark completion status.

Legend: [x] = Done, [ ] = Todo, [~] = In progress, [opt] = Optional

## Phase 0 — Foundation (existing baseline)
- [x] Initialize Java 21 + Spring Boot project (skeleton, SOLID structure)
- [x] Expose /health endpoint
- [x] Implement webhook controller: POST /webhook/gitlab (accepts MR events payload)
- [x] Implement AggregatorService to run registered agents sequentially
- [x] Implement basic agents (Analyst, Code, Test, Architecture) as stateless components
- [x] Implement ReviewProcessor orchestrator (parse MR payload, get Jira/GitLab data, run agents, post comment)
- [x] Implement basic Jira client (fetch issue; minimal fields)
- [x] Implement basic GitLab client (post comment; fetch changed files list)
- [x] Implement AIReviewComment → Markdown formatting (close to spec template)
- [x] Add minimal unit tests (AggregatorService test)
- [x] Provide basic README with local run instructions

## Phase 1 — MVP blockers (ship this first)
1. Comment format and consistency
   - [x] Ensure header uses "[AI-Reviewer | Summary]"
   - [x] Include sections: Jira Context, Done well, Issues with Recommendation, Unit test advice
   - [x] Add global "Source" note if needed to match spec strictly (per-issue source already present)

2. Notifications (error alerting)
   - [x] Add Notifier module (choose simplest: SMTP email)
   - [x] Wire ReviewProcessor to send admin notification on failure (MR id, reason, timestamp)
   - [opt] Add a service comment in MR when review fails (non-blocking)

3. Logging and rotation
   - [x] Add Logback configuration for file appender with size-based rotation (e.g., 10MB x 10 files)
   - [x] Log key pipeline events and errors

4. Dockerization
- [x] Add Dockerfile (JDK 21 base; minimal image)
- [x] Add docker-compose.yml with restart: always and environment variables
- [x] Document Docker run in README

5. Integration hardening (GitLab payload)
   - [x] Validate event type (MR open/update) and safely ignore others
   - [x] Improve parsing fallback paths and null-safety for payload fields

6. Tests expansion
- [x] Unit tests for ReviewProcessor parsing (title → Jira key; projectId; iid)
- [x] Unit tests for JiraClient (parsing, error handling, empty config)
- [x] Unit tests for GitLabClient (URL formation, headers, error handling)
- [x] Unit tests for AIReviewComment.toMarkdown() format correctness
- [x] Unit tests for agents’ heuristics (issues/advice based on inputs)
- [x] Smoke/E2E test with mocked clients: webhook → one aggregated comment posted
- [x] Unit tests for notifier (email formatting, error handling)

7. Integration documentation
- [x] Create docs/INTEGRATION.md: GitLab webhook setup, Jira auth, curl examples
- [x] Update README with supported versions (GitLab CE 14.0.0, Jira 7.8.1) and clear setup

## Phase 2 — Security and model adapter
1. Secrets and safety
   - [ ] Provide .env example and document env usage; ensure no secrets in code/logs
   - [ ] Implement secrets filtering before LLM calls (regex remove tokens/passwords/URLs)
   - [ ] Add "no-code-mode" flag to disable sending code to LLM (MVP safeguard)
   - [ ] AES encryption utility for passwords in env/config (key via system var/keystore)

2. LLM adapter (OpenAI o3) — MVP-ready interface, mockable
   - [ ] Define LLMAdapter interface (timeouts, retries, token limits)
   - [ ] Implement OpenAI client (can be stubbed for MVP with config to disable external calls)
   - [ ] Integrate adapter into agents where applicable (keeping current heuristics)
   - [ ] Configuration flags for timeouts/retries/no-code-mode

3. Architecture/Docs context access
   - [ ] Add simple local file reader for .md/.pdf (size limit, path config)
   - [ ] Allow ArchitectureAgent to reference Tech Radar and guidelines text

## Phase 3 — Performance, diffs, and resilience
1. Diff quality
   - [ ] Optionally fetch real diffs from GitLab for better analysis (keep fallback to pseudo-diff)

2. Time budgets and concurrency
   - [ ] Global pipeline timeout (≤ 15 minutes for ≤ 500 lines changed)
   - [ ] Per-call timeouts: Jira/GitLab/LLM
   - [ ] Verify agents are stateless; configure thread pool if needed
   - [ ] Test 5 concurrent MR processing (no degradation)

3. Docker resilience
   - [ ] Confirm compose uses restart: always
   - [ ] Document/review behavior on crash; verify logs preserved

4. Log retention
   - [ ] Ensure rotation policy aligns with storage retention policy; document location and cleanup

## Phase 4 — CI/CD and documentation polish
1. CI/CD integration (optional complement to webhook)
   - [ ] Provide .gitlab-ci.yml template snippet to call AI-Reviewer

2. Documentation
   - [ ] Extend README: Docker, env, troubleshooting, limitations, security mode
   - [ ] Docs for comment format with examples and mapping to spec
   - [ ] Deployment guide with volumes/log policies
   - [ ] Security guide (secrets, encryption, filters, logging policy)
   - [ ] Usage examples: 3–5 synthetic MR scenarios with example comments/screenshots

## Cross-cutting — Quality gates
- [ ] Achieve ≥70% unit test coverage on core logic
- [ ] Keep code free from hard-coded secrets
- [ ] Ensure on-prem friendly configuration (no external calls when disabled)

## Traceability (spec mapping quick refs)
- GitLab MR handling: spec 424–433; Tasks Phase 1/5, Phase 3/1
- Comment format: spec 455–479; Tasks Phase 1/1 and Phase 4/2
- Notifications: spec 481–486; Tasks Phase 1/2
- Security/secrets: spec 488–499, 687–691; Tasks Phase 2/1, Phase 4/4
- Performance/reliability: spec 500–511, 692–696; Tasks Phase 3/2–3
- Logging/rotation: spec 517–523; Tasks Phase 1/3, Phase 3/4
- Dockerization: spec 491–499, 618–623, 693–695; Tasks Phase 1/4, Phase 3/3
- LLM adapter: spec 409–454, 612–629; Tasks Phase 2/2
- Documentation access: spec 535–542; Tasks Phase 2/3
- Tests: spec 706–709; Tasks Phase 1/6 and Cross-cutting
