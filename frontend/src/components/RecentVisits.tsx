import type { RecentVisits } from '../api';
import { formatDateTime } from '../format';

interface RecentVisitsProps {
  data: RecentVisits | null;
  loading?: boolean;
}

export function RecentVisits({ data, loading = false }: RecentVisitsProps) {
  if (loading) {
    return (
      <div className="table-wrap">
        <div className="skeleton-rows">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="skeleton skeleton-row" />
          ))}
        </div>
      </div>
    );
  }

  const visits = data?.visits ?? [];

  return (
    <div className="table-wrap">
      <table className="visits-table">
        <thead>
          <tr>
            <th>Time (UTC)</th>
            <th>Path</th>
            <th>IP</th>
            <th>User-Agent</th>
          </tr>
        </thead>
        <tbody>
          {visits.length === 0 ? (
            <tr>
              <td colSpan={4} className="empty-cell">
                No visits recorded yet
              </td>
            </tr>
          ) : (
            visits.map((v, i) => (
              <tr key={`${v.time}-${i}`}>
                <td className="mono">{formatDateTime(v.time)}</td>
                <td className="path-cell">{v.path}</td>
                <td className="mono">{v.ip}</td>
                <td className="ua-cell" title={v.ua}>
                  {v.ua || '—'}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
