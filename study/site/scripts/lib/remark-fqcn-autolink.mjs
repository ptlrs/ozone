// Build-time remark plugin. Not a runtime plugin registered in
// docusaurus.config.js — we invoke this during MDX emission instead so we
// can be selective about which files see it (only generated blocks).
//
// Scans text nodes for fqcn-shaped tokens and, when they are in the known
// fqcn-index, replaces the text node with an MDX JSX expression
// `<Fqcn>the.fqcn</Fqcn>`. Untouched otherwise.
//
// Applied by scripts/lib/emit-*.mjs against the emitted MDX string before
// it hits disk — string-level, no AST parsing. This keeps the pipeline
// simple and predictable, and it composes with the mermaid-click pass in
// step 5 which also operates on the string.

/** Escape regexp meta characters. */
function esc(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// Match a "Java-ish" fully-qualified token: 3+ dot-separated identifiers
// starting with a lowercase package segment and ending with a Class name.
// Kept intentionally strict so it does not eat "study.atlas.tmp" style
// prose. The last segment must start with an uppercase letter.
export const FQCN_TOKEN = /(?<![A-Za-z0-9_./-])((?:[a-z][a-zA-Z0-9_]*\.){2,}[A-Z][A-Za-z0-9_$]*)(?![A-Za-z0-9_$.])/g;

/**
 * Replace fqcn tokens in a plain (non-code) markdown string with
 * <Fqcn>fqcn</Fqcn>. Skips fenced code blocks, indented code, inline
 * `code spans`, MDX component tags, and link targets.
 *
 * Also sanitizes MDX-hostile stray characters (`{`, `}`, unbalanced `<`)
 * in unprotected prose so text pulled verbatim from atlas .md files (which
 * were authored as GitHub Markdown, not MDX) does not blow up the MDX
 * parser. Fenced code, inline code, real MDX component tags, and link
 * targets stay untouched.
 *
 * @param {string} md
 * @param {Object} fqcnIndex — map from fqcn to any truthy value
 * @param {{unresolved: Set<string>}} sink — populated with tokens that
 *   looked like fqcns but were not in the index; the caller decides what
 *   to do with them (usually a UnresolvedNote footer).
 */
export function autolinkFqcnsInMdx(md, fqcnIndex, sink) {
  const lines = md.split('\n');
  let inFence = false;
  const out = [];
  for (const raw of lines) {
    // Toggle on ``` fences (with or without a language).
    if (/^\s*```/.test(raw)) {
      inFence = !inFence;
      out.push(raw);
      continue;
    }
    if (inFence) {
      out.push(raw);
      continue;
    }
    out.push(rewriteLine(raw, fqcnIndex, sink));
  }
  return out.join('\n');
}

// Recognized MDX component tags — the atlas emit path uses only these.
// Anything else that looks like a `<tag>` gets escaped.
// Only our custom components + a very small set of HTML tags MDX will
// accept without a paired closing (details / summary must always come
// paired, so we don't whitelist unmatched raw HTML tags — anything that
// isn't in this set gets escaped, which is what we want for the
// atlas-authored feature files that were written as plain Markdown).
const KNOWN_MDX_TAGS = new Set([
  'Fqcn', 'SourceLink', 'Backlinks', 'ClassBadge', 'ProgressCheckbox',
  'UnresolvedNote', 'details', 'summary',
  // Plain HTML tags MDX accepts as-is. Used for the meta chip strip on
  // class pages, the mono hint in view reading lists, and small card layouts.
  'div', 'span', 'code', 'p', 'strong', 'ul', 'li', 'a',
]);

function isKnownTag(raw) {
  // raw is like "<Foo ...>" or "</Foo>" or "<Foo/>".
  const m = raw.match(/^<\/?\s*([A-Za-z][A-Za-z0-9]*)/);
  if (!m) return false;
  return KNOWN_MDX_TAGS.has(m[1]);
}

function escapeMdxHostileText(s) {
  // Escape `{` and `}` so MDX doesn't read them as expressions.
  return s.replace(/[{}]/g, (c) => (c === '{' ? '&#123;' : '&#125;'));
}

function rewriteLine(line, fqcnIndex, sink) {
  // Split the line into pieces: `code spans` are protected as-is; everything
  // else is scanned for fqcn tokens. This is a lightweight tokenizer.
  const pieces = [];
  let i = 0;
  while (i < line.length) {
    // Skip whole <Fqcn>...</Fqcn> spans in one shot so the inner fqcn text
    // is not re-wrapped (that produces <Fqcn><Fqcn>fqcn</Fqcn></Fqcn>, which
    // renders as "[object Object]").
    if (line.startsWith('<Fqcn>', i)) {
      const end = line.indexOf('</Fqcn>', i + 6);
      if (end !== -1) {
        pieces.push({protected: true, text: line.slice(i, end + '</Fqcn>'.length)});
        i = end + '</Fqcn>'.length;
        continue;
      }
    }
    if (line[i] === '`') {
      const end = line.indexOf('`', i + 1);
      if (end === -1) {
        pieces.push({protected: false, text: line.slice(i)});
        break;
      }
      pieces.push({protected: true, text: line.slice(i, end + 1)});
      i = end + 1;
      continue;
    }
    // MDX component tags: only PRESERVE if it's one of KNOWN_MDX_TAGS.
    // Anything else (e.g. `<init>` from javadoc, `<br>` inside a table cell
    // that happens to be adjacent to prose) gets escaped so MDX treats it
    // as literal text.
    if (line[i] === '<') {
      const end = line.indexOf('>', i + 1);
      if (end !== -1) {
        const chunk = line.slice(i, end + 1);
        if (isKnownTag(chunk)) {
          pieces.push({protected: true, text: chunk});
        } else {
          pieces.push({protected: true, text: '&lt;' + chunk.slice(1, -1) + '&gt;'});
        }
        i = end + 1;
        continue;
      }
      // Unbalanced '<' — escape it as literal so MDX moves on.
      pieces.push({protected: true, text: '&lt;'});
      i += 1;
      continue;
    }
    // Also protect markdown link/image targets `](...)`. We match the whole
    // `](...)` chunk starting right after a `]`.
    if (line[i] === ']' && line[i + 1] === '(') {
      const end = line.indexOf(')', i + 2);
      if (end !== -1) {
        pieces.push({protected: true, text: line.slice(i, end + 1)});
        i = end + 1;
        continue;
      }
      // Unbalanced — advance past the ']' so we don't loop.
      pieces.push({protected: true, text: ']('});
      i += 2;
      continue;
    }
    // Otherwise, collect a run up to the next protected marker.
    let j = i + 1;
    while (
      j < line.length &&
      line[j] !== '`' &&
      line[j] !== '<' &&
      !(line[j] === ']' && line[j + 1] === '(')
    ) {
      j++;
    }
    pieces.push({protected: false, text: line.slice(i, j)});
    i = j;
  }

  return pieces
    .map((p) => {
      if (p.protected) return p.text;
      // First escape MDX-hostile characters in the unprotected text, then
      // run the fqcn substitution.
      const safe = escapeMdxHostileText(p.text);
      const linked = safe.replace(FQCN_TOKEN, (whole, fqcn) => {
        if (fqcnIndex[fqcn]) {
          return `<Fqcn>${fqcn}</Fqcn>`;
        }
        if (sink) sink.unresolved.add(fqcn);
        return whole;
      });
      // Auto-link Apache Jira tickets. Only match tokens that are not already
      // inside a markdown link (previous char is not `[`) and only in
      // unprotected prose so a fenced example like `HDDS-1234` stays raw.
      return linked.replace(
        /(?<![[\w-])(HDDS-\d+)(?!\])/g,
        (m) => `[${m}](https://issues.apache.org/jira/browse/${m})`,
      );
    })
    .join('');
}
