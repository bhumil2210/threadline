import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import satori from 'satori';
import { Resvg } from '@resvg/resvg-js';
import type { EventType, Timeline } from './timeline';
import css from '../styles/global.css?raw';

/**
 * Share images (1200×630 PNG) for links posted on Reddit, Slack, X and the like. Build time only:
 * nothing here ships to the browser. Drawn from the timeline data, in the dark theme's colors.
 */

const WIDTH = 1200;
const HEIGHT = 630;

const require = createRequire(import.meta.url);
const font = (pkg: string, file: string) => readFileSync(require.resolve(`@fontsource/${pkg}/files/${file}`));
const fonts = [
  { name: 'Plex Sans', data: font('ibm-plex-sans', 'ibm-plex-sans-latin-400-normal.woff'), weight: 400 as const },
  { name: 'Plex Sans', data: font('ibm-plex-sans', 'ibm-plex-sans-latin-600-normal.woff'), weight: 600 as const },
  { name: 'Plex Mono', data: font('ibm-plex-mono', 'ibm-plex-mono-latin-400-normal.woff'), weight: 400 as const },
];

/** Colors come from the dark palette in global.css (the first definition of each token), so they never drift. */
const token = (name: string) => {
  const m = new RegExp(`--${name}:\\s*(#[0-9a-fA-F]{6})`).exec(css);
  if (!m) throw new Error(`og: token --${name} not found in global.css`);
  return m[1]!;
};
const C = {
  bg: token('bg'), surface: token('surface'), border: token('border'), text: token('text'), muted: token('muted'),
  accent: token('accent'), run: token('run'), io: token('io'), blocked: token('blocked'), wait: token('wait'),
  lock: token('lock'), error: token('error'),
};

const alpha = (hex: string, a: number) => {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${n >> 16}, ${(n >> 8) & 255}, ${n & 255}, ${a})`;
};

/** Same meaning as the player: fill strength and border pattern differ per state, not just hue. */
const BLOCK: Record<EventType, Record<string, string>> = {
  run: { backgroundColor: C.run, border: `2px solid ${C.run}` },
  io: { backgroundColor: alpha(C.io, 0.2), border: `2px solid ${C.io}` },
  blocked: {
    backgroundImage: `repeating-linear-gradient(45deg, ${alpha(C.blocked, 0.45)} 0 4px, transparent 4px 10px)`,
    border: `2px solid ${C.blocked}`,
  },
  wait: { border: `2px dashed ${C.wait}` },
  lock: { backgroundColor: alpha(C.lock, 0.24), border: `2px solid ${C.lock}` },
  error: { backgroundColor: alpha(C.error, 0.22), border: `2px solid ${C.error}` },
  fail: { backgroundColor: alpha(C.error, 0.22), border: `2px solid ${C.error}` },
  skip: { border: `2px dotted ${C.wait}` },
};

type Node = { type: string; props: { style?: Record<string, string | number>; children?: Node[] | Node | string } };
const el = (style: Record<string, string | number>, children?: Node[] | Node | string): Node => ({
  type: 'div',
  props: { style: { display: 'flex', ...style }, children },
});

const MAX_LANES = 4;
const LANE_H = 40;
const LABEL_W = 170;
const TRACK_W = WIDTH - 2 * 64 - LABEL_W;

function lanes(t: Pick<Timeline, 'lanes' | 'events' | 'duration'>): Node {
  const scale = TRACK_W / t.duration;
  const shown = t.lanes.slice(0, MAX_LANES);
  return el(
    { flexDirection: 'column', flexShrink: 0, height: shown.length * (LANE_H + 10) },
    shown.map((lane) =>
      el({ alignItems: 'center', height: LANE_H, marginBottom: 10, flexShrink: 0 }, [
        el({ width: LABEL_W, fontFamily: 'Plex Mono', fontSize: 20, color: C.muted, overflow: 'hidden' }, lane),
        el(
          { position: 'relative', width: TRACK_W, height: LANE_H, borderBottom: `1px solid ${C.border}` },
          t.events
            .filter((e) => e.lane === lane)
            .map((e) =>
              el({
                position: 'absolute',
                left: e.start * scale,
                top: 3,
                width: Math.max((e.end - e.start) * scale - 3, 4),
                height: LANE_H - 6,
                borderRadius: 4,
                ...BLOCK[e.type],
              }),
            ),
        ),
      ]),
    ),
  );
}

// The bundled fonts are the Latin subset, which has no arrows.
const clip = (s: string, n: number) => {
  const t = s.replace(/→/g, '->');
  return t.length <= n ? t : `${t.slice(0, t.lastIndexOf(' ', n))}…`;
};

export interface OgInput {
  eyebrow: string;
  title: string;
  subtitle?: string;
  timeline: Pick<Timeline, 'lanes' | 'events' | 'duration'>;
}

export async function renderOg({ eyebrow, title, subtitle, timeline }: OgInput): Promise<Uint8Array<ArrayBuffer>> {
  const titleSize = title.length > 48 ? 54 : 64;
  const tree = el(
    {
      width: WIDTH, height: HEIGHT, flexDirection: 'column', justifyContent: 'space-between', boxSizing: 'border-box',
      padding: 64, backgroundColor: C.bg, color: C.text, fontFamily: 'Plex Sans',
    },
    [
      el({ flexDirection: 'column', gap: 18 }, [
        el({ alignItems: 'center', gap: 14, fontFamily: 'Plex Mono', fontSize: 24 }, [
          el({ width: 30, height: 5, borderRadius: 3, backgroundImage: `linear-gradient(90deg, ${C.run} 55%, transparent 55%, transparent 65%, ${C.io} 65%)` }),
          el({ color: C.text }, 'Threadline'),
          el({ color: C.accent }, `· ${eyebrow}`),
        ]),
        el({ fontSize: titleSize, fontWeight: 600, lineHeight: 1.15, maxWidth: 1040 }, title),
        ...(subtitle ? [el({ fontSize: 28, lineHeight: 1.4, color: C.muted, maxWidth: 1040 }, clip(subtitle, 120))] : []),
      ]),
      lanes(timeline),
    ],
  );
  const svg = await satori(tree as Parameters<typeof satori>[0], { width: WIDTH, height: HEIGHT, fonts });
  return new Uint8Array(new Resvg(svg, { fitTo: { mode: 'width', value: WIDTH } }).render().asPng());
}
