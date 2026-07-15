# Layout Guide — Repertorio

## Design Intent

Two distinct layout modes: **public** (inviting, centered, spacious) and
**admin** (functional, sidebar-driven, data-dense). Mobile-first at the core:
everything stacks on small screens, reveals horizontal space as viewport grows.

---

## 1. Global Shell

```
┌──────────────────────────────────────────────────────────┐
│  HEADER                                                  │
│  [Logo]           [Nav links]           [Auth status]    │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────────── PAGE CONTENT ───────────────────────┐  │
│  │                                                     │  │
│  │   (public: centered max-w-7xl, padded)             │  │
│  │   (admin: sidebar + content area)                  │  │
│  │                                                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                          │
├──────────────────────────────────────────────────────────┤
│  FOOTER                                                  │
│  © Repertorio · centered                                 │
└──────────────────────────────────────────────────────────┘
```

### Header

| Element | Alignment | Behavior |
|---------|-----------|----------|
| Logo | Left | Links to `/` |
| Nav | Center (public) / hidden (admin) | Public: Home, Hermandades. Admin: sidebar replaces top nav |
| Auth | Right | Login button or user avatar + dropdown |
| Mobile | — | Hamburger icon → Sheet (shadcn `<Sheet>`) on left |

**Header height**: `h-16` (64px) on desktop, `h-14` (56px) on mobile.

**Header variants**:
- **Public pages**: dark background (`bg-header`), light text. Sticky top.
- **Admin pages**: white background (`bg-surface`), standard text. Sticky top with bottom border.

### Footer

- Minimal. Single line: "© Repertorio · Semana Santa Management"
- Background: `bg-primary-900` (dark), text: `text-on-dark` muted
- Height: `py-8` (64px)
- Centered text. No columns, no links (the header has links)
- `mt-auto` to stick to bottom when content is short

---

## 2. Public Layout (Home, Hermandad List, Hermandad Detail)

### Breakpoint Behavior

```
Mobile (< 640px)          Tablet (640-1023px)         Desktop (≥ 1024px)
┌──────────────┐          ┌──────────────────┐        ┌──────────────────────────┐
│   HEADER     │          │     HEADER       │        │        HEADER            │
│ [🍔][logo]   │          │ [🍔][logo] nav   │        │ [logo]    nav    [auth]  │
├──────────────┤          ├──────────────────┤        ├──────────────────────────┤
│              │          │                  │        │                          │
│    HERO      │          │      HERO        │        │         HERO             │
│  (full-w)    │          │   (centered)     │        │    (centered, large)     │
│              │          │                  │        │                          │
│  Search bar  │          │  Search bar      │        │   Search bar + filter    │
│  (full-w)    │          │  (max-w-lg)      │        │   (max-w-xl)             │
│              │          │                  │        │                          │
│ ┌──────────┐ │          │ ┌────┐ ┌────┐    │        │ ┌────┐ ┌────┐ ┌────┐     │
│ │ Card     │ │          │ │Card│ │Card│    │        │ │Card│ │Card│ │Card│     │
│ └──────────┘ │          │ └────┘ └────┘    │        │ └────┘ └────┘ └────┘     │
│ ┌──────────┐ │          │ ┌────┐ ┌────┐    │        │ ┌────┐ ┌────┐ ┌────┐     │
│ │ Card     │ │          │ │Card│ │Card│    │        │ │Card│ │Card│ │Card│     │
│ └──────────┘ │          │ └────┘ └────┘    │        │ └────┘ └────┘ └────┘     │
│ ┌──────────┐ │          │                   │        │                          │
│ │ Card     │ │          │    (2 cols)       │        │       (3 cols)           │
│ └──────────┘ │          │                   │        │                          │
│              │          │                   │        │                          │
│  (1 col)     │          │                   │        │                          │
├──────────────┤          ├──────────────────┤        ├──────────────────────────┤
│   FOOTER     │          │     FOOTER        │        │        FOOTER             │
└──────────────┘          └──────────────────┘        └──────────────────────────┘
```

### Page-Specific Layouts

#### Home Page
1. **Hero section**: Full-width, dark background (`bg-header`). Title "Repertorio"
   in display serif (60px), subtitle "Gestión de Hermandades y Marchas" in body.
   Optional: subtle golden ornament or divider line (`bg-secondary-400`, h-1, w-24).
2. **Featured Hermandades**: Section header "Hermandades destacadas", 3-column card
   grid (1→2→3 responsive). Each card: name, city, founded year, member count.
3. **Quick stats strip** (optional): 3–4 stat cards in a row (total hermandades,
   upcoming processions, marchas in catalog). Icon + number + label.

#### Hermandad List
1. **Search bar**: Full-width on mobile, max-w-lg centered on desktop.
   `<Input>` with search icon, placeholder "Buscar hermandades…".
2. **Card grid**: `grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6`.
   Cards show shield/icon, name, city, year founded, member count as badges.
3. **Empty state**: Centered illustration + text "No se encontraron hermandades".
4. **Loading state**: Skeleton cards (shadcn `<Skeleton>`) matching card layout.

#### Hermandad Detail
1. **Header banner**: Dark purple strip with hermandad name, city, founded year.
   Badge showing member count. Full-width.
2. **Content grid** (below banner, white bg):
   - Left (2/3): Description text, upcoming processions list (small cards)
   - Right (1/3): Stats sidebar (total members by role, foundation year)
3. **Members section** (if admin): Table with role badges, pagination.

---

## 3. Admin Layout (Dashboard, Marcha Catalog, Cruceta Editor)

### Shell

```
┌──────────────────────────────────────────────────────────┐
│  HEADER (white, bordered)                                │
│  [☰ sidebar toggle] [Logo]                  [avatar ▼]  │
├────────┬─────────────────────────────────────────────────┤
│        │                                                  │
│ SIDE   │   CONTENT AREA                                   │
│ BAR    │   (overflow-y-auto, p-6)                        │
│        │                                                  │
│ Nav    │   ┌──────────────────────────────────────────┐  │
│ items  │   │  Page header + breadcrumb (optional)     │  │
│        │   ├──────────────────────────────────────────┤  │
│ Icon   │   │                                          │  │
│ + text │   │  Page content (tabs, tables, forms,      │  │
│        │   │  cards depending on view)                │  │
│ Active │   │                                          │  │
│ state  │   │                                          │  │
│        │   └──────────────────────────────────────────┘  │
│        │                                                  │
│ User   │                                                  │
│ info   │                                                  │
├────────┴─────────────────────────────────────────────────┤
│  FOOTER (minimal)                                         │
└──────────────────────────────────────────────────────────┘
```

### Sidebar

| Feature | Desktop | Mobile |
|---------|---------|--------|
| Width | `w-64` (256px) | Hidden, slides in via `<Sheet>` |
| Background | `bg-sidebar` | Same |
| Nav items | Icon + Text | Icon + Text |
| Active item | `bg-primary-800` highlight | Same |
| Toggle | N/A (always visible) | Hamburger in header |

**Nav items**:
1. Dashboard (overview)
2. Hermandades (J)
3. Procesiones
4. Marchas (music catalog)
5. Crucetas (song lists)

**Bottom section**: User name, role badge, logout link.

### Content Area
- Padding: `p-6` (24px) on desktop, `p-4` on mobile
- Background: `bg-bg-base` (light gray-purple tint)
- Max width: none (admin content uses full available space)
- Sections separated with `space-y-6` or `gap-6` grids

---

## 4. Responsive Patterns

| Pattern | Mobile | Tablet | Desktop |
|---------|--------|--------|---------|
| Card grid | 1 col | 2 cols | 3 cols |
| Detail split | stacked | stacked | 2/3 + 1/3 |
| Form layout | stacked (1 col) | 2 cols for related fields | 2–3 cols |
| Data tables | horizontal scroll or card list | full table | full table |
| Sidebar | Sheet overlay | Sheet overlay | Fixed sidebar |
| Header nav | hidden (hamburger) | visible | visible |
| Hero text | 2rem | 2.5rem | 3.75rem |

---

## 5. Z-Index Stack

| Layer | Z-Index | Element |
|-------|---------|---------|
| Page content | auto | Main content |
| Sticky header | 40 | `<header>` (`z-40`) |
| Sidebar | 40 | Sidebar (matches header) |
| Sheet overlay | 50 | Mobile nav `<Sheet>` |
| Dropdown menus | 50 | `<DropdownMenu>` |
| Dialog / Modal | 60 | `<Dialog>` |
| Toast / Notification | 70 | `<Toast>` / sonner |

---

## 6. Spacing System

Use Tailwind's default spacing scale. Key values:

| Token | rem | px | Usage |
|-------|-----|----|-------|
| `p-4` | 1rem | 16px | Card padding (mobile) |
| `p-6` | 1.5rem | 24px | Card padding (desktop), section padding |
| `gap-4` | 1rem | 16px | Grid gap (mobile) |
| `gap-6` | 1.5rem | 24px | Grid gap (desktop) |
| `space-y-4` | 1rem | 16px | Vertical stack spacing |
| `space-y-6` | 1.5rem | 24px | Section spacing |
| `max-w-7xl` | 80rem | 1280px | Public page content width |
| `mx-auto` | — | — | Center public content |
