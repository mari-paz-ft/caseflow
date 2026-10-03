import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    environmentOptions: {
      jsdom: { url: 'http://localhost' },
    },
    clearMocks: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/bff/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
