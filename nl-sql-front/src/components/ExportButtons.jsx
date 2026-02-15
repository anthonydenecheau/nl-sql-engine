export default function ExportButtons({ results }) {
  if (!results || results.length === 0) return null

  const exportCsv = () => {
    const columns = Object.keys(results[0])
    const header = columns.join(',')
    const rows = results.map((row) =>
      columns.map((col) => {
        const val = row[col] != null ? String(row[col]) : ''
        return val.includes(',') || val.includes('"') || val.includes('\n')
          ? '"' + val.replace(/"/g, '""') + '"'
          : val
      }).join(',')
    )
    download(header + '\n' + rows.join('\n'), 'resultats.csv', 'text/csv')
  }

  const exportJson = () => {
    download(JSON.stringify(results, null, 2), 'resultats.json', 'application/json')
  }

  const download = (content, filename, mime) => {
    const blob = new Blob([content], { type: mime })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="export-buttons">
      <button className="export-btn" onClick={exportCsv}>Export CSV</button>
      <button className="export-btn" onClick={exportJson}>Export JSON</button>
    </div>
  )
}
