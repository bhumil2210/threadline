import type { APIRoute } from 'astro';
import { SITE_URL } from '../lib/site';

// Everyone may crawl, AI crawlers included: being cited in AI answers brings readers too.
export const GET: APIRoute = () =>
  new Response(`User-agent: *\nAllow: /\n\nSitemap: ${SITE_URL}/sitemap.xml\n`, {
    headers: { 'Content-Type': 'text/plain' },
  });
