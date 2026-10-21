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
export function highlightedLines(timeline: Timeline, t: number): Set<number> {
  const lines = new Set<number>();
  for (const e of activeEvents(timeline.events, t)) if (e.line) lines.add(e.line);
  const step = timeline.markers[stepIndexAt(timeline.markers, t)];
  if (step?.line) lines.add(step.line);
  return lines;
}
