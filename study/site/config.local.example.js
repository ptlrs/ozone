// Copy this file to config.local.js and edit the values below.
// config.local.js is gitignored. If it is absent, the site runs in
// 'github' link mode and shows a banner pointing back at this example.
//
// The site reads this file at build time and inlines the values into
// static/atlas-config.js so they land on window.__ATLAS_CONFIG for the
// SourceLink and LinkModeToggle components.
module.exports = {
  // Absolute path to your local Ozone checkout. Used by the JetBrains
  // navigate URL. Must be the same project that IDEA opens.
  repoRoot: '/Users/rpatel/Github/ozone',

  // GitHub slug used when the toggle is in 'github' mode.
  githubRepo: 'apache/ozone',

  // Ref to link to on GitHub. A commit sha is safer than a branch name
  // because it does not drift while you read.
  githubRef: 'master',

  // The IDEA project name. Check your IDEA project's .idea/.name file
  // (or the window title) to be sure this matches.
  jetbrainsProject: 'ozone',

  // Default click-mode when nothing is stored in localStorage.
  // 'jetbrains' opens files in IDEA locally; 'github' opens them on the web.
  defaultMode: 'jetbrains',
};
