/**
 * Détecte le type de graphique approprié en analysant les colonnes et les données.
 * Retourne : 'bar', 'line', 'pie', 'scatter', ou null (pas de détection auto).
 */
export function detectChartType(results) {
  if (!results || results.length === 0) return null

  const columns = Object.keys(results[0])
  if (columns.length < 1) return null

  const colTypes = columns.map((col) => ({
    name: col,
    type: inferColumnType(results, col),
  }))

  const stringCols = colTypes.filter((c) => c.type === 'string')
  const numericCols = colTypes.filter((c) => c.type === 'numeric')
  const dateCols = colTypes.filter((c) => c.type === 'date')

  // 1 col date + N cols numériques → line chart
  if (dateCols.length === 1 && numericCols.length >= 1) return 'line'

  // 1 col string + 1 col numérique + ≤10 lignes → pie chart
  if (stringCols.length === 1 && numericCols.length === 1 && results.length <= 10) return 'pie'

  // 1 col string + 1+ col numérique → bar chart
  if (stringCols.length >= 1 && numericCols.length >= 1) return 'bar'

  // 2 cols numériques → scatter plot
  if (numericCols.length === 2 && stringCols.length === 0 && dateCols.length === 0) return 'scatter'

  // Uniquement des strings → bar chart par comptage de fréquence sur la 1ère colonne
  if (stringCols.length >= 1 && numericCols.length === 0) return 'bar'

  return null
}

/**
 * Construit la configuration Chart.js à partir des résultats et du type détecté.
 */
export function buildChartConfig(results, chartType) {
  if (!chartType || !results || results.length === 0) return null

  const columns = Object.keys(results[0])
  const colTypes = columns.map((col) => ({
    name: col,
    type: inferColumnType(results, col),
  }))

  const stringCols = colTypes.filter((c) => c.type === 'string')
  const numericCols = colTypes.filter((c) => c.type === 'numeric')
  const dateCols = colTypes.filter((c) => c.type === 'date')

  const COLORS = [
    '#4361ee', '#3a0ca3', '#7209b7', '#f72585', '#4cc9f0',
    '#06d6a0', '#ffd166', '#ef476f', '#118ab2', '#073b4c',
  ]

  if (chartType === 'bar' || chartType === 'pie') {
    // Cas 1 : on a une colonne string + une colonne numérique → classique
    if (stringCols.length >= 1 && numericCols.length >= 1) {
      const labelCol = stringCols[0].name
      const labels = results.map((r) => String(r[labelCol]))
      const datasets = numericCols.map((col, i) => ({
        label: col.name,
        data: results.map((r) => Number(r[col.name]) || 0),
        backgroundColor: chartType === 'pie' ? labels.map((_, j) => COLORS[j % COLORS.length]) : COLORS[i % COLORS.length],
        borderColor: chartType === 'pie' ? '#fff' : COLORS[i % COLORS.length],
        borderWidth: chartType === 'pie' ? 2 : 1,
      }))
      return { labels, datasets }
    }

    // Cas 2 : que des strings → comptage de fréquence sur la 1ère colonne
    if (stringCols.length >= 1) {
      const labelCol = stringCols[0].name
      const freq = {}
      for (const row of results) {
        const val = String(row[labelCol] ?? '')
        freq[val] = (freq[val] || 0) + 1
      }
      const entries = Object.entries(freq).sort((a, b) => b[1] - a[1])
      const labels = entries.map(([k]) => k)
      const data = entries.map(([, v]) => v)
      return {
        labels,
        datasets: [{
          label: `Nombre par ${labelCol}`,
          data,
          backgroundColor: chartType === 'pie' ? labels.map((_, j) => COLORS[j % COLORS.length]) : COLORS[0],
          borderColor: chartType === 'pie' ? '#fff' : COLORS[0],
          borderWidth: chartType === 'pie' ? 2 : 1,
        }],
      }
    }

    return null
  }

  if (chartType === 'line') {
    // Axe X = date ou première colonne string
    const xCol = dateCols.length > 0 ? dateCols[0] : stringCols[0]
    if (!xCol) return null
    const labels = results.map((r) => String(r[xCol.name]))

    if (numericCols.length >= 1) {
      const datasets = numericCols.map((col, i) => ({
        label: col.name,
        data: results.map((r) => Number(r[col.name]) || 0),
        borderColor: COLORS[i % COLORS.length],
        backgroundColor: COLORS[i % COLORS.length] + '33',
        fill: false,
        tension: 0.3,
      }))
      return { labels, datasets }
    }

    // Pas de numérique → comptage cumulatif
    const cumulative = labels.map((_, i) => i + 1)
    return {
      labels,
      datasets: [{
        label: 'Nombre cumulé',
        data: cumulative,
        borderColor: COLORS[0],
        backgroundColor: COLORS[0] + '33',
        fill: false,
        tension: 0.3,
      }],
    }
  }

  if (chartType === 'scatter') {
    if (numericCols.length >= 2) {
      const xCol = numericCols[0].name
      const yCol = numericCols[1].name
      return {
        datasets: [{
          label: `${xCol} vs ${yCol}`,
          data: results.map((r) => ({ x: Number(r[xCol]) || 0, y: Number(r[yCol]) || 0 })),
          backgroundColor: COLORS[0],
        }],
      }
    }
    return null
  }

  return null
}

function inferColumnType(results, col) {
  const sample = results.slice(0, 20).map((r) => r[col]).filter((v) => v != null && v !== '')

  if (sample.length === 0) return 'string'

  const allNumeric = sample.every((v) => !isNaN(Number(v)))
  if (allNumeric) return 'numeric'

  const datePattern = /^\d{4}-\d{2}-\d{2}/
  const allDates = sample.every((v) => datePattern.test(String(v)))
  if (allDates) return 'date'

  return 'string'
}
