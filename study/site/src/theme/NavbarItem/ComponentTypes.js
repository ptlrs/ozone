// Extends Docusaurus's built-in navbar-item registry with our custom
// LinkModeToggle. Referenced from docusaurus.config.js as
//   {type: 'custom-linkModeToggle', position: 'right'}
import ComponentTypes from '@theme-original/NavbarItem/ComponentTypes';
import LinkModeToggle from '@site/src/components/LinkModeToggle';
import ProgressNavbar from '@site/src/components/ProgressNavbar';

export default {
  ...ComponentTypes,
  'custom-linkModeToggle': LinkModeToggle,
  'custom-progressNavbar': ProgressNavbar,
};
