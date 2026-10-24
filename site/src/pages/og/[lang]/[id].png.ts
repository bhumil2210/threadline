import type { APIRoute, GetStaticPaths } from 'astro';
import { LANG_NAME, allScenarios } from '../../../lib/catalog';
import { renderOg } from '../../../lib/og';
import type { Timeline } from '../../../lib/timeline';

export const getStaticPaths = (async () =>
  (await allScenarios()).map(({ main }) => ({ params: { lang: main.lang, id: main.id }, props: { main } }))) satisfies GetStaticPaths;

export const GET: APIRoute<{ main: Timeline }> = async ({ props: { main } }) =>
  new Response(
    await renderOg({ eyebrow: `${LANG_NAME[main.lang]} · ${main.group}`, title: main.title, subtitle: main.summary, timeline: main }),
    { headers: { 'Content-Type': 'image/png' } },
  );
