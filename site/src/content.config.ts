import { defineCollection } from 'astro:content';
import { glob } from 'astro/loaders';
import { z } from 'astro/zod';
import { timelineSchema } from './lib/timeline';

const scenarios = defineCollection({
  loader: glob({ pattern: '**/*.{yaml,yml}', base: '../scenarios' }),
  schema: timelineSchema,
});

/**
 * Prose shown under each player. `scenarios/java/race-condition.mdx` pairs with the YAML of the same name.
 * Frontmatter holds page-level search metadata: it describes the page, not the timeline.
 */
const writeups = defineCollection({
  loader: glob({ pattern: '**/*.mdx', base: '../scenarios' }),
  schema: z.object({
    seoTitle: z.string().min(1).max(60).optional(), // <title> and share title, phrased like the search people type
    description: z.string().min(1).max(160).optional(), // meta description; falls back to the summary
    faq: z.array(z.object({ q: z.string().min(1), a: z.string().min(1) })).default([]), // "Common questions"
  }),
});

export const collections = { scenarios, writeups };
