# Color Palette — Repertorio

## Design Intent

Semana Santa is visually defined by deep purple (the liturgical color of Lent and
Penance), gold (the opulence of altar cloths, pasos, and candlelight), and the dark
of Sevillian night processions. The palette translates this into a modern admin
tool: reverent without being cloying, professional without being sterile.

The backgrounds shift between light content areas (day-to-day data work) and dark
hero/header surfaces (the weight and ceremony of Holy Week). Accents of amber,
green, and red map directly to the procesión state machine.

---

## CSS Variable Map

```css
/* ───── Primary — Deep Purple ────────────────────────────────── */
/* Root of the palette. Liturgical purple for vestments and banners.
   Dark enough for buttons and nav bars, vivid enough for the brand. */

--color-primary-50:   #f3e8ff;   /* lightest — hover backgrounds, badges */
--color-primary-100:  #e9d5ff;   /* soft purple — table row hover */
--color-primary-200:  #d8b4fe;   /* muted — borders, dividers */
--color-primary-300:  #c084fc;   /* mid — active tab underline */
--color-primary-400:  #a855f7;   /* vibrant — secondary icons */
--color-primary-500:  #7c3aed;   /* base — primary buttons, links */
--color-primary-600:  #6d28d9;   /* hover — button hover, nav active */
--color-primary-700:  #5b21b6;   /* dark — header backgrounds */
--color-primary-800:  #4c1d95;   /* darker — footer, sidebar base */
--color-primary-900:  #2e1065;   /* darkest — overlay backgrounds */

/* ───── Secondary — Gold Accent ──────────────────────────────── */
/* Gold evokes the splendor of processional trimmings, monstrance
   metals, and the warm glow of candles against dark cloth. Used
   sparingly as an accent — never as a dominant surface color. */

--color-secondary-50:   #fefce8;
--color-secondary-100:  #fef9c3;
--color-secondary-200:  #fef08a;
--color-secondary-300:  #fde047;
--color-secondary-400:  #facc15;   /* base gold — star ratings, highlights */
--color-secondary-500:  #eab308;   /* hover gold — icon hover */
--color-secondary-600:  #ca8a04;   /* dark gold — text on light bg */
--color-secondary-700:  #a16207;
--color-secondary-800:  #854d0e;
--color-secondary-900:  #713f12;

/* ───── Backgrounds ──────────────────────────────────────────── */
/* Light surfaces for content areas (clean data work), dark for
   headers and hero sections (the ceremony of Holy Week). */

--color-bg-base:        #fafafa;       /* page background (near-white) */
--color-bg-surface:     #ffffff;       /* card, dropdown, dialog surfaces */
--color-bg-muted:       #f5f3ff;       /* subtle purple tint for muted sections */
--color-bg-header:      #2e1065;       /* primary-900 — dark hero / header */
--color-bg-sidebar:     #1e1b4b;       /* indigo-950 — sidebar (darker for depth) */
--color-bg-overlay:     rgba(46, 16, 101, 0.4);  /* backdrop for dialogs */

/* ───── Semantic (Procesión Status) ──────────────────────────── */
/* Maps directly to ProcesionStatus enum. These colors appear on
   badges and status indicators across the app. */

--color-status-planned:      #f59e0b;   /* amber-500 — "not yet, future" */
--color-status-in-progress:  #3b82f6;   /* blue-500   — "active, happening now" */
--color-status-completed:    #22c55e;   /* green-500  — "done, fulfilled" */
--color-status-cancelled:    #ef4444;   /* red-500    — "stopped, terminal" */

/* Background tints for status badges (10% opacity equivalent) */

--color-status-planned-bg:      #fef3c7;
--color-status-in-progress-bg:  #dbeafe;
--color-status-completed-bg:    #dcfce7;
--color-status-cancelled-bg:    #fee2e2;

/* ───── Text ─────────────────────────────────────────────────── */
/* High contrast for readability. The dark tones mirror the
   solemnity of the subject matter without sacrificing legibility. */

--color-text-primary:     #18181b;     /* zinc-900 — body text */
--color-text-secondary:   #52525b;     /* zinc-600 — muted text, metadata */
--color-text-muted:       #a1a1aa;     /* zinc-400 — placeholders, disabled */
--color-text-on-primary:  #ffffff;     /* white text on primary buttons */
--color-text-on-dark:     #fafafa;     /* light text on dark backgrounds */
--color-text-inverse:     #ffffff;     /* always-white for dark surfaces */

/* ───── Borders & Dividers ──────────────────────────────────── */

--color-border:          #e4e4e7;     /* zinc-200 — standard borders */
--color-border-light:    #f4f4f5;     /* zinc-100 — subtle dividers */
--color-border-primary:  #7c3aed;     /* primary-500 — focus rings, active borders */

/* ───── Feedback ─────────────────────────────────────────────── */
/* Error, warning, info, success for form validation and alerts. */

--color-error:           #ef4444;
--color-warning:         #f59e0b;
--color-info:            #3b82f6;
--color-success:         #22c55e;
```

---

## Tailwind CSS Integration

In `tailwind.config.ts`, define the custom color palette under `theme.extend.colors`:

```typescript
colors: {
  primary: {
    50:  '#f3e8ff',
    100: '#e9d5ff',
    200: '#d8b4fe',
    300: '#c084fc',
    400: '#a855f7',
    500: '#7c3aed',
    600: '#6d28d9',
    700: '#5b21b6',
    800: '#4c1d95',
    900: '#2e1065',
  },
  secondary: {
    50:  '#fefce8',
    100: '#fef9c3',
    200: '#fef08a',
    300: '#fde047',
    400: '#facc15',
    500: '#eab308',
    600: '#ca8a04',
    700: '#a16207',
    800: '#854d0e',
    900: '#713f12',
  },
  // ... extend with the semantic colors above
}
```

This lets you write classes like `bg-primary-500`, `text-secondary-400`, `bg-status-planned`, etc.

---

## Usage Guidelines

| Element | Color | Why |
|---------|-------|-----|
| Primary buttons | `primary-500` bg, white text | Brand recognition, accessible contrast |
| Secondary buttons | `primary-50` bg, `primary-700` text | Subtle action, less weight |
| Nav header (public) | `bg-header` with `text-on-dark` | Dark top anchors the page |
| Sidebar (admin) | `bg-sidebar` with `text-on-dark` | Deeper than header for visual hierarchy |
| Status badges | `bg-status-*-bg` with `text-status-*` | Tinted bg + saturated text = readable |
| Gold accent | Only on icons, star ratings, celebratory elements | Too much gold = garish |
| Card surfaces | White (`bg-surface`) with subtle shadow | Clean data presentation |
| Active nav link | `primary-100` bg or `primary-500` text underline | Purple glow indicates "you are here" |
| Destructive actions | Red (`error`) — delete buttons, cancel badges | Aligns with danger semantics |
