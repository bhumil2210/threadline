# CLAUDE.md — Threadline

An interactive website that teaches concurrency by animating **which thread runs what**, step by step, on a thread-lane timeline. Java first (CompletableFuture, executors, locks, virtual threads), then the equivalent constructs in Go and Rust.

- **Audience:** engineers aged 21–25 who can read code but don't yet have a mental model of interleaving.
- **Differentiator:** real thread lanes (not abstract boxes), and every scenario verified against real execution in CI.
- **Builder:** solo, open source on GitHub.

## Core architecture rule

Every scenario is **data**. Hand-written scenarios (v1) and recorded traces (v2) both compile to **one timeline schema**, and a single player component renders it. Never special-case a scenario in the renderer; extend the schema instead.

Flow: scenario YAML + MDX prose → Astro content collection (Zod schema) at build → Astro static page per scenario → player island animates timeline, highlighted code and shared state → CI runs a real program per scenario and asserts the YAML still matches reality.

## Stack

Goal: fast first load and a 60 fps player on mid-range phones. Every page ships static HTML; the only JS is the player island.

| Layer | Choice |
| --- | --- |
| Runtime | Node 22.22+ (Astro 7 requirement), pnpm |
| Site | Astro 7 + TypeScript strict, static output, one page per scenario (e.g. `/java/completablefuture-thread-hopping`) |
| Player island | Svelte 5 (runes), `client:visible`. The only client JS on the page |
| Timeline | Hand-written SVG. Static parts (lanes, axis, blocks) rendered at build; no Canvas/WebGL, no d3 |
| Animation | One `requestAnimationFrame` loop owning `t`. Per frame, write only a few attributes (reveal clip width, cursor `transform`); never re-render the block list per frame |
| Code highlighting | Shiki 4, at build time, one `<span>` per line so the player toggles a class. Zero highlighter JS on the client |
| Prose | MDX (Astro's default Sätteri pipeline) |
| Validation | Zod 4 via content collection schemas. Build-time only, never shipped to the client |
| Navigation | Astro prefetch on hover + cross-document View Transitions between scenarios |
| Fonts | IBM Plex Sans + Mono, self-hosted, Latin subset, `woff2`, preload the UI font, `font-display: swap` |
| Hosting | Vercel (static), preview per PR |
| Analytics | Plausible or Umami (script < 2 KB) |

Performance budget (enforce with Lighthouse CI on PRs):
- Player JS ≤ 30 KB gzip. LCP < 1.5 s on 4G. CLS 0. No long tasks > 50 ms during playback.
- Reserve the player's height in CSS so it never shifts layout on hydration.
- Respect `prefers-reduced-motion`: step mode on, no auto-play.

Out of scope for v1: backend, database, user accounts, live "run your own code" sandbox, Next.js, Flutter Web.

## Repo layout

```
/site            Astro app + player component
/scenarios       YAML timelines + MDX prose, per language (java/, go/, rust/)
/verify/java     runnable programs + assertions (Gradle, JDK 21+)
/verify/go       runnable programs + assertions (go test)
/verify/rust     runnable programs + assertions (cargo test)
/tracer          (v2) JFR / runtime/trace / tracing → timeline converters
/docs            plan.md, design/ screenshots
```

## Timeline schema

```yaml
id: cf-thread-hopping
lang: java                     # java | go | rust
group: CompletableFuture       # sidebar section
title: Which thread runs the callback?
summary: One-line description shown under the title.
duration: 10                   # abstract time units
lanes: [main, io-1, commonPool-1, cpu-1]
code: |
  var f = supplyAsync(() -> slowCall(), ioPool);
  ...
events:                        # what each lane does over time
  - { id: a, lane: io-1, start: 1, end: 6, type: io,  label: "slowCall()", line: 1 }
  - { id: b, lane: io-1, start: 6, end: 7, type: run, label: "thenApply",  line: 3 }
deps:                          # arrows: end of one event → start of another
  - [a, b]
markers:                       # step captions; player can auto-pause at each
  - { at: 6, text: "Non-async thenApply runs on the thread that completed f.", line: 3 }
state:                         # shared values shown beside the timeline
  - { at: 0, key: queue, value: "[]" }
takeaway: Non-async = run wherever, right now. Async = submit to an executor.
gotcha: A heavy thenApply can stall an HTTP client's I/O threads.
```

Rules:

- `type` ∈ `run | io | blocked | wait | lock | error | fail | skip`.
- Events on the same lane must not overlap.
- Every `deps` id and `line` number must exist (enforce in Zod with refinements).
- `markers` drive step navigation: next/previous jump between marker times.

## Player behavior

- Controls: restart, previous step, play/pause, next step, time scrubber, speed (0.5×/1×/2×), "Pause at each step" (default on).
- Blocks reveal progressively up to `t`. Active blocks get an outline. Dependency arrows appear once the target event starts.
- Code panel highlights lines of events active at `t` plus the current marker's line.
- Deep links via URL hash. Keyboard: Space = play/pause, ←/→ = step.
- Timeline scrolls horizontally inside its own box on narrow screens; page never scrolls sideways.

## Visual language

Dark "oscilloscope" look; the same color always means the same thread state.

| Token | Value | Use |
| --- | --- | --- |
| Background | `#0E1014` | page |
| Surface | `#15181E` | cards |
| Border | `#23272F` | card borders, grid lines |
| Text / muted | `#E7E9EE` / `#9AA3B2` | body / labels |
| Running (CPU) | `#2BD4A4` solid | `run` |
| Waiting on I/O | `#4C8DFF` at 20% fill + solid outline | `io` |
| Blocked | `#FF8A3D` diagonal hatch + outline | `blocked` |
| Queued / parked | `#6B7280` dashed outline | `wait` |
| Holding lock | `#B78CFF` at 24% fill + outline | `lock` |
| Error / failed | `#F2545B` at 22% fill + outline | `error`, `fail` |
| Skipped | `#6B7280` dotted outline, struck label | `skip` |
| Accent | `#FF8A3D` | time cursor, play button, step label |

Fonts: IBM Plex Sans (UI), IBM Plex Mono (code, lane labels, axis). Touch targets ≥ 44 px. Text contrast ≥ 4.5:1. States must differ in lightness/pattern, not hue alone.

Screens (see `docs/design/`): desktop scenario player, cross-language compare page (same problem in Java/Go/Rust + construct map table), mobile player with Code / Key idea / Gotcha tabs.

## Accuracy verification (non-negotiable)

Accuracy is the product. A wrong thread assignment teaches a wrong mental model.

- Every scenario has a runnable twin in `/verify/<lang>` that logs thread names and event order.
- Assert **facts, not timings**: e.g. "thenApply ran on the same thread as supplyAsync", "T2 was BLOCKED while T1 held the monitor". Never assert exact durations.
- GitHub Actions runs Java (JDK 21+), Go and Rust on every PR and on a weekly schedule.
- Use a JDK matrix where behavior differs (e.g. virtual threads pinned inside `synchronized` before JDK 24).
- When unsure how a construct behaves, write the verification program first, then the YAML.
- Java layout: scenario `foo-bar` → twin `verify/java/src/main/java/threadline/java/foobar/Main.java` + facts test in the same package under `src/test`. `ScenarioSyncTest` fails if a YAML has no twin or its `code` lines aren't in the twin, in order.
- Run locally: `cd verify/java && ./gradlew test` (`-Pjdk=25` for another JDK). CI matrix: JDK 21 and 25.

## Milestone 1 (current)

- [x] Zod timeline schema + types (`site/src/lib/timeline.ts`)
- [x] Player component (timeline, code panel, state panel, caption, controls): `site/src/components/Player.svelte`, logic in `site/src/lib/player.ts`
- [ ] Scenario: race condition on `count++`
- [ ] Scenario: `synchronized` vs `AtomicInteger`
- [ ] Scenario: CompletableFuture pipeline (`supplyAsync` → `thenApply` → `thenAccept`)
- [ ] Scenario: `thenApply` vs `thenApplyAsync` — which thread runs the callback
- [ ] Scenario: `allOf` fan-out
- [ ] Verification programs + CI for all 5
- [ ] Deploy to Vercel; test with 3–4 target users

## Scenario backlog

**Java:** race condition · synchronized · AtomicInteger/CAS · fixed thread pool + queue · `Future.get()` blocks · CompletableFuture pipeline · thenApply vs thenApplyAsync · thenCompose vs thenCombine · allOf fan-out · exceptionally/handle · CountDownLatch · Semaphore · BlockingQueue producer/consumer · virtual threads (mount/unmount on carriers)

**Go:** goroutines + WaitGroup · unbuffered channel handoff · worker pool on a buffered channel + close · select with timeout/context · sync.Mutex

**Rust:** `thread::spawn` + `Arc<Mutex<T>>` · Tokio tasks and `.await` yield points · blocking in async vs `spawn_blocking` · `join!` vs sequential `.await`

**Compare pages:** wait for all · first to finish / timeout · mutual exclusion · bounded queue · limit concurrency · cheap threads.

## v2 (later)

Trace-driven mode: record real runs and convert to the same schema.

- Java: JFR (`jdk.JavaMonitorEnter`, `jdk.ThreadPark`, `jdk.VirtualThreadPinned`) or a ByteBuddy agent wrapping CompletableFuture stages.
- Go: `runtime/trace` parsed with `golang.org/x/exp/trace`.
- Rust: `tracing` subscriber emitting Tokio task poll/park events as JSON.

Pre-record at build time; still no live sandbox.

## Conventions

- TypeScript strict mode (including `<script lang="ts">` in Svelte). No `any` in schema or player code.
- Keep the player free of scenario-specific logic.
- Scenario copy: short sentences, one idea per marker, explain *why* not just *what*.
- Commit scenarios with their verification program in the same PR.

## Open decisions

- Domain for Threadline.
