import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  // Kotlin/JS の出力はローカルの file: 依存なので、事前バンドルの対象から外す
  optimizeDeps: { exclude: ['todo-shared'] },
})
