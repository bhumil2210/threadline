import { describe, expect, it } from 'vitest';
import { inlineCodeHtml, jsonLd, plainText, verifyProgramUrl } from './seo';

describe('seo helpers', () => {
  it('renders code spans and escapes HTML', () => {
    expect(inlineCodeHtml('Use `List<T>` & <b>')).toBe('Use <code>List&lt;T&gt;</code> &amp; &lt;b&gt;');
  });

  it('strips code spans for plain text', () => {
    expect(plainText('Call `join()` first')).toBe('Call join() first');
  });

  it('keeps JSON-LD from closing its script tag', () => {
    expect(jsonLd({ a: '</script>' })).not.toContain('</script>');
  });

  it('maps a scenario id to its Java twin', () => {
    expect(verifyProgramUrl('java', 'race-condition')).toMatch(/threadline\/java\/racecondition\/Main\.java$/);
    expect(verifyProgramUrl('go', 'x')).toBeUndefined();
  });
});
