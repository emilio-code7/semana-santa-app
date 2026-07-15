# Component Map — Repertorio

## Design Intent

Maps each UI view to its shadcn/ui components, layout structure, and key states.
A backend developer implementing these pages can use this as a checklist.

All imports from: `@/components/ui/<component>` (shadcn convention via `npx shadcn@latest add`).

---

## 1. Home Page (public) — `/`

```
┌─────────────────────────────────────────────────────┐
│  HERO                                                │
│  ┌─────────────────────────────────────────────────┐│
│  │  [Logo icon] Repertorio                         ││
│  │  Gestión de Hermandades y Marchas de Semana     ││
│  │  Santa                                          ││
│  │                                                 ││
│  │  [Ver Hermandades] [Iniciar Sesión]             ││
│  └─────────────────────────────────────────────────┘│
│                                                      │
│  FEATURED HERMANDADES (section)                      │
│  ┌──────┐ ┌──────┐ ┌──────┐                         │
│  │ Card │ │ Card │ │ Card │                         │
│  └──────┘ └──────┘ └──────┘                         │
│                                                      │
│  STATS STRIP                                         │
│  ┌─────┐ ┌─────┐ ┌─────┐                            │
│  │Stat │ │Stat │ │Stat │                            │
│  └─────┘ └─────┘ └─────┘                            │
└─────────────────────────────────────────────────────┘
```

### Components

| Element | shadcn Component | Props / Notes |
|---------|-----------------|---------------|
| Page wrapper | `<main>` container | `max-w-7xl mx-auto px-4` |
| Hero section | Custom section | Dark bg (`bg-header`), padding `py-20`, centered text |
| Hero title | `<h1>` geist or display serif | `text-4xl md:text-6xl font-bold` |
| Hero subtitle | `<p>` | `text-lg text-zinc-300` |
| CTA buttons | `<Button>` | Primary: `bg-primary-500`, secondary: `variant="outline"` |
| Featured grid | `<div className="grid ...">` | `grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6` |
| Hermandad card | `<Card>` + `<CardHeader>` + `<CardContent>` | Header: name. Content: city, founded year, member badge |
| Card footer | `<CardFooter>` | Optional: "Ver detalle" link |
| Stats strip | `<div className="flex ...">` | 3-4 stat items with icon + number + label |
| Stat item | Custom (or `<Card>` compact) | Icon, large number (h3), tiny label (text-muted) |
| Loading | `<Skeleton>` | Match card shape: h-48 w-full |
| Empty featured | `<p>` centered | "No hay hermandades destacadas" |
| Mobile nav toggle | `<Sheet>` + `<SheetTrigger>` + `<SheetContent>` | Hamburger icon, side="left" |

### States
- **Loading**: 6 skeleton cards in grid
- **Empty**: "No hay hermandades registradas. Sé el primero en crear una."
- **Error**: `<Alert variant="destructive">` — "Error al cargar hermandades"
- **Authenticated**: Header shows user avatar + name instead of login button

---

## 2. Hermandad List (public) — `/hermandades`

```
┌─────────────────────────────────────────────────────┐
│  HEADER: Hermandades                                 │
│                                                      │
│  SEARCH                                              │
│  ┌─────────────────────────────────────────────────┐│
│  │ 🔍 Buscar hermandades...                       ││
│  └─────────────────────────────────────────────────┘│
│                                                      │
│  ┌──────┐ ┌──────┐ ┌──────┐                         │
│  │ Card │ │ Card │ │ Card │                         │
│  │ name │ │ name │ │ name │                         │
│  │ city │ │ city │ │ city │                         │
│  │ year │ │ year │ │ year │                         │
│  │ [n]  │ │ [n]  │ │ [n]  │                         │
│  └──────┘ └──────┘ └──────┘                         │
│                                                      │
│  PAGINATION (optional)                               │
│  [<] [1] [2] [3] [>]                                 │
└─────────────────────────────────────────────────────┘
```

### Components

| Element | shadcn Component | Props / Notes |
|---------|-----------------|---------------|
| Page header | `<h1>` or custom section | `Hermandades` title, optional count badge |
| Search input | `<Input>` | `type="search"`, `placeholder="Buscar..."`, debounced onChange |
| Search icon | `<Search>` from lucide-react | Inside input via `leading` or `prefix` |
| Filter badge group | `<Badge variant="outline">` | Optional: city filters, clickable |
| Card grid | `<div className="grid ...">` | Same as home page: `grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6` |
| Hermandad card | `<Card>` + `<CardHeader>` + `<CardContent>` | Clickable (entire card links to `/hermandades/[id]`) |
| Member count | `<Badge>` | `variant="secondary"` shows member count |
| City + Year | `<p className="text-sm text-muted-foreground">` | Muted metadata |
| Pagination | `<Pagination>` (from shadcn) | Optional if API returns paginated results |
| Skeleton | `<Skeleton>` | Rectangle matching card proportions |
| Empty results | Custom section | Illustration + "No se encontraron hermandades" |
| No search results | `<p>` | "Ninguna hermandad coincide con tu búsqueda" |

### States
- **Loading**: 6 skeleton cards, search bar disabled with opacity
- **Empty (no data)**: "Ninguna hermandad registrada aún."
- **Empty (search)**: "No hay resultados para «{query}»." — with "Limpiar filtros" link
- **Error**: Inline `<Alert variant="destructive">` above grid

---

## 3. Hermandad Detail (public) — `/hermandades/[id]`

```
┌─────────────────────────────────────────────────────┐
│  BANNER (dark bg)                                    │
│  ┌─────────────────────────────────────────────────┐│
│  │  Hermandad del Gran Poder                       ││
│  │  📍 Sevilla · Fundada 1545                      ││
│  │  [👥 24 miembros] [📅 3 procesiones]            ││
│  └─────────────────────────────────────────────────┘│
│                                                      │
│  CONTENT (2/3 + 1/3)                                 │
│  ┌──────────────────┐ ┌────────────┐                 │
│  │ DESCRIPTION       │ │ STATS      │                 │
│  │ Lorem ipsum...    │ │ ● CAPATAZ:2│                 │
│  │                   │ │ ● ADMIN:5  │                 │
│  │ PROCESIONES       │ │ ● DIREC:3  │                 │
│  │ ┌─Procesion card─┐│ │ ● MUSIC:14│                 │
│  │ │ Viernes Santo  ││ └────────────┘                 │
│  │ │ 15:00 · PLANNED││                                │
│  │ └────────────────┘│                                │
│  └──────────────────┘                                 │
└─────────────────────────────────────────────────────┘
```

### Components

| Element | shadcn Component | Props / Notes |
|---------|-----------------|---------------|
| Banner | Custom section | `bg-header`, padding `py-12`, flex column |
| Hermandad name | `<h1>` | `text-3xl font-bold text-white` |
| Location + year | `<p>` | `text-zinc-300` with `📍` icon or `<MapPin>` icon |
| Stats badges | `<Badge>` | `variant="outline"` on dark bg (inverted) |
| Content grid | `<div className="grid ...">` | `grid-cols-1 lg:grid-cols-3 gap-8` |
| Description | `<Card>` | Rich text or plain paragraph in `<CardContent>` |
| Procesiones list | `<div className="space-y-4">` | List of upcoming processions |
| Procesion card | `<Card>` (compact) | Date, time, status badge, link to more |
| Status badge | `<Badge>` | Variant maps to status: `planned`/`in-progress`/`completed`/`destructive` |
| Stats sidebar | `<Card>` | Member role counts inside `<CardContent>` |
| Role count list | `<ul>` with `<li>` items | Each: role name + count + mini icon |
| Member list (admin) | `<Table>` | Only rendered if user has admin role |
| Table caption | `<TableCaption>` | "Miembros de la hermandad" |
| Table row | `<TableRow>` | Name, role badge, joined date |
| Back button | `<Button variant="ghost">` | "← Volver" above banner |
| Edit button (admin) | `<Button>` | "Editar" in top-right of banner |

### States
- **Loading**: Banner skeleton + content skeleton (2-column)
- **Not found**: `<Alert variant="destructive">` — "Hermandad no encontrada"
- **No upcoming processions**: "No hay procesiones programadas" text
- **Not admin**: Member table not rendered; sidebar stats only

---

## 4. Admin Dashboard (tabs) — `/admin/hermandades/[id]`

```
┌─────────────────────────────────────────────────────┐
│  SIDEBAR (always visible desktop, Sheet mobile)      │
│  [Dashboard] [Hermandades] [Procesiones]             │
│  [Marchas] [Crucetas]                                │
├─────────────────────────────────────────────────────┤
│  CONTENT                                              │
│  ┌─────────────────────────────────────────────────┐│
│  │  Hermandad del Gran Poder — Administración      ││
│  │                                                 ││
│  │  [Vista General] [Miembros] [Procesiones] [     ││
│  │   Cruceta]                                       ││
│  │  ┌─────────────────────────────────────────────┐││
│  │  │  TAB CONTENT                                │││
│  │  │                                             │││
│  │  │  (depends on active tab)                    │││
│  │  └─────────────────────────────────────────────┘││
│  └─────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

### Components

| Element | shadcn Component | Props / Notes |
|---------|-----------------|---------------|
| Sidebar nav | `<nav>` with `<Button variant="ghost">` items | Active: `bg-primary-100 text-primary-700` |
| Mobile sidebar | `<Sheet>` + `<SheetContent side="left">` | Contains same nav items |
| Page header | `<div className="flex justify-between">` | Title + "Volver" link |
| Tabs | `<Tabs>` + `<TabsList>` + `<TabsTrigger>` + `<TabsContent>` | 4 tabs: overview, members, processions, cruceta |
| Tab triggers | `<TabsTrigger>` | Icons + text labels |

#### Tab: Vista General (Overview)

| Element | shadcn Component | Notes |
|---------|-----------------|-------|
| Stat cards row | `<div className="grid grid-cols-2 md:grid-cols-4 gap-4">` | 4 stat cards |
| Stat card | `<Card>` | Large number + label (e.g., "24 miembros") |
| Upcoming procesiones | `<Card>` with list | Compact list of next 5 processions |
| Recent activity | `<Card>` | Empty state for now (no audit log yet) |

#### Tab: Miembros (Members)

| Element | shadcn Component | Notes |
|---------|-----------------|-------|
| Add button | `<Button>` | "Añadir miembro" → opens `<Dialog>` |
| Members table | `<Table>` + `<TableHeader>` + `<TableBody>` + `<TableRow>` + `<TableCell>` | |
| Role badge | `<Badge>` | Each role has distinct color |
| Actions | `<DropdownMenu>` | Edit role, Remove member |
| Add dialog | `<Dialog>` + `<DialogHeader>` + `<DialogContent>` | Form with `<Input>` + `<Select>` for role |
| Select role | `<Select>` + `<SelectTrigger>` + `<SelectContent>` + `<SelectItem>` | Options from HermandadRole enum |
| Confirm delete | `<AlertDialog>` | "¿Eliminar miembro?" confirm/cancel |
| Empty | `<Table>` with `<TableCaption>` | "No hay miembros" |

#### Tab: Procesiones (Processions)

| Element | shadcn Component | Notes |
|---------|-----------------|-------|
| Add button | `<Button>` | "Nueva procesión" → `<Dialog>` |
| Procesiones table | `<Table>` | Columns: date, time, status, actions |
| Status badge | `<Badge>` | Mapped to variant per status |
| Status change | `<Select>` inside table row | Inline edit of status |
| Date picker | Native `<input type="date">` | No date picker library needed |
| Empty | `<Table>` with caption | "No hay procesiones" |

#### Tab: Cruceta (Song List Editor)

| Element | shadcn Component | Notes |
|---------|-----------------|-------|
| Selected procesión | `<Select>` | Dropdown to pick which procesión to edit |
| Song list | `<div className="space-y-2">` | Ordered list of cruceta items |
| Cruise item card | `<Card>` (compact, horizontal) | Order number, title, composer, duration |
| Drag handle | `<GripVertical>` icon | Visual indicator for ordering |
| Add song | `<Button>` | "Añadir marcha" → `<Dialog>` with search |
| Song search | `<Input>` inside dialog | Filters available marchas |
| Song search results | `<ScrollArea>` | List of clickable marchas |
| Remove song | `<Button variant="ghost" size="icon">` | Trash icon, confirmation with `<AlertDialog>` |
| Save button | `<Button>` | "Guardar cruceta" |
| Duration total | `<p>` bottom of list | Sum of all song durations |

### States
- **Loading tab**: `<Skeleton>` matching tab content shape
- **Empty tab**: Tab-specific empty state messages
- **Unsaved changes**: `<Badge variant="outline">` "Sin guardar" — appears after modification

---

## 5. Marcha Catalog (admin) — `/admin/marchas`

```
┌─────────────────────────────────────────────────────┐
│  HEADER: Catálogo de Marchas                         │
│  [🔍 Buscar...]                     [+ Añadir]      │
│                                                      │
│  FILTER BADGES                                       │
│  [Todas] [Banda de Palio] [Agrupación] [Cornetas]   │
│                                                      │
│  ┌─────────────────────────────────────────────────┐│
│  │  GRID / TABLE                                    ││
│  │                                                  ││
│  │  Title  │ Composer   │ Band Type   │ Duration    │
│  │  ───────┼────────────┼─────────────┼───────────  ││
│  │  ...    │ ...        │ ...         │ 04:32       ││
│  │  ...    │ ...        │ ...         │ 03:15       ││
│  └─────────────────────────────────────────────────┘│
│                                                      │
│  PAGINATION                                          │
└─────────────────────────────────────────────────────┘
```

### Components

| Element | shadcn Component | Notes |
|---------|-----------------|-------|
| Page header | `<div className="flex justify-between">` | Title + add button |
| Search | `<Input>` with search icon | Debounced |
| Add button | `<Button>` | → `<Dialog>` with create form |
| Filter group | `<ToggleGroup>` or badge buttons | Filter by `bandType` |
| Filter badge | `<Badge variant={active ? "default" : "outline"}>` | Clickable filter toggles |
| Marcha table | `<Table>` | Columns: title, composer, band type, duration, actions |
| Table row | `<TableRow>` | With `<TableCell>` per column |
| Band type badge | `<Badge>` | `variant="secondary"` |
| Duration | `<code>` (mono font) | Formatted as `mm:ss`, `font-mono` |
| Actions | `<DropdownMenu>` | Edit, Delete |
| Delete confirm | `<AlertDialog>` | "¿Eliminar marcha?" |
| Add/Edit dialog | `<Dialog>` + form | Fields: title, composer, band type (`<Select>`), duration, year, youtube URL |
| Duration input | `<Input type="text">` | Masked or `mm:ss` format |
| YouTube URL | `<Input type="url">` | Optional field |
| Empty | `<Table>` with `<TableCaption>` | "No hay marchas. Añade la primera." |
| Search empty | `<div>` centered | "No se encontraron marchas para «{query}»" |

### States
- **Loading**: `<Skeleton>` rows in table
- **Filtered**: Active filter badge is `variant="default"` (purple), others `outline`
- **Search active**: Results update on keystroke (debounce 300ms)

---

## 6. Cruceta Editor (admin, within dashboard)

Accessible via: Admin Dashboard → Cruceta tab (for a specific hermandad/procesión).

```
┌─────────────────────────────────────────────────────────────┐
│  Encabezado: Cruceta — Hermandad del Gran Poder             │
│  Procesión: [Viernes Santo 18:00 ▼]          [Guardar]     │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  #  │ Marcha                  │ Duración   │             ││
│  │  ───┼─────────────────────────┼────────────┤             ││
│  │  1  │ ⠿ Saeta  ⠿             │ 04:32      │  [🗑]      ││
│  │  2  │ ⠿ Amarguras  ⠿         │ 03:15      │  [🗑]      ││
│  │  3  │ ⠿ Virgen de la Macarena│ 05:00      │  [🗑]      ││
│  │  ───┼─────────────────────────┼────────────┤             ││
│  │     │ Total                   │ 12:47      │             ││
│  │                                                          ││
│  │  [+ Añadir marcha]                                       ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### Components

| Element | shadcn Component | Notes |
|---------|-----------------|-------|
| Page header | `<div>` | Title + breadcrumb |
| Procesión selector | `<Select>` | Dropdown to pick procesión for this hermandad |
| Save button | `<Button>` | "Guardar cruceta" |
| Preview link (optional) | `<Button variant="link">` | "Vista previa" |
| Item list | `<div className="space-y-2">` | Ordered list of cruceta items |
| Item row | `<Card>` (compact, horizontal flex) | Order, info, actions |
| Drag handle | `<Button variant="ghost" size="icon">` | `<GripVertical>` icon (visual only for v1, actual drag in v2) |
| Order number | `<Badge variant="outline">` | Circular or small badge |
| Song info | `<div>` | Title (bold) + composer (muted) |
| Duration | `<code className="font-mono">` | `mm:ss` format |
| Remove button | `<Button variant="ghost" size="icon">` | Trash icon, `text-destructive` on hover |
| Total duration | `<div className="border-t pt-2">` | Sum of all durations |
| Add button | `<Button variant="outline">` | "Añadir marcha" → opens `<Sheet>` or `<Dialog>` |
| Add panel | `<Sheet>` or `<Dialog>` | Contains search + results |
| Song search | `<Input>` with search icon | Filter available marchas |
| Search results | `<ScrollArea className="h-72">` | Scrollable list of marchas |
| Result item | `<div>` clickable | Title + composer + duration, `hover:bg-muted cursor-pointer` |
| No results | `<p>` | "No se encontraron marchas" |
| Empty cruceta | `<div>` centered | "La cruceta está vacía. Añade marchas para empezar." |

### States
- **No procesión selected**: `<Select>` shows placeholder "Selecciona una procesión"
- **Procesión selected, no cruceta**: Empty state message with add button
- **Procesión selected, cruceta exists**: Ordered list with drag handles
- **Unsaved changes**: Save button becomes `bg-primary-500` (was muted). Optional unsaved badge
- **Saved**: Toast notification "Cruceta guardada correctamente"
- **Loading**: Skeleton rows (3 items)
- **Error**: `<Alert variant="destructive">` — "Error al guardar la cruceta"

---

## Shared Components (used across views)

| Component | Usage | Notes |
|-----------|-------|-------|
| `<Button>` | All actions | Variants: `default`, `secondary`, `outline`, `ghost`, `destructive`, `link` |
| `<Badge>` | Status labels, role labels, counts | Variants map to semantic meaning |
| `<Card>` | Grouped content blocks | `<CardHeader>`, `<CardContent>`, `<CardFooter>` |
| `<Table>` | Tabular data | `<TableHeader>`, `<TableBody>`, `<TableRow>`, `<TableCell>` |
| `<Tabs>` | Admin dashboard tabs | `<TabsList>`, `<TabsTrigger>`, `<TabsContent>` |
| `<Dialog>` | Add/Edit forms | `<DialogTrigger>`, `<DialogContent>`, `<DialogHeader>`, `<DialogFooter>` |
| `<Sheet>` | Mobile nav, add-song panel | `<SheetTrigger>`, `<SheetContent>`, `<SheetHeader>` |
| `<Select>` | Dropdown choices | `<SelectTrigger>`, `<SelectContent>`, `<SelectItem>` |
| `<Input>` | All text inputs | `type="search"`, `type="text"`, `type="url"`, etc. |
| `<DropdownMenu>` | Row actions (edit, delete) | `<DropdownMenuTrigger>`, `<DropdownMenuContent>`, `<DropdownMenuItem>` |
| `<AlertDialog>` | Destructive confirms | `<AlertDialogTrigger>`, `<AlertDialogContent>`, `<AlertDialogAction>` |
| `<Alert>` | Error/success messages | `variant="destructive"` for errors |
| `<Skeleton>` | Loading placeholders | Matches shape of content being loaded |
| `<ScrollArea>` | Scrollable lists | Used in search results, long tables |
| `<Avatar>` | User profile image | Header dropdown, member list |
| `<Separator>` | Dividers | Section breaks in cards and content |
| `<Pagination>` | Page navigation | Optional, if APIs return paginated data |

---

## Icon Library

Use **lucide-react** (ships with shadcn/ui):

| Icon | Usage |
|------|-------|
| `Search` | Search inputs |
| `Menu` / `X` | Mobile nav toggle |
| `MapPin` | Location (city) |
| `Calendar` | Dates, processions |
| `Clock` | Duration, time |
| `Users` | Members count |
| `Shield` | Hermandad icon |
| `Music` | Marchas |
| `List` | Cruceta items |
| `GripVertical` | Drag handle |
| `Trash2` | Delete action |
| `Pencil` | Edit action |
| `Plus` | Add new |
| `Save` | Save action |
| `ArrowLeft` | Back navigation |
| `Check` | Confirmation |
| `AlertCircle` | Error indicator |
| `Loader2` (animated) | Loading spinner |
| `LogOut` | Logout |
| `User` | User avatar placeholder |
