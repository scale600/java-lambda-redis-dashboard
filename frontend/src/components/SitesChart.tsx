import type { ChartData, ChartOptions } from 'chart.js';
import { Bar } from 'react-chartjs-2';
import type { Sites } from '../api';

interface SitesChartProps {
  data: Sites | null;
  loading?: boolean;
}

export function SitesChart({ data, loading = false }: SitesChartProps) {
  if (loading) {
    return (
      <div className="chart-container">
        <div className="skeleton skeleton-chart" />
      </div>
    );
  }

  const entries = data?.sites ?? [];
  const labels = entries.map((s) => s.site);
  const counts = entries.map((s) => s.count);

  const chartData: ChartData<'bar'> = {
    labels,
    datasets: [
      {
        label: 'Visits',
        data: counts,
        backgroundColor: 'rgba(129, 140, 248, 0.65)',
        borderColor: '#818cf8',
        borderWidth: 1,
        borderRadius: 4,
        barThickness: 16,
      },
    ],
  };

  const options: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1c2230',
        borderColor: '#2a3342',
        borderWidth: 1,
        titleColor: '#e6edf3',
        bodyColor: '#c9d1d9',
        padding: 12,
        displayColors: false,
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        ticks: { precision: 0 },
        grid: { color: 'rgba(255, 255, 255, 0.04)' },
      },
      y: {
        grid: { display: false },
        ticks: { maxTicksLimit: 20 },
      },
    },
  };

  return (
    <div className="chart-container">
      <Bar data={chartData} options={options} />
    </div>
  );
}
