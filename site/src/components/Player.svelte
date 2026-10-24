<script lang="ts">
  import { onMount, type Snippet } from 'svelte';
  import TimelineView, { labelWidth } from './Timeline.svelte';
  import {
    EPS,
    THREAD_STATES,
    activeEvents,
    formatStepHash,
    highlightedLines,
    linesHtml,
    markerCrossed,
    nextStepTime,
    parseStepHash,
    prevStepTime,
    stateAt,
    stepIndexAt,
  } from '../lib/player';
  import type { PlayerVariant } from '../lib/catalog';
  import type { EventType, PlayerTimeline } from '../lib/timeline';

  interface Props {
    /** The main timeline, embedded in the page. */
    first: PlayerTimeline;
    /**
     * The main timeline's code lines (see linesHtml), passed as static HTML rather than as props,
     * so the page carries them once instead of twice.
     */
    children?: Snippet;
    /** The what-if switch, main first. One entry means no switch. */
    options: { id: string; label: string }[];
    /** Every timeline, main first. Fetched when idle, so the page itself stays small. */
    variantsUrl: string;
  }

  let { first, children, options, variantsUrl }: Props = $props();

  /** Wall-clock milliseconds per abstract time unit at 1x. */
  const UNIT_MS = 700;
  const SPEEDS = [0.5, 1, 2] as const;
  const LEGEND: Record<EventType, string> = {
    run: 'Running',
    io: 'Waiting on I/O',
    blocked: 'Blocked',
    wait: 'Waiting / parked',
    lock: 'Holding lock',
    error: 'Error',
    fail: 'Failed',
    skip: 'Skipped',
  };
  const PAD_R = 18;

  let vi = $state(0);
  let loaded = $state<PlayerVariant[] | null>(null);
  let loading: Promise<PlayerVariant[]> | null = null;
  /** The variant shown, or null for the main timeline, whose code is the static children. */
  const current = $derived(vi > 0 && loaded ? loaded[vi]! : null);
  const timeline = $derived(current?.timeline ?? first);
  const markers = $derived(timeline.markers);
  const duration = $derived(timeline.duration);

  let t = $state(0);
  let playing = $state(false);
  let speed = $state<(typeof SPEEDS)[number]>(1);
  let pauseAtSteps = $state(true);
  let tab = $state<'code' | 'idea' | 'gotcha'>('code');
  let selectedId = $state<string | null>(null);

  // Derived views keyed by primitives, so the DOM only updates when the value really changes,
  // not on every animation frame.
  const stepIdx = $derived(stepIndexAt(markers, t));
  const step = $derived(markers[Math.max(0, stepIdx)]!);
  const activeKey = $derived(activeEvents(timeline.events, t).map((e) => e.id).join(' '));
  const activeIds = $derived(new Set(activeKey ? activeKey.split(' ') : []));
  const hlKey = $derived([...highlightedLines(timeline, t)].join(' '));
  const hlLines = $derived(new Set(hlKey ? hlKey.split(' ').map(Number) : []));
  const stateKey = $derived(JSON.stringify(stateAt(timeline.state, t)));
  const stateRows = $derived(JSON.parse(stateKey) as ReturnType<typeof stateAt>);
  const usedTypes = $derived([...new Set(timeline.events.map((e) => e.type))]);
  const selected = $derived(timeline.events.find((e) => e.id === selectedId) ?? null);

  let raf = 0;
  let last = 0;
  let reducedMotion = false;
  let scroller: HTMLDivElement | undefined = $state();
  let scrollerWidth = $state(0);
  let scrollLeft = 0;
  let ready = $state(false);

  const compact = $derived(scrollerWidth > 0 && scrollerWidth < 560);
  const LABEL_W = $derived(compact ? 6 : labelWidth(timeline.lanes));

  // Fill the card's width; below 56px per unit the timeline scrolls instead of squashing.
  const ppu = $derived(scrollerWidth ? Math.max(56, (scrollerWidth - 12 - LABEL_W - PAD_R) / duration) : 72);

  function frame(now: number) {
    const dt = last ? Math.min(now - last, 100) : 0;
    last = now;
    const next = t + (dt / UNIT_MS) * speed;
    const crossed = pauseAtSteps ? markerCrossed(markers, t, next) : null;
    if (crossed) {
      t = crossed.at;
      stop();
      return;
    }
    if (next >= duration) {
      t = duration;
      stop();
      return;
    }
    t = next;
    raf = requestAnimationFrame(frame);
  }

  function play() {
    if (t >= duration - EPS) t = 0;
    if (reducedMotion) {
      t = nextStepTime(markers, t) ?? duration;
      return;
    }
    playing = true;
    last = 0;
    raf = requestAnimationFrame(frame);
  }

  function stop() {
    playing = false;
    cancelAnimationFrame(raf);
  }

  const toggle = () => (playing ? stop() : play());
  const restart = () => {
    stop();
    t = 0;
  };
  const nextStep = () => {
    stop();
    t = nextStepTime(markers, t) ?? duration;
  };
  const prevStep = () => {
    stop();
    t = prevStepTime(markers, t);
  };

  function loadVariants(): Promise<PlayerVariant[]> {
    loading ??= fetch(variantsUrl)
      .then((r) => {
        if (!r.ok) throw new Error(`${variantsUrl}: ${r.status}`);
        return r.json() as Promise<PlayerVariant[]>;
      })
      .then((v) => (loaded = v));
    loading.catch(() => (loading = null)); // allow a retry on the next click
    return loading;
  }

  const prefetch = () => void loadVariants().catch(() => {});

  /** Switch what-if variant, keeping the same moment in time so the two can be compared. */
  async function switchVariant(i: number) {
    if (i === vi) return;
    const all = i > 0 ? await loadVariants() : null;
    stop();
    selectedId = null;
    vi = i;
    t = Math.min(t, (all ? all[i]!.timeline : first).duration);
  }

  const select = (id: string | null) => (selectedId = id === selectedId ? null : id);

  function onKey(e: KeyboardEvent) {
    const el = e.target as HTMLElement;
    if (el.closest('input, textarea, select, [contenteditable]')) return;
    if (e.key === 'Escape' && selectedId) {
      selectedId = null;
    } else if (e.key === ' ' && !el.closest('button, [role="button"]')) {
      e.preventDefault();
      toggle();
    } else if (e.key === 'ArrowRight') {
      e.preventDefault();
      nextStep();
    } else if (e.key === 'ArrowLeft') {
      e.preventDefault();
      prevStep();
    }
  }

  // Keep the cursor in view on narrow screens. Uses cached sizes, so no layout read per frame.
  $effect(() => {
    if (!scroller) return;
    const cx = LABEL_W + t * ppu;
    if (cx > scrollLeft + scrollerWidth - 40) scroller.scrollLeft = cx - LABEL_W - 24;
    else if (cx < scrollLeft + LABEL_W) scroller.scrollLeft = Math.max(0, cx - LABEL_W - 24);
  });

  // Highlight code lines by toggling classes, so the static first-variant code needs no re-render.
  // Runs after the DOM update, so it also styles a variant's lines right after they are swapped in.
  let codeEl: HTMLElement | undefined = $state();
  $effect(() => {
    void current;
    const stepLine = step.line;
    const selLine = selected?.line;
    codeEl?.querySelectorAll('.line').forEach((el, i) => {
      el.classList.toggle('step-line', stepLine === i + 1);
      el.classList.toggle('hl', hlLines.has(i + 1));
      el.classList.toggle('sel-line', selLine === i + 1);
    });
  });

  // Deep link to the current variant and step, updated only when they change.
  $effect(() => {
    if (!ready) return;
    const hash = formatStepHash(vi === 0 ? null : timeline.id, stepIdx + 1);
    // Leave other anchors (e.g. #explanation) alone; only own the URL when it's empty or a step link.
    const ours = location.hash === '' || parseStepHash(location.hash) !== null;
    if (ours && location.hash !== hash) history.replaceState(null, '', hash);
  });

  onMount(() => {
    reducedMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const link = parseStepHash(location.hash);
    const i = link?.variantId ? options.findIndex((o) => o.id === link.variantId) : 0;
    const goTo = (all: PlayerVariant[] | null) => {
      if (link && i >= 0) {
        vi = i;
        const target = (all ? all[i]!.timeline : first).markers[link.step - 1];
        if (target) t = target.at;
      }
      ready = true;
    };
    if (i > 0) {
      loadVariants().then(goTo, () => goTo(null));
    } else {
      goTo(null);
      // Fetch the other variants well after first paint, so switching is instant. Not sooner: a request
      // that starts before the page has painted counts against load time. Pointing at the switch is sooner.
      if (options.length > 1) setTimeout(prefetch, 3000);
    }
    return stop;
  });

  const fmt = (n: number) => n.toFixed(1);
</script>

<svelte:window onkeydown={onKey} />

<section class="player" aria-label="Scenario player">
  {#if options.length > 1}
    <div class="whatif" role="group" aria-label="What if" onpointerenter={prefetch} onfocusin={prefetch}>
      <span class="whatif-label">What if</span>
      <div class="whatif-options">
        {#each options as o, i (o.id)}
          <button type="button" aria-pressed={vi === i} onclick={() => switchVariant(i)}>{o.label}</button>
        {/each}
      </div>
      <p class="whatif-summary">{timeline.summary}</p>
    </div>
  {/if}

  <div class="card stage">
    <div
      class="scroller"
      bind:this={scroller}
      bind:clientWidth={scrollerWidth}
      onscroll={(e) => (scrollLeft = e.currentTarget.scrollLeft)}
    >
      {#key timeline.id}
        <TimelineView {timeline} {t} {activeIds} {ppu} {compact} {selectedId} onselect={select} labelW={LABEL_W} />
      {/key}
    </div>
    <div class="details" class:empty={!selected} aria-live="polite">
      {#if selected}
        {@const info = THREAD_STATES[selected.type]}
        <div class="details-head">
          <span class="swatch {selected.type}"></span>
          <strong>{selected.lane}</strong>
          <span class="details-label">{selected.label}</span>
          {#if selected.threadState ?? info.javaState}<code class="jstate">{selected.threadState ?? info.javaState}</code>{/if}
          <button type="button" class="details-close" onclick={() => (selectedId = null)} aria-label="Close details">×</button>
        </div>
        <p>
          {info.meaning}
          {#if selected.note}{' '}{selected.note}{/if}
        </p>
        <p class="details-meta">
          t = {fmt(selected.start)} to {fmt(selected.end)}{#if selected.line}{' '}· line {selected.line}{/if}
        </p>
      {:else}
        <p class="hint">Tap any block to see what that thread is doing.</p>
      {/if}
    </div>
    <ul class="legend" aria-label="Legend">
      {#each usedTypes as type (type)}
        <li><span class="swatch {type}"></span>{LEGEND[type]}</li>
      {/each}
    </ul>
  </div>

  <div class="caption" aria-live="polite">
    <span class="step-label">Step {stepIdx + 1}/{markers.length}</span>
    <p>{step.text}</p>
  </div>

  <div class="controls">
    <div class="buttons">
      <button type="button" onclick={restart} aria-label="Restart" title="Restart">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 5v14M19 5 9 12l10 7z" /></svg>
      </button>
      <button type="button" onclick={prevStep} aria-label="Previous step" title="Previous step (←)">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 5 8 12l7 7" /></svg>
      </button>
      <button type="button" class="play" onclick={toggle} aria-label={playing ? 'Pause' : 'Play'} title="Play / pause (Space)">
        {#if playing}
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5v14M16 5v14" /></svg>
        {:else}
          <svg viewBox="0 0 24 24" aria-hidden="true"><path class="fill" d="M8 5v14l11-7z" /></svg>
        {/if}
      </button>
      <button type="button" onclick={nextStep} aria-label="Next step" title="Next step (→)">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 7 7-7 7" /></svg>
      </button>
    </div>

    <label class="scrub">
      <span class="sr-only">Time</span>
      <input
        type="range"
        min="0"
        max={duration}
        step="0.01"
        value={t}
        oninput={(e) => {
          stop();
          t = Number(e.currentTarget.value);
        }}
        style="--p: {(t / duration) * 100}%"
      />
      <output>{fmt(t)} / {fmt(duration)}</output>
    </label>

    <div class="options">
      <div class="speeds" role="group" aria-label="Speed">
        {#each SPEEDS as s (s)}
          <button type="button" aria-pressed={speed === s} onclick={() => (speed = s)}>{s}×</button>
        {/each}
      </div>
      <label class="pause-toggle">
        <input type="checkbox" bind:checked={pauseAtSteps} />
        Pause at each step
      </label>
    </div>
  </div>

  <div class="tabs" role="tablist" aria-label="Details">
    {#each [['code', 'Code'], ['idea', 'Key idea'], ['gotcha', 'Gotcha']] as const as [id, label] (id)}
      {#if id !== 'gotcha' || timeline.gotcha}
        <button type="button" role="tab" aria-selected={tab === id} onclick={() => (tab = id)}>{label}</button>
      {/if}
    {/each}
  </div>

  <div class="panels">
    <div class="card code" class:show={tab === 'code'}>
      <pre><code bind:this={codeEl}
          >{#if current}{@html linesHtml(current.codeLines)}{:else}{@render children?.()}{/if}</code
        ></pre>
    </div>

    <aside class="side">
      {#if timeline.state.length}
        <div class="card state">
          <h2>Shared state</h2>
          <dl>
            {#each stateRows as row (row.key)}
              <div class:changed={row.at > step.at - EPS && row.at <= t + EPS && stepIdx >= 0}>
                <dt>{row.key}</dt>
                <dd>{row.value}</dd>
              </div>
            {/each}
          </dl>
        </div>
      {/if}
      <div class="card note idea" class:show={tab === 'idea'}>
        <h2>Key idea</h2>
        <p>{timeline.takeaway}</p>
      </div>
      {#if timeline.gotcha}
        <div class="card note gotcha" class:show={tab === 'gotcha'}>
          <h2>Gotcha</h2>
          <p>{timeline.gotcha}</p>
        </div>
      {/if}
    </aside>
  </div>
</section>

<style>
  .player {
    display: grid;
    /* minmax(0, …) so a wide timeline scrolls inside its card instead of widening the page. */
    grid-template-columns: minmax(0, 1fr);
    gap: 14px;
  }

  .card {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
  }

  .whatif {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 12px;
  }

  .whatif-label {
    font: 500 12px/1 var(--font-mono);
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--accent);
  }

  .whatif-options {
    display: flex;
    flex-wrap: wrap;
    border: 1px solid var(--border);
    border-radius: 8px;
    overflow: hidden;
  }

  .whatif-options button {
    min-height: 40px;
    padding: 0 14px;
    background: var(--surface);
    border: 0;
    font-size: 14px;
    color: var(--muted);
  }

  .whatif-options button + button {
    border-left: 1px solid var(--border);
  }

  .whatif-options button[aria-pressed='true'] {
    background: var(--surface-2);
    color: var(--text);
    box-shadow: inset 0 -2px 0 var(--accent);
  }

  .whatif-summary {
    flex-basis: 100%;
    margin: 0;
    font-size: 15px;
    line-height: 1.5;
    color: var(--muted);
  }

  .details {
    min-height: 92px; /* reserved, so selecting a block never shifts the page */
    margin: 8px 16px 0;
    padding: 10px 12px;
    border: 1px solid var(--border);
    border-radius: 8px;
    font-size: 14px;
  }

  .details.empty {
    display: flex;
    align-items: center;
    border-style: dashed;
  }

  .details p {
    margin: 4px 0 0;
  }

  .details .hint {
    margin: 0;
    color: var(--muted);
  }

  .details-head {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px 10px;
  }

  .details-head strong {
    font: 500 13px var(--font-mono);
  }

  .details-label {
    font: 13px var(--font-mono);
    color: var(--muted);
  }

  .jstate {
    padding: 1px 6px;
    border: 1px solid var(--border);
    border-radius: 4px;
    font-size: 12px;
    background: var(--surface-2);
  }

  .details-close {
    margin-left: auto;
    width: 32px;
    height: 32px;
    padding: 0;
    background: none;
    border: 0;
    border-radius: 999px;
    font-size: 20px;
    line-height: 1;
    color: var(--muted);
  }

  .details-close:hover {
    background: var(--surface-2);
    color: var(--text);
  }

  .details-meta {
    font: 12px var(--font-mono);
    color: var(--muted);
  }

  .code :global(.line.sel-line) {
    box-shadow: inset 0 0 0 1px var(--text);
  }

  .stage {
    padding: 10px 0 12px;
    min-width: 0;
  }

  .scroller {
    overflow-x: auto;
    overscroll-behavior-x: contain;
    scrollbar-width: thin;
    padding: 0 6px;
  }

  .legend {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 16px;
    margin: 8px 0 0;
    padding: 0 16px;
    list-style: none;
    font-size: 12px;
    color: var(--muted);
  }

  .legend li {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .swatch {
    width: 16px;
    height: 10px;
    border-radius: 2px;
    border: 1.25px solid;
  }

  .swatch.run {
    background: var(--run);
    border-color: var(--run);
  }
  .swatch.io {
    background: color-mix(in srgb, var(--io) 20%, transparent);
    border-color: var(--io);
  }
  .swatch.blocked {
    background: repeating-linear-gradient(45deg, var(--blocked) 0 2px, transparent 2px 5px);
    border-color: var(--blocked);
  }
  .swatch.wait {
    border-style: dashed;
    border-color: var(--wait);
  }
  .swatch.lock {
    background: color-mix(in srgb, var(--lock) 24%, transparent);
    border-color: var(--lock);
  }
  .swatch.error,
  .swatch.fail {
    background: color-mix(in srgb, var(--error) 22%, transparent);
    border-color: var(--error);
  }
  .swatch.skip {
    border-style: dotted;
    border-color: var(--wait);
  }

  .caption {
    display: flex;
    gap: 12px;
    align-items: baseline;
    min-height: 3.2em;
    padding: 0 2px;
  }

  .caption p {
    margin: 0;
    font-size: 17px;
    font-weight: 500;
  }

  .step-label {
    flex: none;
    font: 500 12px/1 var(--font-mono);
    color: var(--accent);
    letter-spacing: 0.04em;
    text-transform: uppercase;
  }

  .controls {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 18px;
  }

  .buttons {
    display: flex;
    gap: 6px;
  }

  button {
    font: inherit;
    color: inherit;
    cursor: pointer;
  }

  .buttons button {
    display: grid;
    place-items: center;
    width: 44px;
    height: 44px;
    padding: 0;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 999px;
    transition: background-color 120ms;
  }

  .buttons button:hover {
    background: var(--surface-2);
  }

  .buttons svg {
    width: 20px;
    height: 20px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  .buttons .play {
    width: 52px;
    height: 52px;
    background: var(--accent);
    border-color: var(--accent);
    color: var(--on-accent);
  }

  .buttons .play:hover {
    background: color-mix(in srgb, var(--accent) 88%, var(--text));
  }

  .buttons svg .fill {
    fill: currentColor;
  }

  .scrub {
    flex: 1 1 220px;
    display: flex;
    align-items: center;
    gap: 10px;
    min-height: 44px;
  }

  .scrub input {
    flex: 1;
    height: 44px;
    margin: 0;
    background: transparent;
    appearance: none;
    cursor: pointer;
  }

  .scrub input::-webkit-slider-runnable-track {
    height: 4px;
    border-radius: 2px;
    background: linear-gradient(90deg, var(--accent) var(--p), var(--border) var(--p));
  }

  .scrub input::-moz-range-track {
    height: 4px;
    border-radius: 2px;
    background: linear-gradient(90deg, var(--accent) var(--p), var(--border) var(--p));
  }

  .scrub input::-webkit-slider-thumb {
    appearance: none;
    width: 16px;
    height: 16px;
    margin-top: -6px;
    border-radius: 50%;
    background: var(--text);
    border: 0;
  }

  .scrub input::-moz-range-thumb {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: var(--text);
    border: 0;
  }

  output {
    min-width: 7.5ch;
    font: 12px var(--font-mono);
    color: var(--muted);
    font-variant-numeric: tabular-nums;
    text-align: right;
  }

  .options {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 16px;
  }

  .speeds {
    display: flex;
    border: 1px solid var(--border);
    border-radius: 8px;
    overflow: hidden;
  }

  .speeds button {
    min-width: 48px;
    height: 44px;
    padding: 0 10px;
    background: transparent;
    border: 0;
    font: 12px var(--font-mono);
    color: var(--muted);
  }

  .speeds button + button {
    border-left: 1px solid var(--border);
  }

  .speeds button[aria-pressed='true'] {
    background: var(--surface-2);
    color: var(--text);
  }

  .pause-toggle {
    display: flex;
    align-items: center;
    gap: 8px;
    min-height: 44px;
    font-size: 14px;
    color: var(--muted);
    cursor: pointer;
  }

  .pause-toggle input {
    width: 18px;
    height: 18px;
    accent-color: var(--accent);
  }

  .tabs {
    display: none;
  }

  .panels {
    display: grid;
    grid-template-columns: minmax(0, 1.6fr) minmax(260px, 1fr);
    gap: 14px;
    align-items: start;
  }

  .code {
    overflow: hidden;
  }

  pre {
    margin: 0;
    padding: 12px 0;
    overflow-x: auto;
    font-size: 13px;
    line-height: 1.7;
  }

  /* Lines come from static or {@html} markup, which has no Svelte scope class, hence :global. */
  .code :global(.line) {
    display: block;
    padding: 0 16px 0 0;
    border-left: 3px solid transparent;
    transition: background-color 120ms;
  }

  .code :global(.line.hl) {
    background: var(--hl-run);
  }

  .code :global(.line.step-line) {
    background: var(--hl-step);
    border-left-color: var(--accent);
  }

  .code :global(.ln) {
    display: inline-block;
    width: 3.2ch;
    margin: 0 14px 0 6px;
    text-align: right;
    color: var(--muted); /* 7:1 on the card, 5.8:1 on a highlighted line */
    user-select: none;
  }

  .side {
    display: grid;
    gap: 14px;
  }

  .side h2 {
    margin: 0 0 8px;
    font: 500 12px/1 var(--font-mono);
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--muted);
  }

  .state,
  .note {
    padding: 14px 16px;
  }

  dl {
    display: grid;
    gap: 4px;
    margin: 0;
  }

  dl div {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 6px 8px;
    margin: 0 -8px;
    border-radius: 6px;
    font: 13px var(--font-mono);
    transition: background-color 160ms;
  }

  dl div.changed {
    background: color-mix(in srgb, var(--accent) 13%, transparent);
  }

  dt {
    color: var(--muted);
  }

  dd {
    margin: 0;
    color: var(--text);
    text-align: right;
  }

  .note p {
    margin: 0;
    font-size: 15px;
  }

  .gotcha h2 {
    color: var(--accent);
  }

  .sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
  }

  @media (max-width: 760px) {
    .panels {
      grid-template-columns: minmax(0, 1fr);
    }

    .side {
      display: contents;
    }

    .state {
      order: -1;
    }

    .code,
    .note {
      display: none;
    }

    .code.show,
    .note.show {
      display: block;
    }

    .tabs {
      display: flex;
      gap: 4px;
      border-bottom: 1px solid var(--border);
    }

    .tabs button {
      min-height: 44px;
      padding: 0 14px;
      background: none;
      border: 0;
      border-bottom: 2px solid transparent;
      color: var(--muted);
      font-size: 14px;
    }

    .tabs button[aria-selected='true'] {
      color: var(--text);
      border-bottom-color: var(--accent);
    }

    .caption {
      flex-direction: column;
      gap: 4px;
      min-height: 4.6em;
    }

    .caption p {
      font-size: 16px;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .code :global(.line),
    dl div,
    .buttons button {
      transition: none;
    }
  }
</style>
