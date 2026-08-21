import type { ChartData, ChartOptions } from 'chart.js';
import { Bar } from 'react-chartjs-2';
import type { Paths } from '../api';

const MAX_TITLE_LENGTH = 42;

function wrapLabel(text: string): string[] {
  const lines: string[] = [];
  let rest = text;
  while (rest.length > MAX_TITLE_LENGTH) {
    const slash = rest.lastIndexOf('/', MAX_TITLE_LENGTH);
    const cut = slash > 0 ? slash + 1 : MAX_TITLE_LENGTH;
    lines.push(rest.slice(0, cut));
    rest = rest.slice(cut);
  }
  if (rest.length > 0) {
    lines.push(rest);
  }
  return lines;
}

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
        callbacks: {
          title: (items) => {
            const label = items[0]?.label;
            return label ? wrapLabel(label) : '';
          },
          label: (item) => `Views: ${item.parsed.x}`,
        },
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
