module.exports = {
  id: 'security',
  title: 'Security',
  description:
    'SCM internal CA, block tokens, S3 secrets, delegation tokens, Kerberos, and the ' +
    'Ranger/authorizer path. Only classes from the Security component are included.',
  tags: ['security'],
  selectors: [
    {kind: 'match', component: 'Security'},
  ],
};
