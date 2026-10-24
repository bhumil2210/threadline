import type { APIRoute } from 'astro';
import { allScenarios, scenarioUrl } from '../lib/catalog';
import { SITE_URL } from '../lib/site';

/** Every indexable page: the index, how-we-verify and one page per main scenario (variants live on their main's page). */
export const GET: APIRoute = async () => {
  const paths = ['/', '/how-we-verify/', ...(await allScenarios()).map(({ main }) => scenarioUrl(main))];
  const urls = paths.map((p) => `  <url><loc>${new URL(p, SITE_URL).href}</loc></url>`).join('\n');
  const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`;
  return new Response(xml, { headers: { 'Content-Type': 'application/xml' } });
};
