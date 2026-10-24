import type { APIRoute, GetStaticPaths } from 'astro';
import { allScenarios, playerData } from '../../../lib/catalog';
import type { Timeline } from '../../../lib/timeline';

/**
 * Every timeline of a scenario with what-if variants, main first. The page only embeds the main one,
 * which keeps its first load small; the player fetches this file when the browser is idle.
 */
export const getStaticPaths = (async () =>
  (await allScenarios())
    .filter((s) => s.variants.length > 0)
    .map(({ main, variants }) => ({
      params: { lang: main.lang, id: main.id },
      props: { timelines: [main, ...variants] },
    }))) satisfies GetStaticPaths;

export const GET: APIRoute = async ({ props }) => {
  const { variants } = await playerData(props.timelines as Timeline[]);
  return new Response(JSON.stringify(variants), { headers: { 'Content-Type': 'application/json' } });
};
