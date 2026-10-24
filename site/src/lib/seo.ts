import { SITE_NAME, SITE_URL } from './site';

export const REPO_URL = 'https://github.com/bhumil2210/threadline';

const escapeHtml = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

/** Short prose with `backtick` code spans (write-up Q&As) → safe HTML. */
export const inlineCodeHtml = (s: string) =>
  escapeHtml(s).replace(/`([^`]+)`/g, (_, code: string) => `<code>${code}</code>`);

/** The same prose as plain text, for structured data. */
export const plainText = (s: string) => s.replace(/`([^`]+)`/g, '$1');

/** JSON for a `<script type="application/ld+json">`. Escaping `<` keeps a "</script>" in the data from ending the tag. */
export const jsonLd = (data: unknown) => JSON.stringify(data).replace(/</g, '\\u003c');

export const absoluteUrl = (path: string) => new URL(path, SITE_URL).href;

/** The runnable twin a scenario is verified against (CLAUDE.md: scenario `foo-bar` → package `foobar`). */
export function verifyProgramUrl(lang: string, id: string): string | undefined {
  if (lang !== 'java') return undefined;
  return `${REPO_URL}/blob/main/verify/java/src/main/java/threadline/java/${id.replace(/-/g, '')}/Main.java`;
}

export const pageTitle = (title: string) => `${title} | ${SITE_NAME}`;
