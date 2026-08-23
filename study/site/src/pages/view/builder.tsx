import React from 'react';
import Layout from '@theme/Layout';
import BrowserOnly from '@docusaurus/BrowserOnly';
import ViewBuilder from '@site/src/components/ViewBuilder';

export default function ViewBuilderPage(): JSX.Element {
  return (
    <Layout title="View builder" description="Build a custom reading view over the atlas.">
      <BrowserOnly fallback={<div className="container margin-vert--lg">Loading view builder…</div>}>
        {() => (
          <div className="container margin-vert--lg">
            <h1>View builder</h1>
            <p>Predicate builder over the atlas. Save locally, or export as a real view module.</p>
            <ViewBuilder />
          </div>
        )}
      </BrowserOnly>
    </Layout>
  );
}
