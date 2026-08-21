interface StatCardProps {
  label: string;
  value: number;
  hint?: string;
  loading?: boolean;
}

export function StatCard({ label, value, hint, loading = false }: StatCardProps) {
  return (
    <div className="card stat-card">
      <span className="stat-label">{label}</span>
      {loading ? (
        <>
          <span className="skeleton skeleton-value" />
          <span className="skeleton skeleton-hint" />
        </>
      ) : (
        <>
          <span className="stat-value">{value.toLocaleString()}</span>
          {hint ? <span className="stat-hint">{hint}</span> : null}
        </>
      )}
    </div>
  );
}
