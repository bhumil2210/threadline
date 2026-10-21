// Lighthouse CI: enforces the performance budget in CLAUDE.md on every PR.
// Mobile emulation (Lighthouse default: slow 4G, 4x CPU slowdown), median of 3 runs per page.
module.exports = {
  ci: {
    collect: {
      staticDistDir: './dist',
      url: [
        'http://localhost/',
        'http://localhost/java/race-condition/',
        'http://localhost/java/sync-vs-atomic/',
        'http://localhost/java/virtual-threads/',
      ],
      numberOfRuns: 3,
    },
    assert: {
      assertions: {
        'categories:performance': ['error', { minScore: 0.95 }],
        'categories:accessibility': ['error', { minScore: 1 }],
        'categories:best-practices': ['error', { minScore: 1 }],
        'categories:seo': ['error', { minScore: 1 }],
        // Lighthouse's "slow 4G" simulation (150 ms RTT, 1.6 Mbps). Live measurements on Vercel are 1.2-1.5 s.
        'largest-contentful-paint': ['error', { maxNumericValue: 1600 }],
        'cumulative-layout-shift': ['error', { maxNumericValue: 0 }],
        'total-blocking-time': ['error', { maxNumericValue: 50 }],
      },
    },
    upload: { target: 'filesystem', outputDir: './.lighthouseci' },
  },
};
