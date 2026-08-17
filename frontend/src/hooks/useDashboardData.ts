import { useCallback, useEffect, useState } from 'react';
import {
  fetchOverview,
  fetchPaths,
  fetchRecent,
  fetchTimeseries,
  type Overview,
  type Paths,
  type RecentVisits,
  type Timeseries,
} from '../api';

const POLL_INTERVAL_MS = 60_000;

export interface DashboardState {
  overview: Overview | null;
  timeseries: Timeseries | null;
  paths: Paths | null;
  recent: RecentVisits | null;
  loading: boolean;
  error: string | null;
  lastFetched: string | null;
}

const initialState: DashboardState = {
  overview: null,
  timeseries: null,
  paths: null,
  recent: null,
  loading: true,
  error: null,
  lastFetched: null,
};

export function useDashboardData(): DashboardState {
  const [state, setState] = useState<DashboardState>(initialState);

  const load = useCallback(async () => {
    try {
      const [overview, timeseries, paths, recent] = await Promise.all([
        fetchOverview(),
        fetchTimeseries('hour', 24),
        fetchPaths(20),
        fetchRecent(20),
      ]);
      setState((prev) => ({
        ...prev,
        overview,
        timeseries,
        paths,
        recent,
        loading: false,
        error: null,
        lastFetched: new Date().toISOString(),
      }));
    } catch (err) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: err instanceof Error ? err.message : 'Failed to load data',
      }));
    }
  }, []);

  useEffect(() => {
    load();
    const id = window.setInterval(load, POLL_INTERVAL_MS);
    return () => window.clearInterval(id);
  }, [load]);

  return state;
}
