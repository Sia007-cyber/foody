import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import { apiBaseUrl } from './src/lib/apiBaseUrl.ts'

export default defineConfig(({ command, mode }) => {
  if (command === 'build') apiBaseUrl(loadEnv(mode, process.cwd(), 'VITE_').VITE_API_BASE_URL, true)
  return { plugins: [react()] }
})
