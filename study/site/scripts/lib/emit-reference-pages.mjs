// Copies the atlas top-level reference .md files into docs/ as MDX.
// - Source is read-only.
// - We prepend a front-matter block so Docusaurus assigns a stable id/slug.
// - We rewrite backticked repo-relative source paths into <SourceLink> chips
//   and let autolinkFqcnsInMdx wrap Java fqcns as <Fqcn> chips, so the
//   ENTRYPOINTS / METRICS / CONFIG_KEYS / PROTOBUF_MAP / DESIGN_DOCS /
//   UPGRADES / GAPS tables become clickable end-to-end. The autolinker
//   already sanitizes MDX-hostile characters (`{`, `}`, stray `<`) so the
//   hand-authored atlas prose survives being reparsed as MDX.

import fs from 'node:fs/promises';
import path from 'node:path';
import {atlasRoot, docsRoot} from './paths.mjs';
import {autolinkFqcnsInMdx} from './remark-fqcn-autolink.mjs';
import {info, warn} from './log.mjs';

// Map of atlas source file -> (docs sub-path, id, title, sidebar_label,
// linkify). Reference files that host tables of repo paths / fqcns are
// emitted as .mdx so <SourceLink> and <Fqcn> chips render as JSX components.
// Glossary and prerequisites stay as .md (verbatim, plain prose only).
const REFERENCE_TARGETS = [
  {src: 'REPO_MAP.md',      dst: 'reference/repo-map.mdx',      id: 'reference/repo-map',      label: 'Repo map',      linkify: true},
  {src: 'ENTRYPOINTS.md',   dst: 'reference/entrypoints.mdx',   id: 'reference/entrypoints',   label: 'Entry points',  linkify: true},
  {src: 'PROTOBUF_MAP.md',  dst: 'reference/protobuf-map.mdx',  id: 'reference/protobuf-map',  label: 'Protobuf map',  linkify: true},
  {src: 'CONFIG_KEYS.md',   dst: 'reference/config-keys.mdx',   id: 'reference/config-keys',   label: 'Config keys',   linkify: true},
  {src: 'METRICS.md',       dst: 'reference/metrics.mdx',       id: 'reference/metrics',       label: 'Metrics',       linkify: true},
  {src: 'GLOSSARY.md',      dst: 'reference/glossary.md',       id: 'reference/glossary',      label: 'Glossary',      linkify: false},
  {src: 'PREREQUISITES.md', dst: 'reference/prerequisites.md',  id: 'reference/prerequisites', label: 'Prerequisites', linkify: false},
  {src: 'DESIGN_DOCS.md',   dst: 'reference/design-docs.mdx',   id: 'reference/design-docs',   label: 'Design docs',   linkify: true},
  {src: 'UPGRADES.md',      dst: 'reference/upgrades.mdx',      id: 'reference/upgrades',      label: 'Upgrades',      linkify: true},
  {src: 'GAPS.md',          dst: 'reference/gaps.mdx',          id: 'reference/gaps',          label: 'Gaps',          linkify: true},
];

// The guide is composed from INDEX.md + SCHEDULE.md; step 1 keeps the body
// verbatim (side-by-side sections). Later steps overlay reading-order tiles.
const GUIDE_SOURCES = [
  {file: 'INDEX.md',    heading: 'Reading order (from INDEX.md)'},
  {file: 'SCHEDULE.md', heading: 'Schedule (from SCHEDULE.md)'},
];

// The atlas markdown was hand-authored as GitHub-flavored Markdown, not MDX.
// Docusaurus 3 sends bare .md through the MDX loader by default, which then
// chokes on stray `{...}` fragments (javadoc `{@link ...}` refs, RocksDB
// placeholders like `{keyName}`, etc.). For files that stay .md we add
// `format: md` to force plain CommonMark and copy bodies verbatim. For
// files emitted as .mdx (so we can splice in <SourceLink>/<Fqcn> chips),
// autolinkFqcnsInMdx handles the escaping.
async function copyWithFrontMatter(src, dst, frontMatter, transform = (s) => s, {mdx = false} = {}) {
  const body = await fs.readFile(src, 'utf8');
  const transformed = transform(body);
  const formatLine = mdx ? '' : '\nformat: md';
  const stitched = `---\n${frontMatter}${formatLine}\n---\n\n${transformed}`;
  await fs.mkdir(path.dirname(dst), {recursive: true});
  await fs.writeFile(dst, stitched);
}

// Repo-relative source paths in reference tables look like
// `hadoop-hdds/.../Foo.java`, `hadoop-ozone/.../Bar.proto`, or a
// documentation path under `hadoop-hdds/docs/content/...`. Match a
// backticked chunk that starts with one of the top-level repo roots and
// swap it for a <SourceLink>. Line-numbered forms (`.../Foo.java:123`) are
// supported too so the SourceLink carries a `line=` attribute.
const REPO_PATH_RE = /`((?:hadoop-hdds|hadoop-ozone|dev-support)\/[^`]+?\.(?:java|proto|md|xml|py|sh|yaml|yml))(?::(\d+))?`/g;

function linkifyRepoPaths(body) {
  return body.replace(REPO_PATH_RE, (_whole, p, line) => {
    const lineAttr = line ? ` line={${line}}` : '';
    // Show the last segment of the path as the chip label so a wide table
    // cell does not blow the row width; the anchor's title attribute
    // (rendered by SourceLink) still reveals the full path on hover.
    const short = p.split('/').pop() + (line ? `:${line}` : '');
    return `<SourceLink path="${p}"${lineAttr}><code>${short}</code></SourceLink>`;
  });
}

/**
 * Reference tables usually put the fqcn in a backticked cell, e.g.
 * ``| `org.apache.hadoop.ozone.om.OzoneManager` | …``. The autolinker skips
 * inline code spans by design, so we unwrap those cells first when the
 * whole cell is a single known fqcn — matching how emit-feature-pages.mjs
 * handles class-table rows.
 */
function linkifyBacktickedFqcnCells(body, fqcnIndex) {
  // 1) Table cells whose entire content is one fqcn: ``| `fqcn` |``.
  let out = body.replace(
    /(?<=\|\s)`([a-z][a-zA-Z0-9_.]*\.[A-Z][A-Za-z0-9_$]*)`(?=\s*\|)/g,
    (whole, fqcn) => (fqcnIndex[fqcn] ? `<Fqcn>${fqcn}</Fqcn>` : whole),
  );
  // 2) Bullet items and prose that lead with `- `fqcn`` — reference pages
  //    like GAPS.md list one fqcn per bullet. Match at line start followed
  //    by "- " (or "* ") so we do not touch fqcns nested inside sentences,
  //    which the general autolinker handles once we escape the ticks.
  out = out.replace(
    /^(\s*[-*]\s+)`([a-z][a-zA-Z0-9_.]*\.[A-Z][A-Za-z0-9_$]*)`(?=[ \t]*(?:—|-|·|$))/gm,
    (whole, prefix, fqcn) => (fqcnIndex[fqcn] ? `${prefix}<Fqcn>${fqcn}</Fqcn>` : whole),
  );
  return out;
}

/**
 * Rewrite links to atlas siblings so they resolve inside the site.
 * Only touches bare `FOO.md` / `FOO_BAR.md` (all-caps) references — the
 * top-level atlas reference files — and remaps them to /reference/<slug>.
 * INDEX.md becomes the guide page itself.
 */
function rewriteAtlasSiblingLinks(body) {
  const map = {
    'REPO_MAP.md':      '/reference/repo-map',
    'ENTRYPOINTS.md':   '/reference/entrypoints',
    'PROTOBUF_MAP.md':  '/reference/protobuf-map',
    'CONFIG_KEYS.md':   '/reference/config-keys',
    'METRICS.md':       '/reference/metrics',
    'GLOSSARY.md':      '/reference/glossary',
    'PREREQUISITES.md': '/reference/prerequisites',
    'DESIGN_DOCS.md':   '/reference/design-docs',
    'UPGRADES.md':      '/reference/upgrades',
    'GAPS.md':          '/reference/gaps',
    'INDEX.md':         '/guide/reading-order',
    'SCHEDULE.md':      '/guide/reading-order#schedule-from-schedulemd',
    'PROGRESS.md':      '/progress',
    'README.md':        '/reference/repo-map',
  };
  // Match markdown link targets `](FOO.md)` (with or without an anchor).
  let out = body.replace(/\]\(([A-Z_]+\.md)(#[^)]*)?\)/g, (whole, file, anchor) => {
    const mapped = map[file];
    if (!mapped) return whole;
    return `](${mapped}${anchor ?? ''})`;
  });
  // atlas INDEX.md points at feature files as `components/<comp>/<feature>.md`.
  // Those are emitted at `/component/<comp>/<feature>` starting in step 3.
  // Rewriting them now keeps the guide free of broken-link warnings and the
  // URLs will resolve once step 3 lands.
  out = out.replace(
    /\]\(components\/([a-z0-9-]+)\/([a-z0-9._-]+)\.md(#[^)]*)?\)/g,
    (_whole, comp, feature, anchor) =>
      `](/component/${comp}/${feature}${anchor ?? ''})`,
  );
  return out;
}

export async function emitReferencePages({fqcnIndex} = {fqcnIndex: {}}) {
  // If .md siblings from an earlier build are on disk (e.g. reference/repo-map.md
  // before we started emitting .mdx), remove them so Docusaurus does not see
  // two docs with the same route.
  for (const t of REFERENCE_TARGETS) {
    if (!t.dst.endsWith('.mdx')) continue;
    const staleMd = path.join(docsRoot, t.dst.replace(/\.mdx$/, '.md'));
    try {
      await fs.rm(staleMd);
    } catch {
      /* ignore */
    }
  }

  for (const t of REFERENCE_TARGETS) {
    const src = path.join(atlasRoot, t.src);
    const dst = path.join(docsRoot, t.dst);
    try {
      const transform = t.linkify
        ? (body) => {
            const withSiblingLinks = rewriteAtlasSiblingLinks(body);
            const withPathChips = linkifyRepoPaths(withSiblingLinks);
            // Turn `fqcn` cells into <Fqcn> chips before the general
            // autolinker sees them — the autolinker skips inline code spans.
            const withCellChips = linkifyBacktickedFqcnCells(withPathChips, fqcnIndex);
            // Autolink remaining Java fqcns in prose and sanitize
            // MDX-hostile characters. The autolinker's tokenizer skips
            // markdown link targets and inline code spans, so the
            // <SourceLink> attributes we just emitted stay untouched.
            return autolinkFqcnsInMdx(withCellChips, fqcnIndex, {unresolved: new Set()});
          }
        : rewriteAtlasSiblingLinks;
      await copyWithFrontMatter(
        src,
        dst,
        // Doc id is derived from the file path (reference/glossary). We omit
        // `id:` in front-matter because Docusaurus rejects slashes in explicit
        // ids. `slug:` still controls the public URL.
        `slug: /${t.id}\nsidebar_label: ${JSON.stringify(t.label)}`,
        transform,
        {mdx: t.dst.endsWith('.mdx')},
      );
      info(`emit ${t.dst}`);
    } catch (err) {
      warn(`missing atlas reference file: ${t.src}`, {err: String(err.message)});
    }
  }

  // Guide page: stitch INDEX.md and SCHEDULE.md, then apply guide-specific
  // rewrites (component/sub-feature cross-links, gantt chart week fix).
  const guideChunks = ['---',
    'slug: /guide/reading-order',
    'sidebar_label: "Reading order"',
    'format: md',
    '---',
    '',
    '# Reading order',
    '',
    'This page stitches together `study/atlas/INDEX.md` and `study/atlas/SCHEDULE.md` verbatim.',
    '',
  ];
  for (const g of GUIDE_SOURCES) {
    const p = path.join(atlasRoot, g.file);
    try {
      let body = rewriteAtlasSiblingLinks(await fs.readFile(p, 'utf8'));
      body = rewriteIndexTable(body);
      body = fixGanttChart(body);
      guideChunks.push(`## ${g.heading}`, '', body, '');
    } catch (err) {
      warn(`missing atlas file: ${g.file}`, {err: String(err.message)});
    }
  }
  const dst = path.join(docsRoot, 'guide', 'reading-order.md');
  await fs.mkdir(path.dirname(dst), {recursive: true});
  await fs.writeFile(dst, guideChunks.join('\n'));
  info('emit guide/reading-order.md');
}

/**
 * Rewrite the "Components (reading-order)" table in INDEX.md so both the
 * bare Component name (e.g. `Client`) and the backticked Sub-feature token
 * (e.g. `client-facade`) become clickable. The Feature column already has
 * a link — we leave it alone.
 *
 * Docusaurus generates heading anchors via github-slugger, which strips
 * non-alphanumerics except hyphens. `### Sub-feature: `client.io`` becomes
 * the anchor `sub-feature-clientio`; we mirror that transform below.
 */
function rewriteIndexTable(body) {
  const compSlug = (c) => c.toLowerCase().replace(/[^a-z0-9]+/g, '-');
  // github-slugger (Docusaurus' anchor generator) preserves hyphens and
  // underscores but drops dots and other punctuation. Mirror that:
  // `client-facade` → `client-facade`, `client.io` → `clientio`,
  // `ipc_.metrics` → `ipc_metrics`.
  const subAnchor = (s) => 'sub-feature-' + s.toLowerCase().replace(/[^a-z0-9_-]+/g, '');
  // Match one row of the INDEX components table. Anchor on the trailing
  // "reading_order range" column so we don't match unrelated tables.
  return body.replace(
    /^(\|\s*\d+\s*\|\s*)([^|]+?)(\s*\|\s*\[)([a-z0-9._-]+)(\]\(\/component\/[a-z0-9-]+\/[a-z0-9._-]+\))(\s*\|\s*)`([^`]+)`(\s*\|\s*\d+\s*\|\s*[0-9–\-]+\s*\|)/gm,
    (_whole, pfx, compName, midA, feature, featureLink, midB, sub, tail) => {
      const comp = compName.trim();
      const compLink = `[${comp}](/component/${compSlug(comp)}/)`;
      const featurePath = featureLink.match(/\/component\/([a-z0-9-]+\/[a-z0-9._-]+)/)?.[1];
      const subLink = featurePath
        ? `[\`${sub}\`](/component/${featurePath}#${subAnchor(sub)})`
        : `\`${sub}\``;
      return `${pfx}${compLink}${midA}${feature}${featureLink}${midB}${subLink}${tail}`;
    },
  );
}

/**
 * The SCHEDULE.md Gantt block was authored with `dateFormat X` (epoch
 * seconds) and `4d` / `2d` / `9d` durations, so mermaid renders every task
 * within week 0 (W4 → W5 → W1 wrapping the day-of-week axis). The numeric
 * start offsets and duration integers were actually meant to be weeks —
 * they sum to 26 across the six P0/P1/P2/P3 phases. Switch to `dateFormat
 * YYYY-MM-DD` and stamp real Monday-anchored dates so mermaid draws weeks
 * on the axis. The transform is idempotent: if a future SCHEDULE.md
 * already uses `YYYY-MM-DD` we leave it alone.
 */
function fixGanttChart(body) {
  const startRe = /```mermaid\n([\s\S]*?)```/g;
  return body.replace(startRe, (whole, inner) => {
    if (!/^\s*gantt\b/m.test(inner)) return whole;
    if (!/dateFormat\s+X\b/.test(inner)) return whole;
    // Pick an arbitrary Monday as week 1 start; the axis shows week numbers
    // so the calendar year is not shown. 2024-01-01 was a Monday.
    const epochMonday = new Date(Date.UTC(2024, 0, 1));
    function addWeeks(w) {
      const d = new Date(epochMonday.getTime());
      d.setUTCDate(d.getUTCDate() + (w - 1) * 7);
      return d.toISOString().slice(0, 10);
    }
    let out = inner
      .replace(/dateFormat\s+X\b/, 'dateFormat  YYYY-MM-DD')
      .replace(/axisFormat\s+W%w\b/, 'axisFormat  W%V')
      .replace(
        /^(\s*(?:P\d+|M\d+)[^:\n]*:[^,\n]+,\s*)(\d+)(\s*,\s*)(\d+)d\b/gm,
        (_ln, prefix, startWeek, comma, durWeeks) => {
          return `${prefix}${addWeeks(Number(startWeek))}${comma}${durWeeks}w`;
        },
      );
    return '```mermaid\n' + out + '```';
  });
}
