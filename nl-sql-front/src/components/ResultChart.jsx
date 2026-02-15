import { useMemo } from 'react'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, PointElement, LineElement,
  BarElement, ArcElement, Tooltip, Legend,
} from 'chart.js'
import { Bar, Line, Pie, Scatter } from 'react-chartjs-2'
import { detectChartType, buildChartConfig } from '../utils/chartDetector'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend)

const CHART_COMPONENTS = { bar: Bar, line: Line, pie: Pie, scatter: Scatter }
const CHART_LABELS = { bar: 'Barres', line: 'Lignes', pie: 'Camembert', scatter: 'Nuage de points' }

export default function ResultChart({ results, chartType: manualType, onChartTypeChange }) {
  const autoType = useMemo(() => detectChartType(results), [results])
  const activeType = manualType || autoType || 'bar'
  const chartData = useMemo(() => buildChartConfig(results, activeType), [results, activeType])

  const ChartComponent = CHART_COMPONENTS[activeType]

  return (
    <div>
      <div className="chart-type-selector">
        {Object.keys(CHART_COMPONENTS).map((type) => (
          <button
            key={type}
            className={activeType === type ? 'active' : ''}
            onClick={() => onChartTypeChange(type)}
          >
            {CHART_LABELS[type]}
          </button>
        ))}
      </div>
      {ChartComponent && chartData ? (
        <div className="chart-container">
          <ChartComponent
            data={chartData}
            options={{
              responsive: true,
              maintainAspectRatio: true,
              plugins: { legend: { position: 'top' } },
            }}
          />
        </div>
      ) : (
        <p className="no-chart">Impossible de construire un graphique {CHART_LABELS[activeType]} avec ces données.</p>
      )}
    </div>
  )
}
