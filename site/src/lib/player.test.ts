import { describe, expect, it } from 'vitest';
import {
  THREAD_STATES,
  activeEvents,
  formatStepHash,
  highlightedLines,
  markerCrossed,
  nextStepTime,
  parseStepHash,
  prevStepTime,
  stateAt,
  stepIndexAt,
} from './player';
import type { Timeline } from './timeline';

const markers = [
  { at: 0, text: 'a' },
  { at: 2, text: 'b', line: 3 },
  { at: 5, text: 'c' },
];

const timeline: Timeline = {
  id: 't',
  lang: 'java',
  group: 'g',
  order: 1,
  title: 'T',
  summary: 's',
  duration: 6,
  lanes: ['main', 'T1'],
  code: 'a\nb\nc\nd\n',
  events: [
    { id: 'x', lane: 'main', start: 0, end: 2, type: 'run', label: 'x', line: 1 },
    { id: 'y', lane: 'T1', start: 1, end: 4, type: 'io', label: 'y', line: 4 },
  ],
  deps: [],
  markers,
  state: [
    { at: 0, key: 'count', value: '0' },
    { at: 3, key: 'seen', value: '0' },
    { at: 4, key: 'count', value: '1' },
  ],
  takeaway: 'k',
};

describe('steps', () => {
  it('finds the current step', () => {
    expect(stepIndexAt(markers, 0)).toBe(0);
    expect(stepIndexAt(markers, 1.9)).toBe(0);
    expect(stepIndexAt(markers, 2)).toBe(1);
    expect(stepIndexAt(markers, 6)).toBe(2);
  });

  it('moves forward to the next marker, or null at the end', () => {
    expect(nextStepTime(markers, 0)).toBe(2);
    expect(nextStepTime(markers, 2)).toBe(5);
    expect(nextStepTime(markers, 5)).toBeNull();
  });

  it('moves back to the current marker first, then the previous one', () => {
    expect(prevStepTime(markers, 3)).toBe(2);
    expect(prevStepTime(markers, 2)).toBe(0);
    expect(prevStepTime(markers, 0)).toBe(0);
  });

  it('reports a marker crossed during a frame, excluding the start time', () => {
    expect(markerCrossed(markers, 1.5, 2.1)?.at).toBe(2);
    expect(markerCrossed(markers, 2, 2.1)).toBeNull();
    expect(markerCrossed(markers, 2.2, 4)).toBeNull();
  });
});

describe('time-derived views', () => {
  it('treats events as active on [start, end)', () => {
    expect(activeEvents(timeline.events, 1).map((e) => e.id)).toEqual(['x', 'y']);
    expect(activeEvents(timeline.events, 2).map((e) => e.id)).toEqual(['y']);
    expect(activeEvents(timeline.events, 4)).toEqual([]);
  });

  it('shows the latest state per key in first-appearance order', () => {
    expect(stateAt(timeline.state, 0)).toEqual([{ key: 'count', value: '0', at: 0 }]);
    expect(stateAt(timeline.state, 4).map((s) => `${s.key}=${s.value}`)).toEqual(['count=1', 'seen=0']);
  });

  it('highlights running event lines and the step line', () => {
    expect([...highlightedLines(timeline, 2.5)].sort()).toEqual([3, 4]);
  });
});

describe('step links', () => {
  it('round-trips main and variant step hashes', () => {
    expect(formatStepHash(null, 3)).toBe('#step-3');
    expect(formatStepHash('deadlock-fixed', 2)).toBe('#deadlock-fixed/step-2');
    expect(parseStepHash('#step-3')).toEqual({ variantId: null, step: 3 });
    expect(parseStepHash('#deadlock-fixed/step-2')).toEqual({ variantId: 'deadlock-fixed', step: 2 });
  });

  it('ignores other anchors', () => {
    expect(parseStepHash('#explanation')).toBeNull();
    expect(parseStepHash('')).toBeNull();
  });
});

describe('thread states', () => {
  it('maps every block type to what Java reports', () => {
    expect(THREAD_STATES.blocked.javaState).toBe('BLOCKED');
    expect(THREAD_STATES.wait.javaState).toBe('WAITING');
    expect(THREAD_STATES.io.javaState).toBe('RUNNABLE');
    expect(THREAD_STATES.skip.javaState).toBeNull();
  });
});
