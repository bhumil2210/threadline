<script module lang="ts">
  export const LABEL_W = 92;
</script>

<script lang="ts">
  import type { Timeline, TimelineEvent } from '../lib/timeline';

  interface Props {
    timeline: Timeline;
    t: number;
    activeIds: Set<string>;
    ppu: number;
  }

  let { timeline, t, activeIds, ppu }: Props = $props();

  const AXIS_H = 32;
  const LANE_H = 44;
  const BLOCK_H = 28;
  const PAD_R = 18; // keep in sync with Player
  const GAP = 1.5;
  const CHAR_W = 6.7; // IBM Plex Mono at 11px

  const uid = $props.id();
  const x = (time: number) => LABEL_W + time * ppu;
  const laneIndex = (lane: string) => timeline.lanes.indexOf(lane);
  const blockY = (lane: string) => AXIS_H + laneIndex(lane) * LANE_H + (LANE_H - BLOCK_H) / 2;
  const midY = (lane: string) => blockY(lane) + BLOCK_H / 2;

  const width = $derived(x(timeline.duration) + PAD_R);
  const height = AXIS_H + timeline.lanes.length * LANE_H + 6;
  const ticks = $derived(Array.from({ length: Math.floor(timeline.duration) + 1 }, (_, i) => i));

  const byId = new Map(timeline.events.map((e) => [e.id, e]));

  interface Arrow {
    to: TimelineEvent;
    d: string;
  }

  const arrows: Arrow[] = $derived(
    timeline.deps.flatMap(([fromId, toId]) => {
      const from = byId.get(fromId);
      const to = byId.get(toId);
      if (!from || !to || from.lane === to.lane) return [];
      // Elbow: leave the source block's top/bottom edge at its end time, run along the
      // target lane, enter the target block from the left.
      const down = laneIndex(to.lane) > laneIndex(from.lane);
      const x1 = x(from.end) - 6;
      const y1 = down ? blockY(from.lane) + BLOCK_H : blockY(from.lane);
      const x2 = x(to.start) + GAP;
      const y2 = midY(to.lane);
      if (x2 - x1 < 14) {
        const yEnd = down ? blockY(to.lane) : blockY(to.lane) + BLOCK_H;
        return [{ to, d: `M${x1} ${y1} V${yEnd}` }];
      }
      const r = 8;
      const turn = down ? y2 - r : y2 + r;
      return [{ to, d: `M${x1} ${y1} V${turn} Q${x1} ${y2} ${x1 + r} ${y2} H${x2}` }];
    }),
  );

  const activeLanes = $derived(new Set(timeline.events.filter((e) => activeIds.has(e.id)).map((e) => e.lane)));
  const fits = (e: TimelineEvent) => e.label.length * CHAR_W + 12 <= (e.end - e.start) * ppu - GAP * 2;
</script>

<svg
  class="timeline"
  {width}
  {height}
  viewBox="0 0 {width} {height}"
  role="img"
  aria-label="Thread timeline: {timeline.lanes.join(', ')}"
>
  <defs>
    <pattern id="{uid}-hatch" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
      <rect width="6" height="6" fill="var(--blocked)" fill-opacity="0.14" />
      <line x1="0" y1="0" x2="0" y2="6" stroke="var(--blocked)" stroke-width="2.2" />
    </pattern>
    <marker id="{uid}-arrow" viewBox="0 0 8 8" refX="7" refY="4" markerWidth="7" markerHeight="7" orient="auto">
      <path d="M0 0 L8 4 L0 8 z" fill="var(--muted)" />
    </marker>
    <clipPath id="{uid}-reveal">
      <rect x={LABEL_W} y="0" width={Math.max(0, t * ppu)} {height} />
    </clipPath>
  </defs>

  <!-- Static: grid, axis, lanes. Never depends on t. -->
  <g class="grid">
    {#each ticks as tick (tick)}
      <line x1={x(tick)} x2={x(tick)} y1={AXIS_H - 4} y2={height} />
      <text x={x(tick)} y={AXIS_H - 20} text-anchor="middle">{tick}</text>
    {/each}
  </g>
  {#each timeline.lanes as lane, i (lane)}
    <g class="lane" class:busy={activeLanes.has(lane)}>
      <line class="lane-rule" x1="0" x2={width} y1={AXIS_H + (i + 1) * LANE_H} y2={AXIS_H + (i + 1) * LANE_H} />
      <text class="lane-label" x="10" y={AXIS_H + i * LANE_H + LANE_H / 2} dominant-baseline="central">{lane}</text>
    </g>
  {/each}

  <!-- Under the blocks, so they never cross a label. -->
  {#each arrows as a (a.d)}
    <path class="dep" class:shown={t >= a.to.start} d={a.d} marker-end="url(#{uid}-arrow)" />
  {/each}

  <!-- Revealed up to t by one clip rect: a single attribute write per frame. -->
  <g clip-path="url(#{uid}-reveal)">
    {#each timeline.events as e (e.id)}
      <g class="block {e.type}" class:active={activeIds.has(e.id)}>
        <title>{e.lane}: {e.label} ({e.type})</title>
        <rect
          x={x(e.start) + GAP}
          y={blockY(e.lane)}
          width={Math.max(2, (e.end - e.start) * ppu - GAP * 2)}
          height={BLOCK_H}
          rx="4"
          fill={e.type === 'blocked' ? `url(#${uid}-hatch)` : undefined}
        />
        {#if fits(e)}
          <text x={x(e.start) + GAP + 7} y={midY(e.lane)} dominant-baseline="central">{e.label}</text>
        {/if}
      </g>
    {/each}
  </g>

  <g class="cursor" transform="translate({x(t)} 0)">
    <line y1={AXIS_H - 4} y2={height} />
    <path d="M-5 {AXIS_H - 10} h10 l-5 6 z" />
  </g>
</svg>

<style>
  .timeline {
    display: block;
    font-family: var(--font-mono);
    user-select: none;
  }

  .grid line {
    stroke: var(--border);
    stroke-width: 1;
  }

  .grid text {
    fill: var(--muted);
    font-size: 10px;
  }

  .lane-rule {
    stroke: var(--border);
  }

  .lane-label {
    fill: var(--muted);
    font-size: 12px;
    transition: fill 120ms;
  }

  .lane.busy .lane-label {
    fill: var(--text);
  }

  .block rect {
    stroke-width: 1.25;
  }

  .block text {
    font-size: 11px;
    fill: var(--text);
    pointer-events: none;
  }

  .run rect {
    fill: var(--run);
    stroke: var(--run);
  }

  .run text {
    fill: #06231b;
    font-weight: 500;
  }

  .io rect {
    fill: color-mix(in srgb, var(--io) 20%, transparent);
    stroke: var(--io);
  }

  .blocked rect {
    stroke: var(--blocked);
  }

  /* A dark halo keeps the label readable over the hatch. */
  .blocked text {
    font-weight: 500;
    paint-order: stroke;
    stroke: var(--surface);
    stroke-width: 4px;
    stroke-linejoin: round;
  }

  .wait rect {
    fill: transparent;
    stroke: var(--wait);
    stroke-dasharray: 4 3;
  }

  .wait text {
    fill: var(--muted);
  }

  .lock rect {
    fill: color-mix(in srgb, var(--lock) 24%, transparent);
    stroke: var(--lock);
  }

  .error rect,
  .fail rect {
    fill: color-mix(in srgb, var(--error) 22%, transparent);
    stroke: var(--error);
  }

  .skip rect {
    fill: transparent;
    stroke: var(--wait);
    stroke-dasharray: 1 3;
  }

  .skip text {
    fill: var(--muted);
    text-decoration: line-through;
  }

  .block.active rect {
    stroke: var(--text);
    stroke-width: 2;
  }

  .dep {
    fill: none;
    stroke: var(--muted);
    stroke-width: 1.25;
    opacity: 0;
    transition: opacity 160ms;
  }

  .dep.shown {
    opacity: 0.85;
  }

  .cursor line {
    stroke: var(--accent);
    stroke-width: 1.5;
  }

  .cursor path {
    fill: var(--accent);
  }

  @media (prefers-reduced-motion: reduce) {
    .dep,
    .lane-label {
      transition: none;
    }
  }
</style>
