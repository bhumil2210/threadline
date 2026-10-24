import { getCollection } from 'astro:content';
import { highlightLines } from './highlight';
import { toPlayerTimeline, type PlayerTimeline, type Timeline } from './timeline';

export const LANG_NAME = { java: 'Java', go: 'Go', rust: 'Rust' } as const;
const LANG_ORDER = ['java', 'go', 'rust'] as const;

export const scenarioUrl = (s: Pick<Timeline, 'lang' | 'id'>) => `/${s.lang}/${s.id}/`;

/** A main scenario with its what-if variants (the main timeline first). */
export interface Scenario {
  main: Timeline;
  variants: Timeline[];
}

/**
 * All main scenarios in reading order (by language, then `order`), each with its variants.
 * Fails the build when a variant points at a missing scenario or a switch label is missing.
 */
export async function allScenarios(): Promise<Scenario[]> {
  const all = (await getCollection('scenarios')).map((e) => e.data);
  const mains = all.filter((t) => !t.variantOf);
  const byId = new Map(mains.map((t) => [`${t.lang}/${t.id}`, t]));

  for (const v of all.filter((t) => t.variantOf)) {
    const main = byId.get(`${v.lang}/${v.variantOf}`);
    if (!main) throw new Error(`${v.lang}/${v.id}: variantOf "${v.variantOf}" is not a main scenario`);
    if (!main.variant) throw new Error(`${main.lang}/${main.id} has variants, so it needs a "variant" label too`);
  }

  return mains
    .sort((a, b) => LANG_ORDER.indexOf(a.lang) - LANG_ORDER.indexOf(b.lang) || a.order! - b.order!)
    .map((main) => ({
      main,
      variants: all
        .filter((t) => t.variantOf === main.id && t.lang === main.lang)
        .sort((a, b) => (a.order ?? Infinity) - (b.order ?? Infinity) || a.id.localeCompare(b.id)),
    }));
}

/** Groups in first-appearance order, each with its scenarios in reading order. */
export function byGroup(scenarios: Scenario[]): { group: string; items: Scenario[] }[] {
  const groups = new Map<string, Scenario[]>();
  for (const s of scenarios) groups.set(s.main.group, [...(groups.get(s.main.group) ?? []), s]);
  return [...groups].map(([group, items]) => ({ group, items }));
}

/** What the player needs per timeline: its data and its code, highlighted at build time. */
export interface PlayerVariant {
  timeline: PlayerTimeline;
  codeLines: string[];
}

/**
 * Player data for a scenario and its variants, main first. `css` covers the code colors of all of them,
 * and is the same on the page and in variants.json because both highlight the same blocks in order.
 */
export async function playerData(timelines: Timeline[]): Promise<{ css: string; variants: PlayerVariant[] }> {
  const code = await highlightLines(
    timelines.map((t) => t.code),
    timelines[0]!.lang,
  );
  return {
    css: code.css,
    variants: timelines.map((t, i) => ({ timeline: toPlayerTimeline(t), codeLines: code.lines[i]! })),
  };
}

export const variantsUrl = (s: Pick<Timeline, 'lang' | 'id'>) => `${scenarioUrl(s)}variants.json`;
