import { getCollection } from 'astro:content';
import type { Timeline } from './timeline';

export const LANG_NAME = { java: 'Java', go: 'Go', rust: 'Rust' } as const;
const LANG_ORDER = ['java', 'go', 'rust'] as const;

export const scenarioUrl = (s: Pick<Timeline, 'lang' | 'id'>) => `/${s.lang}/${s.id}/`;

/** All scenarios in reading order: by language, then `order`. */
export async function allScenarios(): Promise<Timeline[]> {
  const entries = await getCollection('scenarios');
  return entries
    .map((e) => e.data)
    .sort((a, b) => LANG_ORDER.indexOf(a.lang) - LANG_ORDER.indexOf(b.lang) || a.order - b.order);
}

/** Groups in first-appearance order, each with its scenarios in reading order. */
export function byGroup(scenarios: Timeline[]): { group: string; items: Timeline[] }[] {
  const groups = new Map<string, Timeline[]>();
  for (const s of scenarios) groups.set(s.group, [...(groups.get(s.group) ?? []), s]);
  return [...groups].map(([group, items]) => ({ group, items }));
}
