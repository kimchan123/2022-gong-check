import { css } from '@emotion/react';

import animation from '@/styles/animation';
import theme from '@/styles/theme';

const slideMenu = (isShowMenu: boolean | null) => css`
  position: absolute;
  min-height: 100vh;
  background-color: ${theme.colors.white};
  z-index: 1000000;
  width: 100%;

  animation: ${isShowMenu === true ? animation.navigatorOpen : animation.navigatorClose} 0.2s ease-out;
  animation-fill-mode: forwards;
`;

const styles = { slideMenu };

export default styles;
