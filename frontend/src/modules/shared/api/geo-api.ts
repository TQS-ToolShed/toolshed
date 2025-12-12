import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const GEO_API_URL = `${API_BASE_URL}/api/geo`;
const PUBLIC_GEO_API_URL = 'https://json.geoapi.pt';

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

const GEO_BACKEND_TIMEOUT_MS = 15000;
const GEO_PUBLIC_TIMEOUT_MS = 8000;

const hasStorage = typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';

type PublicDistrict = { distrito?: string };
type PublicMunicipalityResponse = { distrito?: string; municipios?: string[] };

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

  // 3. Create the fetch logic wrapped in a function
  const fetchLogic = async (): Promise<string[]> => {
    try {
      // Try Backend
      const response = await axios.get<string[]>(`${GEO_API_URL}/districts`, {
        timeout: GEO_BACKEND_TIMEOUT_MS,
      });

      if (response.data?.length) {
        districtCache = response.data;
        storedDistricts = response.data;
        saveToStorage(DISTRICTS_STORAGE_KEY, response.data);
        return response.data;
      }
      console.warn('Backend returned empty districts; falling back to public GeoAPI');
    } catch (error) {
      console.warn('Backend district fetch failed; falling back to public GeoAPI', error);
    }

    try {
      // Try Public API
      const response = await axios.get<PublicDistrict[]>(`${PUBLIC_GEO_API_URL}/distritos`, {
        timeout: GEO_PUBLIC_TIMEOUT_MS,
      });

      const districts = dedupeAndFilter(response.data?.map((d) => d.distrito) ?? []);
      districtCache = districts;
      storedDistricts = districts;
      saveToStorage(DISTRICTS_STORAGE_KEY, districts);
      return districts;
    } catch (error) {
      console.error('Failed to fetch districts', error);
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

  // Use a canonical key to avoid mismatches due to case or extra spaces
  const cacheKey = normalizedDistrict.toLowerCase();
  const storageKey = `geo_api_municipality_${cacheKey}`;
  console.log('Checking cache for key:', storageKey, '| input district:', district);

  // 1. Return cached data immediately
  const cached = municipalityCache.get(cacheKey);
  if (cached) return cached;

  // 1b. Load from persistent storage (per-district key) before any network call
  if (hasStorage) {
    const raw = window.localStorage.getItem(storageKey);
    if (raw !== null) {
      try {
        const parsed = JSON.parse(raw);
        const municipalities = Array.isArray(parsed) ? dedupeAndFilter(parsed) : [];
        municipalityCache.set(cacheKey, municipalities);
        return municipalities;
      } catch (error) {
        console.warn(`Failed to parse ${storageKey} from storage`, error);
        return [];
      }
    }
  }

  // 1c. Load from aggregated persistent storage for backward compatibility
  if (!storedMunicipalities) {
    storedMunicipalities = loadFromStorage<Record<string, string[]>>(
      MUNICIPALITIES_STORAGE_KEY,
      {}
    );
  }

  const stored = storedMunicipalities[cacheKey] ?? storedMunicipalities[normalizedDistrict];
  if (stored?.length) {
    municipalityCache.set(cacheKey, stored);
    // Also write through to the per-district key to prevent future misses
    saveToStorage(storageKey, stored);
    return stored;
  }

  // 2. Return existing in-flight request if one is already running for THIS district
  if (municipalityRequests.has(cacheKey)) {
    return municipalityRequests.get(cacheKey)!;
  }

  // 3. Define the fetch logic
  const fetchLogic = async (): Promise<string[]> => {
    const encodedDistrict = encodeURIComponent(normalizedDistrict);

    try {
      // Try Backend
      const response = await axios.get<string[]>(
        `${GEO_API_URL}/districts/${encodedDistrict}/municipalities`,
        { timeout: GEO_BACKEND_TIMEOUT_MS }
      );

      if (response.data?.length) {
        municipalityCache.set(cacheKey, response.data);
        storedMunicipalities = storedMunicipalities ?? {};
        storedMunicipalities[cacheKey] = response.data;
        saveToStorage(MUNICIPALITIES_STORAGE_KEY, storedMunicipalities);
        saveToStorage(storageKey, response.data);
        return response.data;
      }
      console.warn('Backend returned empty municipalities; falling back to public GeoAPI');
    } catch (error) {
      console.warn('Backend municipality fetch failed; falling back to public GeoAPI', error);
    }

    try {
      // Try Public API
      const response = await axios.get<PublicMunicipalityResponse>(
        `${PUBLIC_GEO_API_URL}/distrito/${encodedDistrict}/municipios`,
        { timeout: GEO_PUBLIC_TIMEOUT_MS }
      );

      const municipalities = dedupeAndFilter(response.data?.municipios ?? []);
      municipalityCache.set(cacheKey, municipalities);
      storedMunicipalities = storedMunicipalities ?? {};
      storedMunicipalities[cacheKey] = municipalities;
      saveToStorage(MUNICIPALITIES_STORAGE_KEY, storedMunicipalities);
      saveToStorage(storageKey, municipalities);
      return municipalities;
    } catch (error) {
      console.error('Failed to fetch municipalities', error);
      return [];
    }
  };

  // 4. Store promise in map, await it, and remove from map when done
  const promise = fetchLogic();
  municipalityRequests.set(cacheKey, promise);

  try {
    return await promise;
  } finally {
    municipalityRequests.delete(cacheKey);
  }
};