// @ts-check
import { defineConfig } from 'astro/config';

import svelte from '@astrojs/svelte';
import mdx from '@astrojs/mdx';

// https://astro.build/config
export default defineConfig({
  site: 'https://threadline-be-fast.vercel.app', // keep in sync with SITE_URL in src/lib/site.ts
  output: 'static',
  trailingSlash: 'always',
  prefetch: { prefetchAll: true, defaultStrategy: 'hover' },
  // CSS is ~3.5 KB gzipped per page: inlining saves a render-blocking round trip on slow networks.
  build: { inlineStylesheets: 'always' },
  markdown: {
    // Both themes as CSS variables; global.css picks one, matching the site theme.
    shikiConfig: { themes: { dark: 'github-dark-default', light: 'github-light-default' }, defaultColor: false },
  },
  integrations: [svelte(), mdx()],
});
