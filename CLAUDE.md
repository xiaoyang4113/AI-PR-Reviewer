# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI PR Review 助手 — an AI-powered code review tool that analyzes GitHub Pull Requests and provides intelligent feedback. Built for the 七牛云 XEngineer 暑期实训营 (72-hour hackathon, 2026-05-29 ~ 2026-05-31).

## Architecture

```
frontend/   → Vue 3 + Vite + Tailwind CSS (SPA)
backend/    → Java 17 + Spring Boot 3.2 + MyBatis-Plus
```

**Backend layering:**
- `controller/` — REST endpoints，直接返回 `ApiResponse<T>` 不包装 `ResponseEntity`，状态码由 GlobalExceptionHandler 管控
- `service/` — Business logic interfaces: `ReviewService` (orchestration), `GitHubService` (PR diff fetch), `AiAnalysisService` (LLM analysis)
- `service/impl/` — Implementation classes
- `mapper/` — MyBatis-Plus mapper interfaces (`ReviewTaskMapper`, `ReviewCommentMapper`) + XML mapping files in `resources/mapper/`
- `model/entity/` — Database entities (`ReviewTask`, `ReviewComment`)
- `model/dto/` — Request/response DTOs (`ReviewCreateRequest`, `ReviewResultDTO`, `ApiResponse`, `AiAnalysisResult`, `CommentDTO`, `GitHubDiffResult`)
- `model/enums/` — Enumerations (`ReviewStatus`, `RiskLevel`)
- `model/constant/` — System constants (`ReviewConstants` — pagination, field limits, HTTP headers, API field names)
- `config/` — CORS (`WebConfig`), RestTemplate (`RestTemplateConfig`), MyBatis-Plus pagination (`MyBatisPlusConfig`), typed properties (`GitHubProperties`, `AiProperties`)
- `exception/` — `BusinessException` + `GlobalExceptionHandler`

**Key flow:** User submits repo URL + PR number → backend fetches diff from GitHub API → sends diff to DeepSeek (OpenAI-compatible) API with structured JSON prompt → stores results in DB → returns to frontend.

## Common Commands

### Backend (run from `backend/`)
```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ReviewServiceTest
```

### Frontend (run from `frontend/`)
```bash
# Install dependencies
npm install

# Dev server (proxies /api to localhost:8080)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Environment Variables

Backend reads from `.env` or system env:
- `GITHUB_TOKEN` — GitHub personal access token (optional, increases API rate limit)
- `AI_API_KEY` — DeepSeek API key (required for real analysis)
- `AI_API_BASE` — AI endpoint (default: `https://api.deepseek.com/v1`)
- `AI_MODEL` — Model name (default: `deepseek-v4-flash`，追求质量可换 `deepseek-v4-pro`)

## Database

- **Dev & Prod:** MySQL 8 — two tables: `review_tasks` and `review_comments` (1:N via `task_id` FK with CASCADE delete/update)
- Schema DDL: `backend/sql/schema.sql` (includes UNIQUE INDEX on repo_url+pr_number for dedup)
- ORM: MyBatis-Plus 3.5.7 with XML mapper files in `resources/mapper/`
- Task timeout: tasks stuck in PROCESSING > 5 min are auto-ignored on resubmit

## Mock Fallback

When AI API is unavailable, `AiAnalysisService` returns hardcoded mock data so the UI can still be demonstrated. The `mockMode` flag in `ReviewResultDTO` triggers a prominent amber warning banner in the frontend explaining the situation and how to fix it.

## Frontend Features

- Dark theme, responsive layout (Tailwind CSS)
- PR input form with client-side validation
- Skeleton loading animation during AI analysis
- CRITICAL (red) / WARNING (amber) / INFO (blue) risk badges
- Side-by-side code diff (problem code vs optimized code)
- Review history grid with pagination ("load more" button)
- Click history items to re-view details

## Project Status

All code submitted. Pending: Demo video recording + README video link.

## Competition PR Rules

Each PR must be single-purpose with: title (one line), functional description, implementation approach, and test instructions. Do not batch all code into one commit.
