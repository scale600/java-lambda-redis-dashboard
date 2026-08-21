import { useCallback, useEffect, useRef, useState } from 'react';
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

export type TimeRange = '24h' | '7d' | '30d';

export const RANGE_OPTIONS: { value: TimeRange; label: string }[] = [
  { value: '24h', label: '24H' },
  { value: '7d', label: '7D' },
  { value: '30d', label: '30D' },
];

const RANGE_CONFIG: Record<
  TimeRange,
  { granularity: 'hour' | 'day'; limit: number }
> = {
  '24h': { granularity: 'hour', limit: 24 },
  '7d': { granularity: 'day', limit: 7 },
  '30d': { granularity: 'day', limit: 30 },
};

export interface DashboardState {
  overview: Overview | null;
  timeseries: Timeseries | null;
  paths: Paths | null;
  recent: RecentVisits | null;
  loading: boolean;
  rangeLoading: boolean;
  error: string | null;
  lastFetched: string | null;
}

export interface Dashboard extends DashboardState {
  range: TimeRange;
  setRange: (range: TimeRange) => void;
}

const initialState: DashboardState = {
  overview: null,
  timeseries: null,
  paths: null,
  recent: null,
  loading: true,
  rangeLoading: false,
  error: null,
  lastFetched: null,
};

export function useDashboardData(): Dashboard {
  const [state, setState] = useState<DashboardState>(initialState);
  const [range, setRangeState] = useState<TimeRange>('24h');
  const rangeRef = useRef<TimeRange>('24h');

  const load = useCallback(async (r: TimeRange) => {
    const { granularity, limit } = RANGE_CONFIG[r];
    try {
      const [overview, timeseries, paths, recent] = await Promise.all([
        fetchOverview(),
        fetchTimeseries(granularity, limit),
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
        rangeLoading: false,
        error: null,
        lastFetched: new Date().toISOString(),
      }));
    } catch (err) {
      setState((prev) => ({
        ...prev,
        loading: false,
        rangeLoading: false,
        error: err instanceof Error ? err.message : 'Failed to load data',
      }));
    }
  }, []);

  const loadCurrent = useCallback(() => load(rangeRef.current), [load]);

  useEffect(() => {
    loadCurrent();
    const id = window.setInterval(loadCurrent, POLL_INTERVAL_MS);
    return () => window.clearInterval(id);
  }, [loadCurrent]);

  const setRange = useCallback(
    (r: TimeRange) => {
      if (r === rangeRef.current) {
        return;
      }
      rangeRef.current = r;
      setRangeState(r);
      setState((prev) => ({ ...prev, rangeLoading: true }));
      void load(r);
    },
    [load],
  );

  return { ...state, range, setRange };
}
