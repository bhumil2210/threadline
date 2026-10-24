import type { APIRoute } from 'astro';
import { LANG_NAME, allScenarios, scenarioUrl } from '../lib/catalog';
import { absoluteUrl } from '../lib/seo';

/** A plain-text map of the site for AI assistants (llmstxt.org): each scenario with its one-line takeaway. */
export const GET: APIRoute = async () => {
  const lines = (await allScenarios()).map(
    ({ main }) => `- [${LANG_NAME[main.lang]}: ${main.title}](${absoluteUrl(scenarioUrl(main))}): ${main.takeaway}`,
  );
  const body = `# Threadline

> Concurrency, animated on real thread lanes: which thread runs what, step by step. Every timeline is checked against a real program in CI (JDK 21 and 25), asserting facts like "thenApply ran on the thread that completed the future".

## Scenarios

${lines.join('\n')}

## About

- [How we verify](${absoluteUrl('/how-we-verify/')}): how each timeline is tested against real execution
`;
  return new Response(body, { headers: { 'Content-Type': 'text/plain; charset=utf-8' } });
};
