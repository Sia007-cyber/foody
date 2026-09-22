import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'
import { apiBaseUrl } from './src/lib/apiBaseUrl.ts'

export default defineConfig(({ command, mode }) => {
  if (command === 'build') apiBaseUrl(loadEnv(mode, process.cwd(), 'VITE_').VITE_API_BASE_URL, true)
  return {
    plugins: [
      react(),
      VitePWA({
        registerType: 'prompt',
        injectRegister: null,
        manifest: {
          id: '/',
          name: 'فودی | سفارش و مدیریت کسب‌وکار',
          short_name: 'فودی',
          description: 'سامانه سفارش، رزرو و مدیریت کسب‌وکار فودی',
          lang: 'fa',
          dir: 'rtl',
          start_url: '/',
          scope: '/',
          display: 'standalone',
          background_color: '#ffffff',
          theme_color: '#ff6b00',
          icons: [
            { src: '/pwa-192x192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
            { src: '/pwa-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
            { src: '/pwa-maskable-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
          ],
        },
        workbox: {
          globPatterns: ['**/*.{js,css,html,svg,png,woff2}'],
          globIgnores: ['pwa-*.png'],
          navigateFallback: 'index.html',
          navigateFallbackDenylist: [/^\/api(?:\/|$)/, /^\/uploads(?:\/|$)/],
          cleanupOutdatedCaches: true,
          runtimeCaching: [],
        },
      }),
    ],
  }
})
