// Injects atlas components into every MDX file's global scope so authors
// can use `<SourceLink>` / `<Fqcn>` (added in step 3) without imports.
import MDXComponents from '@theme-original/MDXComponents';
import SourceLink from '@site/src/components/SourceLink';
import Fqcn from '@site/src/components/Fqcn';
import Backlinks from '@site/src/components/Backlinks';
import ClassBadge from '@site/src/components/ClassBadge';
import ProgressCheckbox from '@site/src/components/ProgressCheckbox';
import UnresolvedNote from '@site/src/components/UnresolvedNote';

export default {
  ...MDXComponents,
  SourceLink,
  Fqcn,
  Backlinks,
  ClassBadge,
  ProgressCheckbox,
  UnresolvedNote,
};
