
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './index.css';
import './theme/theme.css';
import App from './App.jsx';
import { clearLegacyAuthStorage } from './api/authStorage.js';
import { ThemeProvider } from './theme/ThemeContext.jsx';

clearLegacyAuthStorage();

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>
);