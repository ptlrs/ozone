#!/usr/bin/env python3
"""One-shot sanitizer applied on top of the existing atlas artifacts so
downstream MDX consumers (the study/site Docusaurus site) do not choke on
raw HTML fragments and javadoc references that leaked in from source-file
comments.

The transformation is the same one now baked into `escape_md` / `mdx_safe`
in emit_features.py, subfeatures.py, reorder_subs.py, reconcile.py. This
script rewrites study/atlas/atlas.json in place and rewrites
study/atlas/components/**/*.md in place. Both are idempotent.

The rules:
  - `{@link X}` and `{@code X}` → `X`
  - `&`     → `&amp;`
  - `<` `>` → `&lt;` `&gt;`
  - `{` `}` → `&#123;` `&#125;`

We DO NOT touch text inside fenced ``` code blocks or inside inline
`code spans`. `atlas.json`'s free-text fields never contain code, so the
JSON pass runs the substitution unconditionally on those fields.

Run from anywhere:
    python3 study/atlas/tmp/mdx_sanitize.py
"""

import json
import re
from pathlib import Path

# Locate study/atlas/ relative to this file so the script works regardless
# of CWD.
HERE = Path(__file__).resolve().parent
ATLAS = HERE.parent  # study/atlas/
COMPS = ATLAS / 'components'
ATLAS_JSON = ATLAS / 'atlas.json'


def mdx_safe(s):
    if not isinstance(s, str):
        return s
    s = re.sub(r'\{@link\s+([^}]+)\}', r'\1', s)
    s = re.sub(r'\{@code\s+([^}]+)\}', r'\1', s)
    s = s.replace('&', '&amp;')
    s = s.replace('<', '&lt;').replace('>', '&gt;')
    s = s.replace('{', '&#123;').replace('}', '&#125;')
    return s


def mdx_safe_preserving_code(md):
    """Same transform as `mdx_safe` but skip fenced code blocks and inline
    code spans. Used for hand-authored .md files that already contain
    fenced blocks with legitimate `<`/`{`/`}` (e.g. mermaid diagrams,
    Java identifiers)."""
    out = []
    in_fence = False
    for line in md.split('\n'):
        if line.lstrip().startswith('```'):
            in_fence = not in_fence
            out.append(line)
            continue
        if in_fence:
            out.append(line)
            continue
        out.append(_transform_inline(line))
    return '\n'.join(out)


def _transform_inline(line):
    # Split on inline `code spans` so their contents pass through.
    parts = []
    i = 0
    while i < len(line):
        if line[i] == '`':
            j = line.find('`', i + 1)
            if j == -1:
                parts.append(line[i:])
                break
            parts.append(line[i:j + 1])
            i = j + 1
            continue
        # Grab a run until the next backtick.
        j = line.find('`', i)
        if j == -1:
            parts.append(_transform_prose(line[i:]))
            break
        parts.append(_transform_prose(line[i:j]))
        i = j
    return ''.join(parts)


def _transform_prose(s):
    """Apply the mdx_safe transform, but avoid double-escaping existing
    HTML entities already present in the markdown (e.g. `&amp;`) and
    preserve legitimately-used MDX tags authored in the atlas source."""
    # Preserve `<details>`, `</details>`, `<summary>`, `</summary>` — the
    # atlas quiz sections use them intentionally as MDX components.
    KEEP_TAGS = r'</?(?:details|summary)>'
    ENT = r'&(amp|lt|gt|quot|#\d+);'
    tokens = []

    def swap(m):
        tokens.append(m.group(0))
        return f'\x00{len(tokens) - 1}\x00'

    # Order matters: guard entities first, then the whitelisted tags.
    guarded = re.sub(ENT, swap, s)
    guarded = re.sub(KEEP_TAGS, swap, guarded)
    # Same as mdx_safe.
    guarded = re.sub(r'\{@link\s+([^}]+)\}', r'\1', guarded)
    guarded = re.sub(r'\{@code\s+([^}]+)\}', r'\1', guarded)
    guarded = guarded.replace('&', '&amp;')
    guarded = guarded.replace('<', '&lt;').replace('>', '&gt;')
    guarded = guarded.replace('{', '&#123;').replace('}', '&#125;')

    def unswap(m):
        idx = int(m.group(1))
        return tokens[idx]

    return re.sub(r'\x00(\d+)\x00', unswap, guarded)


def sanitize_atlas_json():
    data = json.loads(ATLAS_JSON.read_text())
    changed = 0
    for row in data:
        # role_one_liner
        before = row.get('role_one_liner', '') or ''
        after = mdx_safe(before)
        if after != before:
            row['role_one_liner'] = after
            changed += 1
        # persistence has a couple of raw fragments
        if 'persistence' in row and isinstance(row['persistence'], str):
            after = mdx_safe(row['persistence'])
            if after != row['persistence']:
                row['persistence'] = after
                changed += 1
        # invariants / sharp_edges: transform each string
        for f in ('invariants', 'sharp_edges'):
            if f in row and isinstance(row[f], list):
                new_list = [mdx_safe(x) if isinstance(x, str) else x for x in row[f]]
                if new_list != row[f]:
                    row[f] = new_list
                    changed += 1
    ATLAS_JSON.write_text(json.dumps(data, indent=1) + '\n')
    print(f'atlas.json: {changed} fields sanitized in {len(data)} rows')


def sanitize_component_md():
    files = sorted(COMPS.glob('*/*.md'))
    touched = 0
    for p in files:
        raw = p.read_text()
        new = mdx_safe_preserving_code(raw)
        if new != raw:
            p.write_text(new)
            touched += 1
    print(f'components/**/*.md: {touched} of {len(files)} files rewritten')


if __name__ == '__main__':
    sanitize_atlas_json()
    sanitize_component_md()
