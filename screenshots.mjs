/**
 * Script de capture d'écran de l'interface NL-SQL Engine.
 *
 * Prérequis :
 *   - Le frontend doit tourner : npm run dev (port 5173)
 *   - Le backend doit tourner : ./mvnw quarkus:dev (port 8080)
 *   - Chromium Playwright installé : npx playwright install chromium
 *
 * Usage :
 *   node screenshots.mjs
 *
 * Les captures sont enregistrées dans ./images/
 */

import { chromium } from 'playwright'
import { mkdirSync } from 'fs'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const IMAGES_DIR = resolve(__dirname, 'images')
const BASE_URL = process.env.FRONT_URL || 'http://localhost:5173'
const VIEWPORT = { width: 1280, height: 900 }

mkdirSync(IMAGES_DIR, { recursive: true })

async function screenshot(page, name, opts = {}) {
  const path = resolve(IMAGES_DIR, `${name}.png`)
  await page.screenshot({ path, fullPage: opts.fullPage ?? false })
  console.log(`  ✓ ${name}.png`)
}

async function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function main() {
  console.log('Lancement des captures d\'écran...\n')

  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: VIEWPORT })
  const page = await context.newPage()

  // ── 1. Page d'accueil vide ──
  console.log('[1/7] Page d\'accueil')
  await page.goto(BASE_URL)
  await page.waitForLoadState('networkidle')
  await sleep(500)
  await screenshot(page, '01-accueil')

  // ── 2. Saisie d'une question ──
  console.log('[2/7] Saisie d\'une question')
  const textarea = page.locator('.query-input')
  await textarea.fill('Quels sont les personnages originaires de Tatooine ?')
  await sleep(300)
  await screenshot(page, '02-saisie-question')

  // ── 3. Résultats (réponse + SQL + tableau) ──
  console.log('[3/7] Résultats')
  await page.locator('.submit-btn').click()
  // Attendre que le loading disparaisse et les résultats apparaissent
  try {
    await page.locator('.results').waitFor({ state: 'visible', timeout: 120000 })
    await sleep(500)
  } catch {
    console.log('  ⚠ Timeout en attente des résultats — capture en l\'état')
  }
  await screenshot(page, '03-resultats', { fullPage: true })

  // ── 4. Vue graphique ──
  console.log('[4/7] Vue graphique')
  const chartBtn = page.locator('.view-toggle button', { hasText: 'Graphique' })
  if (await chartBtn.isVisible()) {
    await chartBtn.click()
    await sleep(800)
    await screenshot(page, '04-graphique', { fullPage: true })
  } else {
    console.log('  ⚠ Bouton graphique non visible — skip')
  }

  // ── 5. Formulaire d'enregistrement de prompt ──
  console.log('[5/7] Enregistrer un prompt')
  const saveBtn = page.locator('.save-btn')
  if (await saveBtn.isVisible()) {
    await saveBtn.click()
    await sleep(300)
    await screenshot(page, '05-enregistrer-prompt', { fullPage: true })
    // Fermer le formulaire
    const cancelBtn = page.locator('.cancel-btn')
    if (await cancelBtn.isVisible()) await cancelBtn.click()
  } else {
    console.log('  ⚠ Bouton enregistrer non visible — skip')
  }

  // ── 6. Bibliothèque de prompts ──
  console.log('[6/7] Bibliothèque de prompts')
  const libBtn = page.locator('.library-toggle')
  if (await libBtn.isVisible()) {
    await libBtn.click()
    await page.locator('.prompt-library').waitFor({ state: 'visible', timeout: 5000 }).catch(() => {})
    await sleep(500)
    await screenshot(page, '06-bibliotheque')
    // Fermer la modal
    const closeBtn = page.locator('.close-btn')
    if (await closeBtn.isVisible()) await closeBtn.click()
    await sleep(300)
  } else {
    console.log('  ⚠ Bouton bibliothèque non visible — skip')
  }

  // ── 7. Sélecteur de domaine ──
  console.log('[7/7] Sélecteur de domaine')
  const domainSelector = page.locator('.domain-selector')
  if (await domainSelector.isVisible()) {
    await screenshot(page, '07-domaine-selector')
  } else {
    console.log('  ⚠ Sélecteur de domaine non visible (aucun domaine en base) — skip')
  }

  await browser.close()
  console.log(`\nTerminé — captures dans ${IMAGES_DIR}/`)
}

main().catch((err) => {
  console.error('Erreur:', err.message)
  process.exit(1)
})
