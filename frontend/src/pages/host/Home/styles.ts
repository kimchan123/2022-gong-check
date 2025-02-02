import { css } from '@emotion/react';

import theme from '@/styles/theme';

const header = css`
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100vw;
  height: 64px;
  background-color: ${theme.colors.white};
  box-shadow: 0px 2px 2px 2px ${theme.colors.shadow20};

  img {
    height: 40px;
    transform: translateY(4px);
  }
`;

const layout = css`
  display: flex;
  padding: 48px 0 64px 0;
  height: calc(100vh - 64px);
  flex-direction: column;
  align-items: center;
  justify-content: space-around;

  img {
    width: 80%;
    max-width: 420px;
  }
`;

const styles = { header, layout };

export default styles;
