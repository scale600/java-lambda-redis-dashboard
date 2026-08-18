import type { RecentVisits } from '../api';
import { formatDateTime } from '../format';

interface RecentVisitsProps {
  data: RecentVisits | null;
}

export function RecentVisits({ data }: RecentVisitsProps) {
  const visits = data?.visits ?? [];

  return (
    <div className="table-wrap">
      <table className="visits-table">
        <thead>
          <tr>
            <th>Time</th>
            <th>Path</th>
            <th>Site</th>
            <th>IP</th>
            <th>User-Agent</th>
          </tr>
        </thead>
        <tbody>
          {visits.length === 0 ? (
            <tr>
              <td colSpan={5} className="empty-cell">
                No visits recorded yet
              </td>
            </tr>
          ) : (
            visits.map((v, i) => (
              <tr key={`${v.time}-${i}`}>
                <td className="mono">{formatDateTime(v.time)}</td>
                <td className="path-cell">{v.path}</td>
                <td className="site-cell">{v.site || '—'}</td>
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
