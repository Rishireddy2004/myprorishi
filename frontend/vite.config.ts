import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 3000,
    allowedHosts: [
      'localhost',
      '.ngrok-free.app',
      '.ngrok-free.dev',
      '.ngrok.io',
    ],
    proxy: {
      '/api': { target: 'http://localhost:8080', rewrite: (path) => path.replace(/^\/api/, '') },
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
})
