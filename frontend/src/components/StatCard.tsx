interface StatCardProps {
  label: string;
  value: number;
  hint?: string;
}

export function StatCard({ label, value, hint }: StatCardProps) {
  return (
    <div className="card stat-card">
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value.toLocaleString()}</span>
      {hint ? <span className="stat-hint">{hint}</span> : null}
    </div>
  );
}
