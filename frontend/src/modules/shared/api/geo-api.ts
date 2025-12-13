import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const GEO_API_URL = `${API_BASE_URL}/api/geo`;

// --- Caches ---
let districtCache: string[] | null = null;
const municipalityCache = new Map<string, string[]>();

// --- Persistent Storage Keys ---
const DISTRICTS_STORAGE_KEY = 'geo:districts:v1';
const MUNICIPALITIES_STORAGE_KEY = 'geo:municipalities:v1';

let storedDistricts: string[] | null = null;
let storedMunicipalities: Record<string, string[]> | null = null;

// --- In-Flight Request Deduplication ---
// These prevent double-fetching (e.g., React Strict Mode) by storing the active promise
let districtRequestPromise: Promise<string[]> | null = null;
const municipalityRequests = new Map<string, Promise<string[]>>();

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

const dedupeAndFilter = (values: Array<string | undefined | null>) =>
  Array.from(new Set(values.filter((v): v is string => !!v && v.trim().length > 0)));

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
      const response = await withRetries(
        () => axios.get<string[]>(`${GEO_API_URL}/districts`, { timeout: GEO_BACKEND_TIMEOUT_MS }),
        3,
        800
      );

      const data = Array.isArray(response.data) ? response.data : [];
      if (data.length) {
        districtCache = data;
        storedDistricts = data;
        saveToStorage(DISTRICTS_STORAGE_KEY, data);
        return data;
      }

      console.warn('Backend returned empty districts');
      return [];
    } catch (error) {
      console.warn('Backend district fetch failed', error);
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

export const fetchMunicipalities = async (district: string): Promise<string[]> => {
  const normalizedDistrict = district.trim();
  if (!normalizedDistrict) return [];

  // Canonical key avoids mismatches due to case/spacing
  const cacheKey = normalizedDistrict.toLowerCase();
  const storageKey = `geo_api_municipality_${cacheKey}`;
  console.log('Checking cache for key:', storageKey, '| input district:', district);

  // 1. In-memory cache
  const cached = municipalityCache.get(cacheKey);
  if (cached) return cached;

  // 2. Per-district localStorage cache (strictly before any network promises)
  if (hasStorage) {
    const raw = window.localStorage.getItem(storageKey);
    if (raw !== null) {
      try {
        const parsed = JSON.parse(raw);
        const municipalities = Array.isArray(parsed) ? dedupeAndFilter(parsed) : [];
        if (municipalities.length) {
          municipalityCache.set(cacheKey, municipalities);
          return municipalities;
        }

        // Avoid treating empty cached results as authoritative (they may come from
        // previous rate-limit/timeout situations). Remove and continue.
        window.localStorage.removeItem(storageKey);
      } catch (error) {
        console.warn(`Failed to parse ${storageKey} from storage`, error);
        window.localStorage.removeItem(storageKey);
      }
    }
  }

  // 3. Aggregated storage (backward compatibility)
  if (!storedMunicipalities) {
    storedMunicipalities = loadFromStorage<Record<string, string[]>>(MUNICIPALITIES_STORAGE_KEY, {});
  }

  const stored = storedMunicipalities[cacheKey] ?? storedMunicipalities[normalizedDistrict];
  if (stored?.length) {
    municipalityCache.set(cacheKey, stored);
    saveToStorage(storageKey, stored);
    return stored;
  }

  // 4. In-flight dedupe
  if (municipalityRequests.has(cacheKey)) {
    return municipalityRequests.get(cacheKey)!;
  }

  // 5. Backend-only fetch
  const fetchLogic = async (): Promise<string[]> => {
    const encodedDistrict = encodeURIComponent(normalizedDistrict);

    try {
      const response = await withRetries(
        () =>
          axios.get<string[]>(`${GEO_API_URL}/districts/${encodedDistrict}/municipalities`, {
            timeout: GEO_BACKEND_TIMEOUT_MS,
          }),
        3,
        800
      );

      const data = Array.isArray(response.data) ? response.data : [];
      if (data.length) {
        municipalityCache.set(cacheKey, data);
        storedMunicipalities = storedMunicipalities ?? {};
        storedMunicipalities[cacheKey] = data;
        saveToStorage(MUNICIPALITIES_STORAGE_KEY, storedMunicipalities);
        saveToStorage(storageKey, data);
        return data;
      }

      console.warn('Backend returned empty municipalities');
      return [];
    } catch (error) {
      console.warn('Backend municipality fetch failed', error);
      return [];
    }
  };

  const promise = fetchLogic();
  municipalityRequests.set(cacheKey, promise);

  try {
    return await promise;
  } finally {
    municipalityRequests.delete(cacheKey);
  }
};