import type { ChartData, ChartOptions } from 'chart.js';
import { Line } from 'react-chartjs-2';
import type { Timeseries } from '../api';
import { formatDay, formatHour } from '../format';

interface TimeseriesChartProps {
  data: Timeseries | null;
  loading?: boolean;
}

export function TimeseriesChart({ data, loading = false }: TimeseriesChartProps) {
  if (loading) {
    return (
      <div className="chart-container">
        <div className="skeleton skeleton-chart" />
      </div>
    );
  }

  const isDaily = data?.granularity === 'day';
  const labels =
    data?.series.map((p) =>
      isDaily ? formatDay(p.timestamp) : formatHour(p.timestamp),
    ) ?? [];
  const counts = data?.series.map((p) => p.count) ?? [];

  const chartData: ChartData<'line'> = {
    labels,
    datasets: [
      {
        label: 'Visitors',
        data: counts,
        borderColor: '#818cf8',
        backgroundColor: 'rgba(129, 140, 248, 0.14)',
        fill: true,
        tension: 0.35,
        pointRadius: 0,
        pointHitRadius: 12,
        borderWidth: 2,
      },
    ],
  };

  const options: ChartOptions<'line'> = {
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
        grid: { display: false },
        ticks: { maxTicksLimit: 8, maxRotation: 0 },
      },
      y: {
        beginAtZero: true,
        ticks: { precision: 0 },
      },
    },
  };

  return (
    <div className="chart-container">
      <Line data={chartData} options={options} />
    </div>
  );
}
