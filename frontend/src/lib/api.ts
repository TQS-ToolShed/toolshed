const PRIMARY_BASE = 'http://deti-tqs-09.ua.pt:8080';
const LOCALHOST_BASE = 'http://localhost:8080';

export const API_BASE_URL =
  import.meta.env?.VITE_API_URL ??
  (typeof window !== 'undefined' && window.location.hostname === 'localhost'
    ? LOCALHOST_BASE
    : PRIMARY_BASE);
