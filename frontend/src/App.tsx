import { StatCard } from './components/StatCard';
import { TimeseriesChart } from './components/TimeseriesChart';
import { PathsChart } from './components/PathsChart';
import { RecentVisits } from './components/RecentVisits';
import { SitesChart } from './components/SitesChart';
import { formatRelative } from './format';
import { RANGE_OPTIONS, useDashboardData } from './hooks/useDashboardData';

export default function App() {
  const {
    overview,
    timeseries,
    paths,
    sites,
    recent,
    loading,
    rangeLoading,
    error,
    lastFetched,
    range,
    setRange,
  } = useDashboardData();

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1>Traffic Dashboard</h1>
          <p className="subtitle">Real-time visitor monitoring</p>
        </div>
        <div className="header-status">
          {error ? (
            <span className="status-badge status-error">offline</span>
          ) : loading ? (
            <span className="status-badge">connecting…</span>
          ) : (
            <span className="status-badge status-ok">
              live · {lastFetched ? formatRelative(lastFetched) : ''}
            </span>
          )}
        </div>
      </header>

      {error ? (
        <div className="error-banner">
          Unable to reach the API ({error}). Showing last known data — retrying
          automatically.
        </div>
      ) : null}

      <section className="stat-grid">
        <StatCard label="Total visits" value={overview?.total ?? 0} loading={loading} />
        <StatCard
          label="Today"
          value={overview?.today ?? 0}
          hint="UTC"
          loading={loading}
        />
        <StatCard
          label="Unique today"
          value={overview?.uniqueToday ?? 0}
          hint="UTC"
          loading={loading}
        />
      </section>

      <section className="dashboard-grid">
        <div className="card panel">
          <div className="panel-header">
            <h2>Visitors over time</h2>
            <div className="range-controls">
              <span className="range-tz">UTC</span>
              <div className="range-toggle" role="group" aria-label="Time range">
                {RANGE_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`range-option${range === option.value ? ' is-active' : ''}`}
                    aria-pressed={range === option.value}
                    onClick={() => setRange(option.value)}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
          <TimeseriesChart data={timeseries} loading={loading || rangeLoading} />
        </div>

        <div className="card panel">
          <div className="panel-header">
            <h2>Top paths</h2>
            <span className="panel-meta">today</span>
          </div>
          <PathsChart data={paths} loading={loading} />
        </div>

        <div className="card panel">
          <div className="panel-header">
            <h2>Sites</h2>
            <span className="panel-meta">today</span>
          </div>
          <SitesChart data={sites} loading={loading} />
        </div>
      </section>

      <section className="card panel">
        <div className="panel-header">
          <h2>Recent visits</h2>
          <span className="panel-meta">latest 20</span>
        </div>
        <RecentVisits data={recent} loading={loading} />
      </section>
    </div>
  );
}
