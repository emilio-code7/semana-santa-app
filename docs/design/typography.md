# Typography — Repertorio

## Design Intent

The app lives between two poles: public-facing pages that should feel
ceremonial and warm, and admin pages that demand clarity and scan-ability.
The type system serves both with one refined sans-serif for body (Geist or Inter)
and a decorative display face reserved for the hero section and major headings.

---

## Font Stack

### 1. Body Text: Geist (primary) or Inter (fallback)

```css
/* Geist — Vercel's typeface, modern and compact */
/* Inter — Google Fonts, wider adoption, excellent legibility */

--font-sans: 'Geist', 'Inter', -apple-system, BlinkMacSystemFont,
             'Segoe UI', Roboto, sans-serif;
```

**Why Geist**:
- Narrower letter-spacing saves horizontal space on data-heavy admin views
- Clean geometric shapes match the "modern admin tool" goal
- Available as variable font (one file for all weights)
- Pair with Inter as installed-system-font fallback

**Why Inter as fallback**: Zero cost, zero friction, high legibility at small
sizes for table data and dense forms.

### 2. Display / Headings (optional accent): unna or Cormorant Garamond

```css
/* Optional — a serif for hero headings only */
--font-display: 'Cormorant Garamond', 'Georgia', serif;
```

**Use only on**:
- Home page hero title ("Repertorio")
- Large section headers on public pages (max h2)

**Reason**: A light serif evokes the historical weight of Semana Santa traditions
without screaming "religious." Use it at 3xl+ sizes only; everything else uses
the sans stack.

### 3. Mono: JetBrains Mono

```css
--font-mono: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace;
```

**Use for**:
- Duration display (`04:32` for marcha length)
- Technical metadata (UUIDs, timestamps in tables)
- Code snippets if any (none expected in UI)

---

## Type Scale

Based on a 1.25 ratio (musical fourth) for clean hierarchy:

| Level | Size (rem) | Size (px) | Weight | Line-Height | Letter-Spacing | Usage |
|-------|-----------|----------|--------|-------------|----------------|-------|
| **h1** | 2.5rem | 40px | 700 | 1.1 | -0.02em | Page title (public hero) |
| **h2** | 2.0rem | 32px | 600 | 1.2 | -0.01em | Section header |
| **h3** | 1.5rem | 24px | 600 | 1.3 | 0 | Card title, modal header |
| **h4** | 1.25rem | 20px | 600 | 1.4 | 0 | Subsection, table caption |
| **body** | 0.938rem | 15px | 400 | 1.6 | 0 | Paragraph text |
| **body-sm** | 0.875rem | 14px | 400 | 1.5 | 0 | Table cells, form labels |
| **small** | 0.75rem | 12px | 400 | 1.4 | 0 | Metadata, badges |
| **tiny** | 0.688rem | 11px | 500 | 1.3 | 0.02em | Overline, stat labels |

**Display (serif, hero only)** :

| Level | Size | Weight | Line-Height |
|-------|------|--------|-------------|
| hero-display | 3.75rem (60px) | 500 (light) | 1.05 |

---

## Tailwind Configuration

```typescript
// tailwind.config.ts
fontFamily: {
  sans: ['Geist', 'Inter', ...defaultTheme.fontFamily.sans],
  display: ['Cormorant Garamond', 'Georgia', 'serif'],
  mono: ['JetBrains Mono', 'Fira Code', ...defaultTheme.fontFamily.mono],
},
fontSize: {
  'tiny':  ['0.688rem', { lineHeight: '1.3', letterSpacing: '0.02em', fontWeight: '500' }],
  'body-sm': ['0.875rem', { lineHeight: '1.5' }],
  // h1-h4 use the default Tailwind scale (text-4xl → text-xl)
}
```

---

## Usage Rules

1. **Never use the display serif on admin pages** — it undermines the professional,
   data-oriented tone. Reserve it for public-facing hero and section headers.

2. **Body text on dark backgrounds** (header, sidebar) uses `font-light` (300) weight
   and `text-on-dark` color. Light-on-dark text reads better with slightly thinner strokes.

3. **Table cells** are `body-sm` (14px). No line-height tricks — keep rows compact.

4. **Line-length** on public pages: cap paragraphs at ~70ch for readability.
   Admin content can flow full-width (data density wins).

5. **No underlines on headings** — ever. Use weight and size for hierarchy, not decoration.

6. **Font loading strategy**:
   - Geist: self-host from `node_modules/geist/dist/fonts/` (no external requests)
   - Inter: Google Fonts with `display=swap` and `preconnect` hint
   - Cormorant Garamond: Google Fonts, `display=swap`, weight 500 only
   - JetBrains Mono: Google Fonts, `display=swap`, weight 400 only
