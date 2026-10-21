import { defineCollection } from 'astro:content';
import { glob } from 'astro/loaders';
import { timelineSchema } from './lib/timeline';

const scenarios = defineCollection({
  loader: glob({ pattern: '**/*.{yaml,yml}', base: '../scenarios' }),
  schema: timelineSchema,
});

export const collections = { scenarios };
