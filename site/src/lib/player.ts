import type { Timeline, TimelineEvent } from './timeline';

type Marker = Timeline['markers'][number];
type StateEntry = Timeline['state'][number];

/** Times closer than this count as "at" a marker, so float drift never skips a step. */
export const EPS = 1e-6;

/** Index of the last marker at or before t, or -1 before the first one. */
export function stepIndexAt(markers: readonly Marker[], t: number): number {
  let idx = -1;
  for (let i = 0; i < markers.length; i++) {
    if (markers[i]!.at <= t + EPS) idx = i;
    else break;
  }
  return idx;
}

/** Time of the next marker strictly after t, or null when none is left. */
export function nextStepTime(markers: readonly Marker[], t: number): number | null {
  const m = markers.find((m) => m.at > t + EPS);
  return m ? m.at : null;
}

/** Previous step: back to the current marker if past it, else the one before. */
export function prevStepTime(markers: readonly Marker[], t: number): number {
  const i = stepIndexAt(markers, t);
  if (i < 0) return 0;
  if (t > markers[i]!.at + EPS) return markers[i]!.at;
  return i > 0 ? markers[i - 1]!.at : 0;
}

/** First marker crossed when moving from `from` (exclusive) to `to` (inclusive). */
export function markerCrossed(markers: readonly Marker[], from: number, to: number): Marker | null {
  return markers.find((m) => m.at > from + EPS && m.at <= to + EPS) ?? null;
}

/** Events running at t. An event is active on [start, end). */
export function activeEvents(events: readonly TimelineEvent[], t: number): TimelineEvent[] {
  return events.filter((e) => e.start <= t + EPS && t + EPS < e.end);
}

export interface StateValue {
  key: string;
  value: string;
  at: number;
}

/** Latest value per key at t, keys in order of first appearance. Keys not yet set are left out. */
export function stateAt(state: readonly StateEntry[], t: number): StateValue[] {
  const latest = new Map<string, StateValue>();
  const order: string[] = [];
  for (const s of [...state].sort((a, b) => a.at - b.at)) {
    if (!order.includes(s.key)) order.push(s.key);
    if (s.at <= t + EPS) latest.set(s.key, { key: s.key, value: s.value, at: s.at });
  }
  return order.flatMap((k) => latest.get(k) ?? []);
}

/** Code lines to highlight at t: lines of running events plus the current marker's line. */
export function highlightedLines(timeline: Pick<Timeline, 'events' | 'markers'>, t: number): Set<number> {
  const lines = new Set<number>();
  for (const e of activeEvents(timeline.events, t)) if (e.line) lines.add(e.line);
  const step = timeline.markers[stepIndexAt(timeline.markers, t)];
  if (step?.line) lines.add(step.line);
  return lines;
}

export interface ThreadStateInfo {
  /** What java.lang.Thread.getState() reports, or null when the step never ran. */
  javaState: string | null;
  meaning: string;
}

/** What each block type means for the thread, in Java's own terms. */
export const THREAD_STATES: Record<TimelineEvent['type'], ThreadStateInfo> = {
  run: { javaState: 'RUNNABLE', meaning: 'Running on a CPU.' },
  io: {
    javaState: 'RUNNABLE',
    meaning:
      'Waiting on I/O, like a network call. For blocking socket I/O Java still reports RUNNABLE: the thread is stuck in the operating system, not in Java.',
  },
  blocked: {
    javaState: 'BLOCKED',
    meaning: 'Waiting to enter a synchronized block that another thread holds. It does nothing until the lock is released.',
  },
  wait: {
    javaState: 'WAITING',
    meaning:
      'Parked until something wakes it: join(), get(), await(), take(), acquire(). Uses no CPU. With a timeout it is TIMED_WAITING.',
  },
  lock: { javaState: 'RUNNABLE', meaning: 'Holding a lock. Any other thread that needs it will block until it is released.' },
  error: { javaState: 'RUNNABLE', meaning: 'Running code that throws an exception.' },
  fail: { javaState: 'RUNNABLE', meaning: 'Ran, but the operation failed (for example a CAS that lost the race).' },
  skip: { javaState: null, meaning: 'Skipped: this step never ran.' },
};

/** Step links: `#step-3` for the main timeline, `#<variant id>/step-3` for a variant. */
export function formatStepHash(variantId: string | null, step: number): string {
  return variantId ? `#${variantId}/step-${step}` : `#step-${step}`;
}

export function parseStepHash(hash: string): { variantId: string | null; step: number } | null {
  const m = /^#(?:([a-z0-9-]+)\/)?step-(\d+)$/.exec(hash);
  return m ? { variantId: m[1] ?? null, step: Number(m[2]) } : null;
}

/**
 * The code panel's markup: one span per line, so the player highlights a line by toggling a class.
 * `classes` gives the extra classes of line n (1-based), used to render the first frame at build time.
 */
export function linesHtml(lines: readonly string[], classes: (n: number) => string = () => ''): string {
  return lines
    .map((html, i) => {
      const extra = classes(i + 1);
      return `<span class="line${extra ? ` ${extra}` : ''}"><span class="ln">${i + 1}</span>${html || ' '}</span>`;
    })
    .join('');
}
