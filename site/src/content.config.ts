import { defineCollection } from 'astro:content';
import { glob } from 'astro/loaders';
import { timelineSchema } from './lib/timeline';

const scenarios = defineCollection({
  loader: glob({ pattern: '**/*.{yaml,yml}', base: '../scenarios' }),
  schema: timelineSchema,
});

/** Prose shown under each player. `scenarios/java/race-condition.mdx` pairs with the YAML of the same name. */
const writeups = defineCollection({
  loader: glob({ pattern: '**/*.mdx', base: '../scenarios' }),
});

export const collections = { scenarios, writeups };
