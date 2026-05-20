/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: 'rgb(var(--wk-primary-rgb) / <alpha-value>)',
        'background-light': 'rgb(var(--wk-bg-page-rgb) / <alpha-value>)',
        'background-dark': 'rgb(var(--wk-bg-page-rgb) / <alpha-value>)',
      },
      fontFamily: {
        sans: [
          // '"Microsoft YaHei"',
          // '"微软雅黑"',
          // 'PingFang SC',
          // 'Helvetica Neue',
          // 'Arial',
          // 'Noto Sans SC',
          // 'ui-sans-serif',
          // 'system-ui',
          // 'sans-serif',
          '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Fira Sans', 'Helvetica Neue', 'sans-serif'
        ],
        display: [
          // '"Microsoft YaHei"',
          // '"微软雅黑"',
          // 'PingFang SC',
          // 'Helvetica Neue',
          // 'Arial',
          // 'Noto Sans SC',
          // 'sans-serif',
          '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Fira Sans', 'Helvetica Neue', 'sans-serif'
        ],
      },
    },
  },
  plugins: [],
}
