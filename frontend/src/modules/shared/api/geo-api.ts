import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const GEO_API_URL = `${API_BASE_URL}/api/geo`;
const PUBLIC_FALLBACK_URL = 'https://json.geoapi.pt/distritos';

// Map GeoAPI response (objects) into a string array of district names
const normalizeDistricts = (raw: unknown): string[] => {
  if (Array.isArray(raw) && raw.length && typeof raw[0] === 'string') {
    return raw as string[];
  }
  if (Array.isArray(raw)) {
    return (raw as Array<Record<string, unknown>>)
      .map((item) => {
        const distrito = item?.distrito;
        return typeof distrito === 'string' ? distrito : null;
      })
      .filter((d): d is string => Boolean(d));
  }
  return [];
};

// --- Caches ---
let districtCache: string[] | null = null;

// --- Persistent Storage Keys ---
const DISTRICTS_STORAGE_KEY = 'geo:districts:v1';

let storedDistricts: string[] | null = null;

// --- In-Flight Request Deduplication ---
// These prevent double-fetching (e.g., React Strict Mode) by storing the active promise
let districtRequestPromise: Promise<string[]> | null = null;

// Backend may rate-limit or be slower when warming cache; allow more time
const GEO_BACKEND_TIMEOUT_MS = 30000;

const sleep = (ms: number) => new Promise((res) => setTimeout(res, ms));
const withRetries = async <T>(fn: () => Promise<T>, attempts = 3, baseDelayMs = 500): Promise<T> => {
  let lastErr: unknown;
  for (let i = 0; i < attempts; i++) {
    try {
      return await fn();
    } catch (err) {
      lastErr = err;
      const delay = baseDelayMs * Math.pow(2, i);
      await sleep(delay);
    }
  }
  throw lastErr;
};

const hasStorage = typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';

const loadFromStorage = <T>(key: string, fallback: T): T => {
  if (!hasStorage) return fallback;
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return fallback;
    const parsed = JSON.parse(raw) as T;
    return parsed ?? fallback;
  } catch (error) {
    console.warn(`Failed to load ${key} from storage`, error);
    return fallback;
  }
};

const saveToStorage = (key: string, value: unknown) => {
  if (!hasStorage) return;
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch (error) {
    console.warn(`Failed to save ${key} to storage`, error);
  }
};

export const fetchDistricts = async (): Promise<string[]> => {
  // 1. Return cached data immediately if available
  if (districtCache) return districtCache;

  // 1b. Load from persistent storage if present
  if (!storedDistricts) {
    storedDistricts = loadFromStorage<string[]>(DISTRICTS_STORAGE_KEY, []);
  }
  if (storedDistricts.length) {
    districtCache = storedDistricts;
    return districtCache;
  }

  // 2. Return existing in-flight request if one is already running
  if (districtRequestPromise) return districtRequestPromise;

  // 3. Create the fetch logic wrapped in a function (backend-only)
  const fetchLogic = async (): Promise<string[]> => {
    try {
      // Try backend first
      const response = await withRetries(
        () => axios.get<string[]>(`${GEO_API_URL}/districts`, { timeout: GEO_BACKEND_TIMEOUT_MS }),
        3,
        800
      );

      const data = normalizeDistricts(response.data);
      if (data.length) {
        districtCache = data;
        storedDistricts = data;
        saveToStorage(DISTRICTS_STORAGE_KEY, data);
        return data;
      }

      console.warn('Backend returned empty districts, falling back to public GeoAPI');
    } catch (error) {
      console.warn('Backend district fetch failed', error);
    }

    // Fallback: hit public GeoAPI directly if backend is empty or failed
    try {
      const response = await withRetries(
        () => axios.get<string[]>(PUBLIC_FALLBACK_URL, { timeout: GEO_BACKEND_TIMEOUT_MS }),
        3,
        800
      );
      const data = normalizeDistricts(response.data);
      districtCache = data;
      storedDistricts = data;
      saveToStorage(DISTRICTS_STORAGE_KEY, data);
      return data;
    } catch (fallbackError) {
      console.warn('Public GeoAPI district fetch failed', fallbackError);
      return [];
    }
  };

  // 4. Assign promise, await it, and clear it afterwards
  districtRequestPromise = fetchLogic();
  try {
    return await districtRequestPromise;
  } finally {
    districtRequestPromise = null;
  }
};
