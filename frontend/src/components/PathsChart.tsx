import type { ChartData, ChartOptions } from 'chart.js';
import { Bar } from 'react-chartjs-2';
import type { Paths } from '../api';

interface PathsChartProps {
  data: Paths | null;
  loading?: boolean;
}

export function PathsChart({ data, loading = false }: PathsChartProps) {
  if (loading) {
    return (
      <div className="chart-container">
        <div className="skeleton skeleton-chart" />
      </div>
    );
  }

  const entries = data?.paths ?? [];
  const labels = entries.map((p) => p.path);
  const counts = entries.map((p) => p.count);

  const chartData: ChartData<'bar'> = {
    labels,
    datasets: [
      {
        label: 'Views',
        data: counts,
        backgroundColor: 'rgba(45, 212, 191, 0.65)',
        borderColor: '#2dd4bf',
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
