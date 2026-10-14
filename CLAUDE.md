# CLAUDE.md

Clarity is a self-hosted, single-owner overview of bank accounts. It reads
accounts read-only via PSD2 through Enable Banking, categorizes transactions,
detects fixed costs and shows the money left until the next salary. Business
spaces add contracts, and invoices read from mailboxes that are matched to
incoming payments.

- `api/`: Java 25, Spring Boot 4, Hibernate/JPA, Postgres, Lombok, Gradle;
  Jakarta Mail for mailboxes, PDFBox and Tesseract (OCR) for invoices
- `panel/`: React 19, TypeScript, Vite, Tailwind 4, shadcn/ui (Radix),
  TanStack Query, react-router, i18next, Recharts, Vitest, ESLint
- `docker-compose.yml`: postgres + api + panel (nginx); only the panel is
  published, on `127.0.0.1:8080`

## Commands

| Where | Task | Command |
|---|---|---|
| `api/` | Build + test (what CI runs) | `./gradlew build` |
| `api/` | Tests only | `./gradlew test` |
| `api/` | Run locally (reads `api/config.ini`) | `./gradlew bootRun` |
| `panel/` | Dev server (proxies `/v1` to `localhost:8080`) | `npm run dev` |
| `panel/` | Demo without backend (in-browser fake api) | `npm run demo` |
| `panel/` | What CI runs | `npm run lint && npm test && npm run build` |

Local database:
`docker run -d -p 127.0.0.1:5432:5432 -e POSTGRES_USER=clarity -e POSTGRES_PASSWORD=clarity -e POSTGRES_DB=clarity postgres:18`

Reading scanned invoices locally needs `tesseract` with the `deu` and `eng`
languages installed; the Docker image brings its own.

## Style (both parts)

- 2 spaces, max 80 columns, LF, no final newline (`.editorconfig`).
- Follow the code around you. Comments explain *why*, are written in English
  and go in Javadoc/JSDoc above a class or method, not inside method bodies.
- Commit messages: short imperative sentence describing the user-visible
  effect ("Add a page for subscriptions and fixed costs"), no prefixes.
- Never commit real bank data, invoices, mailbox credentials, `.env`,
  `config.ini` secrets or keys (`secrets/` holds the Enable Banking `.pem`,
  `clarity.key` encrypts mailbox passwords), not even in tests.
- Document accumulated knowledge as individual markdown files under `docs/`,
  one focused topic per file (like `docs/enable-banking.md`), rather than
  letting it pile up only in code.

## api

- Package by feature under `de.lukasbreuer.clarity.api` (`banking`,
  `business`, `owner`, `space`, `transaction`, …); HTTP controllers live
  separately under `web/<feature>`.
- Configuration comes from `config.ini` (INI, commons-configuration2), not
  Spring properties. In Docker, `docker-entrypoint.sh` generates it from env
  vars. Config classes implement `configuration.Configuration` and are exposed
  as beans in a `*Module` `@Configuration` class.
- Mailbox passwords are stored encrypted with `secret.SecretBox`; its key file
  (`[secrets] key_file`) lives outside the database and is backed up with it.
- Schema is managed by Hibernate `hbm2ddl.auto=update`; there are no
  migrations. Entity changes must stay compatible with existing data.
- Lombok idioms: `@Getter @Accessors(fluent = true)` (accessors are `amount()`,
  not `getAmount()`), static factory `create(...)` via
  `@AllArgsConstructor(staticName = "create")` /
  `@RequiredArgsConstructor(staticName = "create")`, protected no-arg
  constructor for JPA. Classes are `final` where possible.
- Everything is asynchronous: repositories extend `DatabaseRepository` and
  return `CompletableFuture` with `@Async`; controllers return
  `CompletableFuture<ApiResponse>`. Only `@Modifying` queries are synchronous.
- Responses: `ApiResponse.success(Map)` / `ApiResponse.error(code)`. JSON keys
  are snake_case. Error codes are grouped: `1xxx` auth/setup/settings, `2xxx`
  banking; the panel translates them via `<section>.errors.<code>` in the
  locale files, so add a translation for every new code.
- Request bodies are parsed with `ApiRequestBody` (org.json), not DTOs.
- All routes live under `/v1` (servlet context path).
- Security: every endpoint requires the owner session unless annotated
  `@PublicEndpoint` (fail closed). State-changing requests need the
  `X-Clarity-Request: 1` header (`RequestGuardFilter`). Sessions are HttpOnly
  cookies, only the token hash is stored. Don't weaken any of this.
- Tests are plain JUnit 5 unit tests of pure logic (categorizer, recurring
  detection, parsing, payment matching); no Spring context. Mail tests run
  against GreenMail.

## Java code style (follow these — they override default habits)

The reviewer who checks these rules:

```
                 .-""""""""-.
               .'   ______   '.
              /   .' 150 '.    \        "Bad code style.
             |   /  lines  \    |        Long functions,
             |  |   max!!   |   |        logic in constructor,
              \  '.______.'    /         no lombok usage.
               '.   ____     .'          Fix!"
            ____'-.(____).-'____
           /    @Getter        \
          /  @Accessors(fluent) \
         |   ┌───────────────┐   |
         |   │ static void?  │   |
         |   │     NEIN.     │   |
         |   └───────────────┘   |
          \_____________________/
              ||           ||
              ||  .-----.  ||
             _||_ | 80  | _||_
            (____)|cols |(____)
                  '-----'

     ☕ "-> { return ...; }  ✗"
```

- **Object-oriented.** Model behaviour as classes with state + instance
  methods.
- **Avoid static functions** — they work against OO. Prefer an instance
  method on the object that owns the data. Legitimate exceptions kept on
  purpose: `static final` constants, and **static factory readers** that
  construct the object they return — the house style is Lombok
  `@RequiredArgsConstructor(staticName = "create")` plus hand-written
  factories like `ApiResponse.success()/error(...)`, `ApiRequestBody.of(...)`
  and `CategoryRule.forCounterparty(...)`. Use `new` only inside those
  factories.
- **No one-line `if`s.** Always use braces, even for a single statement.
- **No brace-block lambdas.** A lambda body stays a single expression — never
  a `{ … }` block, and especially never one with a `return` inside. When the
  body needs statements, extract it into a named private method and pass a
  method/expression reference instead (e.g.
  `.thenApply(payments -> paymentsResponse(...))`, not
  `.thenApply(payments -> { … return …; })`). This keeps the async chains
  readable and the logic testable.
- **No comments inside method bodies.** Keep methods short enough to read on
  their own; put the explanation in a Javadoc `/** … */` ABOVE the method (as
  `OwnerInterceptor` and `RequestGuardFilter` do).
- **All code comments in English.** Every comment and doc comment is written
  in English. Only user-facing strings are localized (see below). If you touch
  a file with a German comment, translate it while you're there.
- **Short methods and classes.** Split when they grow; one job each. Rule of
  thumb: **no Java file over ~150 lines**, methods ideally **5–10 lines**.
  When a flow needs more, decompose it into a chain of small private methods
  rather than one long method (see how `OwnerInterceptor.preHandle` hands off
  to `isPublic`, `findValidSession` and `touch`). Treat these as hard smells,
  not hard limits.
- **Descriptive, unique names** for classes and methods — no generic or
  duplicated names. **No one-letter variable names** (loop counters
  included); the name says what it holds. Unnamed lambda/`catch` params
  written as `_` are the intentional exception (Java unnamed variables).
- **Avoid boilerplate.** No scaffolding "for later", no repeated patterns a
  shared helper removes — extract the repetition. The async plumbing
  (`DatabaseRepository.generateAvailableId`, `ApiResponse.future()`) exists so
  controllers don't re-implement it; reuse it.
- **Lombok-first accessors.** `@Accessors(fluent = true)` so getters are
  `name()` not `getName()`; match this everywhere.
- **Localize everything.** The api returns no user-facing text: it answers
  with data and error codes, and every displayed string lives in the panel's
  `src/locales/de.json` and `en.json`.
- **JSON uses `snake_case` keys**, both in the HTTP request/response bodies
  (`bank_name`, `valid_until`, `next_date`) and in the locale files. Locale
  files are **nested objects** per section (`{"setup": {"errors":
  {"1001": …}}}`), looked up with dot-separated keys
  (`t("setup.errors.1001")`).
- **Package by feature, NOT by layer.** Under `de.lukasbreuer.clarity.api`
  the top level is domain-based (`banking/`, `business/`, `owner/`, `space/`,
  `transaction/`); a feature's state, entity, repository and logic live
  TOGETHER, and subfolders are sub-domains, never technical types
  (`banking/sync/`, `business/invoice/`, `owner/session/`,
  `transaction/category/`). Cross-cutting infrastructure lives in
  `configuration/`, `database/`, `hashing/`, `log/`, `secret/`. The one
  deliberate boundary layer is `web/` — the HTTP edge, organized by route
  group (`web/banking/`, `web/business/`, `web/security/`), not by type. Do
  NOT introduce generic `models/`, `services/`, `controllers/` folders.

## panel

- Import alias `@/` → `src/`. Pages in `src/pages/<area>/`, shared pieces in
  `src/components/`, shadcn primitives in `src/components/ui/` (generated,
  edit sparingly), pure logic in `src/lib/` with `*.test.ts` next to it.
- All api calls go through `src/lib/api.ts` (types mirror the api's JSON);
  data fetching through TanStack Query hooks in `src/hooks/`.
- Demo mode (`VITE_DEMO=true`) routes every call to `src/demo/demo-server.ts`
  with data from `demo-data.ts` (business data in `demo-business.ts`). **A new
  endpoint needs a demo handler too**, otherwise the GitHub Pages demo breaks.
  Actions impossible in the demo return error `9000`.
- Every user-facing string goes into both `src/locales/en.json` and `de.json`.
  Keep texts short.
- Put calculations (budget, statistics, periods) in `src/lib/` as pure
  functions and test them with Vitest; components stay thin.
- The panel loads nothing from third parties (fonts are bundled via
  fontsource). Keep it that way.