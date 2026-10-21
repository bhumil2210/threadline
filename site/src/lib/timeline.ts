import { z } from 'astro/zod';

export const EVENT_TYPES = ['run', 'io', 'blocked', 'wait', 'lock', 'error', 'fail', 'skip'] as const;

const time = z.number().nonnegative();
const line = z.number().int().positive();

const event = z.object({
  id: z.string().min(1),
  lane: z.string().min(1),
  start: time,
  end: time,
  type: z.enum(EVENT_TYPES),
  label: z.string().min(1),
  line: line.optional(),
});

const marker = z.object({
  at: time,
  text: z.string().min(1),
  line: line.optional(),
});

const stateEntry = z.object({
  at: time,
  key: z.string().min(1),
  value: z.string(),
});

const baseTimeline = z.object({
  id: z.string().regex(/^[a-z0-9-]+$/, 'id must be kebab-case'),
  lang: z.enum(['java', 'go', 'rust']),
  group: z.string().min(1),
  title: z.string().min(1),
  summary: z.string().min(1),
  duration: z.number().positive(),
  lanes: z.array(z.string().min(1)).min(1),
  code: z.string().min(1),
  events: z.array(event).min(1),
  deps: z.array(z.tuple([z.string(), z.string()])).default([]),
  markers: z.array(marker).min(1),
  state: z.array(stateEntry).default([]),
  takeaway: z.string().min(1),
  gotcha: z.string().min(1).optional(),
});

export const timelineSchema = baseTimeline.superRefine((t, ctx) => {
  const issue = (path: (string | number)[], message: string) =>
    ctx.addIssue({ code: 'custom', path, message });

  const codeLines = t.code.replace(/\n$/, '').split('\n').length;
  const checkLine = (n: number | undefined, path: (string | number)[]) => {
    if (n !== undefined && n > codeLines) issue(path, `line ${n} is past the end of code (${codeLines} lines)`);
  };
  const checkTime = (at: number, path: (string | number)[]) => {
    if (at > t.duration) issue(path, `time ${at} is past duration ${t.duration}`);
  };

  if (new Set(t.lanes).size !== t.lanes.length) issue(['lanes'], 'lane names must be unique');

  const ids = new Set<string>();
  t.events.forEach((e, i) => {
    if (ids.has(e.id)) issue(['events', i, 'id'], `duplicate event id "${e.id}"`);
    ids.add(e.id);
    if (!t.lanes.includes(e.lane)) issue(['events', i, 'lane'], `unknown lane "${e.lane}"`);
    if (e.end <= e.start) issue(['events', i, 'end'], 'end must be after start');
    checkTime(e.end, ['events', i, 'end']);
    checkLine(e.line, ['events', i, 'line']);
  });

  for (const lane of t.lanes) {
    const onLane = t.events
      .map((e, i) => ({ e, i }))
      .filter(({ e }) => e.lane === lane)
      .sort((a, b) => a.e.start - b.e.start);
    for (let k = 1; k < onLane.length; k++) {
      const prev = onLane[k - 1]!;
      const cur = onLane[k]!;
      if (cur.e.start < prev.e.end) {
        issue(['events', cur.i, 'start'], `"${cur.e.id}" overlaps "${prev.e.id}" on lane ${lane}`);
      }
    }
  }

  t.deps.forEach(([from, to], i) => {
    if (!ids.has(from)) issue(['deps', i, 0], `unknown event "${from}"`);
    if (!ids.has(to)) issue(['deps', i, 1], `unknown event "${to}"`);
  });

  t.markers.forEach((m, i) => {
    checkTime(m.at, ['markers', i, 'at']);
    checkLine(m.line, ['markers', i, 'line']);
  });
  t.state.forEach((s, i) => checkTime(s.at, ['state', i, 'at']));
});

export type Timeline = z.infer<typeof timelineSchema>;
export type TimelineEvent = Timeline['events'][number];
export type EventType = (typeof EVENT_TYPES)[number];
