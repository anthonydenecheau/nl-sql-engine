import { useState, useMemo } from 'react'

const PAGE_SIZE = 20

export default function ResultTable({ results }) {
  const [page, setPage] = useState(0)
  const [sortCol, setSortCol] = useState(null)
  const [sortAsc, setSortAsc] = useState(true)

  const columns = useMemo(() => (results.length > 0 ? Object.keys(results[0]) : []), [results])

  const sorted = useMemo(() => {
    if (!sortCol) return results
    return [...results].sort((a, b) => {
      const va = a[sortCol], vb = b[sortCol]
      if (va == null) return 1
      if (vb == null) return -1
      if (!isNaN(Number(va)) && !isNaN(Number(vb))) {
        return sortAsc ? Number(va) - Number(vb) : Number(vb) - Number(va)
      }
      return sortAsc ? String(va).localeCompare(String(vb)) : String(vb).localeCompare(String(va))
    })
  }, [results, sortCol, sortAsc])

  const totalPages = Math.ceil(sorted.length / PAGE_SIZE)
  const paged = sorted.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const handleSort = (col) => {
    if (sortCol === col) {
      setSortAsc(!sortAsc)
    } else {
      setSortCol(col)
      setSortAsc(true)
    }
    setPage(0)
  }

  return (
    <div>
      <div className="table-wrapper">
        <table className="results-table">
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={col} onClick={() => handleSort(col)} style={{ cursor: 'pointer' }}>
                  {col} {sortCol === col ? (sortAsc ? '▲' : '▼') : ''}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {paged.map((row, i) => (
              <tr key={i}>
                {columns.map((col) => (
                  <td key={col}>{row[col] != null ? String(row[col]) : ''}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>← Précédent</button>
          <span>{page + 1} / {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>Suivant →</button>
        </div>
      )}
    </div>
  )
}
