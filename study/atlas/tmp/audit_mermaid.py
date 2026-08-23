#!/usr/bin/env python3
"""Extract every mermaid block from every atlas markdown file and flag issues.

Common bugs in agent-authored diagrams:
  - `flowchart` type declared but contains classDiagram syntax (<|--, *--, o--, <<...>>)
  - `sequenceDiagram` type declared but contains classDiagram or flowchart syntax
  - `\n` inside node labels (mermaid requires <br/> or short single-line)
  - Unquoted node labels containing '(', ')', '/', ':', '.'
  - `classDiagram` blocks with wrong link arrows
"""
import re
from pathlib import Path
from collections import Counter

ROOT = Path('study/atlas')
issues = []

# extract mermaid blocks
MERMAID_RE = re.compile(r'```mermaid\n(.*?)\n```', re.DOTALL)

def check_block(md_path, block, lineno):
    lines = block.splitlines()
    first = next((l.strip() for l in lines if l.strip() and not l.strip().startswith('%')), '')
    kind = first.split()[0] if first else '?'
    # normalize a couple of common types
    is_flow = kind.startswith('flowchart') or kind == 'graph'
    is_class = kind == 'classDiagram'
    is_seq  = kind == 'sequenceDiagram'
    is_state = kind.startswith('stateDiagram')
    is_er   = kind == 'erDiagram'
    is_gantt = kind == 'gantt'
    is_xy    = kind == 'xychart-beta'

    body = '\n'.join(lines[1:])

    if is_flow:
        # classDiagram-only tokens that shouldn't appear
        if re.search(r'<\|--|\*--|<--\||--\*|--o|o--', body):
            issues.append((md_path, 'flowchart-uses-classDiagram-arrows', lineno))
        if re.search(r'<<\w+>>', body):
            issues.append((md_path, 'flowchart-uses-stereotype', lineno))
        # \n inside node text
        if re.search(r'\["[^"]*\\n[^"]*"\]|\("[^"]*\\n[^"]*"\)', body):
            issues.append((md_path, 'flowchart-node-label-has-\\n', lineno))
        # bare label with newline
        if re.search(r'\[[^\]]*\n', body):
            issues.append((md_path, 'flowchart-node-label-crosses-line', lineno))
    if is_seq:
        if re.search(r'<\|--|\*--|--\*|--o|o--', body):
            issues.append((md_path, 'sequenceDiagram-uses-classDiagram-arrows', lineno))
        if re.search(r'-->\|', body):
            pass  # ok
    if is_class:
        # classDiagram-specific: shape braces are allowed; check for sequence syntax
        if re.search(r'participant\s+\w', body):
            issues.append((md_path, 'classDiagram-uses-participant', lineno))
    if is_state:
        # stateDiagram often mis-uses classDiagram arrows
        if re.search(r'<\|--', body):
            issues.append((md_path, 'stateDiagram-uses-classDiagram-arrows', lineno))

    # Universal: unbalanced brackets or unquoted labels with reserved chars
    # These are frequent culprits.
    for i, l in enumerate(lines):
        # A node like  Foo[label with (parens)]  breaks parsing
        m = re.search(r'\[([^\]"]*?[():\/\.@,][^\]"]*?)\]', l)
        if m and not l.strip().startswith('%') and is_flow:
            issues.append((md_path, f'flowchart-unquoted-label-special-char: {m.group(1)[:40]}', lineno + i))
    # end

for md in ROOT.rglob('*.md'):
    if md.suffix != '.md': continue
    text = md.read_text()
    # find each mermaid block with its start line
    idx = 0
    while True:
        m = MERMAID_RE.search(text, idx)
        if not m: break
        block = m.group(1)
        # compute lineno of the start of the block
        lineno = text.count('\n', 0, m.start()) + 1
        check_block(md.relative_to(ROOT), block, lineno)
        idx = m.end()

# Also count total mermaid blocks per file
total_blocks = 0
per_file = Counter()
for md in ROOT.rglob('*.md'):
    text = md.read_text()
    n = len(MERMAID_RE.findall(text))
    if n:
        per_file[md.relative_to(ROOT)] = n
        total_blocks += n

print(f'total mermaid blocks: {total_blocks}')
print(f'files with diagrams: {len(per_file)}')
print(f'issues found: {len(issues)}')
if issues:
    by_type = Counter(x[1].split(':')[0] for x in issues)
    for k,n in by_type.most_common():
        print(f'  {n}  {k}')
    print()
    print('first 25:')
    for f,t,ln in issues[:25]:
        print(f'  {f}:{ln}  {t}')
