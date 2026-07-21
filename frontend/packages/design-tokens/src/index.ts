import type { ThemeConfig } from 'antd';

export const commerceTheme: ThemeConfig = {
  token: {
    colorPrimary: '#0058be',
    colorInfo: '#0058be',
    colorSuccess: '#00855b',
    colorError: '#ba1a1a',
    colorBgLayout: '#f8f9ff',
    colorText: '#121c2a',
    colorTextSecondary: '#424754',
    borderRadius: 12,
    fontFamily: "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', sans-serif",
  },
  components: {
    Button: { controlHeight: 42, borderRadius: 12 },
    Input: { controlHeight: 42, borderRadius: 12 },
    Card: { borderRadiusLG: 18 },
  },
};
