# RAG Product Search

AI-powered product search using Retrieval-Augmented Generation (RAG). Ask a question in plain language — like *"waterproof jacket under $50 for hiking"* — and get back a synthesized, natural-language answer backed by an actual product catalog, not a keyword match.

Built as a Java full-stack project to demonstrate Spring AI, vector search, and constraint-aware LLM prompting end-to-end.

## What it does

A user submits a natural-language query. The system:
1. Embeds the query and runs a similarity search against a Postgres/pgvector-backed product catalog
2. Retrieves the top-K most relevant products, ranked by similarity
3. Passes the retrieved products and the original query to an LLM, which synthesizes a natural-language recommendation — constrained to only recommend products that genuinely match what was asked (product type, price, use case)

## Tech stack

**Backend**
- Java 21, Spring Boot 4.1.1, Maven
- Spring AI 2.0.0 — `ChatClient`, `VectorStore`, `QuestionAnswerAdvisor`
- PostgreSQL + pgvector — vector similarity search
- OpenRouter (OpenAI-compatible API) — embeddings and chat completion, using free-tier models

**Frontend**
- React / Next.js — search interface

**Infrastructure**
- Docker Compose — local Postgres/pgvector instance

## Architecture

```
                     ┌──────────────────┐
   HTTP request ───► │  SearchController │
                     └────────┬─────────┘
                              │
                     ┌────────▼─────────┐
                     │   SearchService   │
                     └────────┬─────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                                ▼
      ┌───────────────┐              ┌──────────────────┐
      │  VectorStore   │              │    ChatClient     │
      │ (similarity    │◄─────────────┤ + QuestionAnswer  │
      │   search)      │  retrieved   │      Advisor      │
      └───────┬────────┘  documents   └─────────┬─────────┘
              │                                  │
              ▼                                  ▼
    ┌───────────────────┐              ┌──────────────────┐
    │ Postgres/pgvector  │              │  OpenRouter (LLM) │
    │  (product catalog) │              │  synthesis        │
    └────────────────────┘              └──────────────────┘

  ProductIngestionRunner (startup-only)
  reads products.json → embeds → populates vector_store
```

## Project structure

Organized by layer, with ingestion deliberately separated out:

```
com.nirupama.ragproductsearch
├── model/          # JPA entity (Product)
├── repository/      # Direct DB access for inspection/debugging (not part of the live search path)
├── ingestion/        # One-time startup ingestion — separate from request-time logic
├── controller/       # REST endpoints
├── service/          # Retrieval + LLM synthesis logic
├── dto/               # API request/response shapes, decoupled from the DB entity
└── config/
```

**Why `ingestion/` is separate from `service/`**: ingestion runs once at application startup via a `CommandLineRunner`; everything in `service/` runs per-request. Splitting on "when it runs" rather than lumping everything backend-side into one package kept each piece's responsibility unambiguous.

**Why `SearchResult`/`SearchResponse` DTOs exist separately from the `Product` entity**: the API's public response shape shouldn't be tightly coupled to the database entity — if the entity changes internally, the API contract doesn't have to change with it.

## Key design decisions

- **pgvector over Pinecone/Chroma** — chosen specifically because it reinforces PostgreSQL rather than adding a new, unrelated dependency, and it runs free and local via Docker with no external account or vendor lock-in.
- **One embedding document per product** (name + description + category + price combined into a single chunk) rather than splitting fields separately — appropriate at this catalog size; would need revisiting for longer, multi-paragraph product descriptions.
- **Explicit, structured system prompt** rather than relying on `QuestionAnswerAdvisor`'s default template — the model is required to state the constraints it's identified (product type, price, use case) *before* answering, which surfaces reasoning it would otherwise skip straight past.

