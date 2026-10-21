import { codeToTokensWithThemes } from 'shiki';
import type { Timeline } from './timeline';

const SHIKI_LANG: Record<Timeline['lang'], string> = { java: 'java', go: 'go', rust: 'rust' };
const ITALIC = 1;
/** github-light's comment gray is 4.5:1 on white and fails on highlighted lines; use GitHub's darker gray. */
const LIGHT_REMAP: Record<string, string> = { '#6E7781': '#57606A' };

const escape = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

export interface Highlighted {
  /** One HTML string per line, so the player only toggles classes. */
  lines: string[];
  /** Defines --d / --l for each color class used; global.css picks one per theme. */
  css: string;
}

/**
 * Highlights code at build time for both themes. Each distinct (dark, light) color pair becomes one
 * short class, which keeps the page small compared with inline styles on every token.
 */
export async function highlightLines(code: string, lang: Timeline['lang']): Promise<Highlighted> {
  const tokens = await codeToTokensWithThemes(code.replace(/\n$/, ''), {
    lang: SHIKI_LANG[lang] as 'java',
    themes: { dark: 'github-dark-default', light: 'github-light-default' },
  });

  const classes = new Map<string, string>();
  const classFor = (dark: string, light: string) => {
    const key = `${dark}|${light}`;
    if (!classes.has(key)) classes.set(key, `k${classes.size}`);
    return classes.get(key)!;
  };

  const lines = tokens.map((line) =>
    line
      .map((tok) => {
        const dark = tok.variants.dark;
        const light = tok.variants.light;
        if (!dark?.color || !light?.color) return escape(tok.content);
        const cls = ['tk', classFor(dark.color, LIGHT_REMAP[light.color.toUpperCase()] ?? light.color)];
        if ((dark.fontStyle ?? 0) & ITALIC) cls.push('it');
        return `<span class="${cls.join(' ')}">${escape(tok.content)}</span>`;
      })
      .join(''),
  );

  const css = [...classes]
    .map(([key, cls]) => {
      const [dark, light] = key.split('|');
      return `.${cls}{--d:${dark};--l:${light}}`;
    })
    .join('');

  return { lines, css };
}
