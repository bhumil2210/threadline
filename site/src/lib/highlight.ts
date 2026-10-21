import { codeToTokens } from 'shiki';
import type { Timeline } from './timeline';

const SHIKI_LANG: Record<Timeline['lang'], string> = { java: 'java', go: 'go', rust: 'rust' };

const escape = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

/** Highlights code at build time into one HTML string per line, so the player only toggles classes. */
export async function highlightLines(code: string, lang: Timeline['lang']): Promise<string[]> {
  const { tokens } = await codeToTokens(code.replace(/\n$/, ''), {
    lang: SHIKI_LANG[lang] as 'java',
    theme: 'github-dark-default',
  });
  return tokens.map((line) =>
    line
      .map((tok) => {
        const style = [tok.color && `color:${tok.color}`, tok.fontStyle === 1 && 'font-style:italic']
          .filter(Boolean)
          .join(';');
        return style ? `<span style="${style}">${escape(tok.content)}</span>` : escape(tok.content);
      })
      .join(''),
  );
}
