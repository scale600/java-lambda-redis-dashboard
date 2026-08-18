const DEFAULT_BASE_URL = 'https://7m2j372rk8.execute-api.us-east-1.amazonaws.com/prod';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? DEFAULT_BASE_URL;

export interface Overview {
  total: number;
  today: number;
  uniqueToday: number;
  lastUpdated: string;
}

export interface SeriesPoint {
  timestamp: string;
  count: number;
}

export interface Timeseries {
  granularity: 'hour' | 'day';
  series: SeriesPoint[];
}

export interface PathCount {
  path: string;
  count: number;
}

export interface Paths {
  paths: PathCount[];
}

export interface SiteCount {
  site: string;
  count: number;
}

export interface Sites {
  sites: SiteCount[];
}

export interface Visit {
  time: string;
  path: string;
  site?: string;
  ip: string;
  ua: string;
}

export interface RecentVisits {
  visits: Visit[];
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`);
  if (!res.ok) {
    throw new Error(`Request failed (${res.status})`);
  }
  return res.json() as Promise<T>;
}

export function fetchOverview(): Promise<Overview> {
  return get<Overview>('/stats/overview');
}

export function fetchTimeseries(granularity: 'hour' | 'day' = 'hour', limit = 24): Promise<Timeseries> {
  return get<Timeseries>(`/stats/timeseries?granularity=${granularity}&limit=${limit}`);
}

export function fetchPaths(limit = 20): Promise<Paths> {
  return get<Paths>(`/stats/paths?limit=${limit}`);
}

export function fetchSites(limit = 20): Promise<Sites> {
  return get<Sites>(`/stats/sites?limit=${limit}`);
}

export function fetchRecent(limit = 20): Promise<RecentVisits> {
  return get<RecentVisits>(`/stats/recent?limit=${limit}`);
}
