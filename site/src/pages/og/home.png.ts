import type { APIRoute } from 'astro';
import { allScenarios } from '../../lib/catalog';
import { renderOg } from '../../lib/og';

/** The index's share image, drawn with the virtual threads timeline (busy lanes, every state at a glance). */
export const GET: APIRoute = async () => {
  const all = await allScenarios();
  const showcase = all.find(({ main }) => main.id === 'virtual-threads')?.main ?? all[0]!.main;
  return new Response(
    await renderOg({ eyebrow: 'Java concurrency, visualized', title: 'See which thread runs what.', subtitle: 'Threads, locks, executors, CompletableFuture and virtual threads, animated on real thread lanes.', timeline: showcase }),
    { headers: { 'Content-Type': 'image/png' } },
  );
};
