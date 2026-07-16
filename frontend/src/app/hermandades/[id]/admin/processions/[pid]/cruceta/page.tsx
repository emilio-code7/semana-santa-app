"use client"

import { use, useState, useMemo, useCallback, useEffect, useRef } from "react"
import { useSession } from "next-auth/react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import Link from "next/link"
import { toast, Toaster } from "sonner"
import {
  ArrowUp, ArrowDown, Plus, X, Search,
  Save, RotateCcw, Music, ChevronLeft,
} from "lucide-react"

import type { Marcha, BandType } from "@/types/marcha"
import type { Procesion } from "@/types/procesion"
import type { CrucetaItem, CrucetaRequest } from "@/types/cruceta"
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"

const API_BASE = "http://localhost:8080/api"

const BAND_TYPES: { label: string; value: BandType | null }[] = [
  { label: "ALL", value: null },
  { label: "Palio", value: "BANDA_PALIO" },
  { label: "Agrupación", value: "AGRUPACION_MUSICAL" },
  { label: "Cornetas", value: "BANDA_CORNETAS" },
]

const BAND_TYPE_BADGE: Record<BandType, string> = {
  BANDA_PALIO: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300",
  AGRUPACION_MUSICAL: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300",
  BANDA_CORNETAS: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("es-ES", {
    year: "numeric", month: "long", day: "numeric",
  })
}

function LoadingSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-6 w-48" />
      <Skeleton className="h-4 w-72" />
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
        <div className="space-y-3">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
        <div className="space-y-3">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      </div>
    </div>
  )
}

export default function CrucetaPage({
  params,
}: {
  params: Promise<{ id: string; pid: string }>
}) {
  const { id: hermandadId, pid: procesionId } = use(params)
  const { data: session } = useSession()
  const token = session?.accessToken
  const queryClient = useQueryClient()

  // -- queries --
  const { data: procesion } = useQuery({
    queryKey: ["procesion", procesionId],
    queryFn: async () => {
      const res = await fetch(`${API_BASE}/procesiones/${encodeURIComponent(procesionId)}`,
        { headers: token ? { Authorization: `Bearer ${token}` } : {} },
      )
      if (!res.ok) throw new Error("Failed to fetch procesion")
      return res.json() as Promise<Procesion>
    },
    enabled: !!procesionId,
  })

  const { data: marchas = [], isLoading: loadingMarchas } = useQuery({
    queryKey: ["marchas"],
    queryFn: async () => {
      const res = await fetch(`${API_BASE}/marchas`,
        { headers: token ? { Authorization: `Bearer ${token}` } : {} },
      )
      if (!res.ok) throw new Error("Failed to fetch marchas")
      return res.json() as Promise<Marcha[]>
    },
    enabled: !!token,
    staleTime: 60_000,
  })

  const {
    data: cruceta,
    isLoading: loadingCruceta,
    isError: crucetaError,
  } = useQuery({
    queryKey: ["cruceta", hermandadId, procesionId],
    queryFn: async () => {
      const res = await fetch(
        `${API_BASE}/hermandades/${encodeURIComponent(hermandadId)}/procesiones/${encodeURIComponent(procesionId)}/cruceta`,
        { headers: token ? { Authorization: `Bearer ${token}` } : {} },
      )
      if (res.status === 404) return { items: [] as CrucetaItem[] }
      if (!res.ok) throw new Error("Failed to fetch cruceta")
      return res.json()
    },
    enabled: !!hermandadId && !!procesionId,
  })

  // -- local editing state --
  const [items, setItems] = useState<CrucetaItem[]>([])
  const savedRef = useRef<CrucetaItem[]>([])
  const initialized = useRef(false)

  // Hydrate local state from API on first load
  useEffect(() => {
    if (cruceta && !initialized.current) {
      const serverItems = cruceta.items ?? []
      setItems(serverItems)
      savedRef.current = serverItems
      initialized.current = true
    }
  }, [cruceta])

  // -- catalog filter state --
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedSearch, setDebouncedSearch] = useState("")
  const [bandTypeFilter, setBandTypeFilter] = useState<BandType | null>(null)

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(searchQuery), 200)
    return () => clearTimeout(t)
  }, [searchQuery])

  // -- helpers --
  const marchaMap = useMemo(() => {
    const map = new Map<string, Marcha>()
    marchas.forEach((m) => map.set(m.id, m))
    return map
  }, [marchas])

  const addedMarchaIds = useMemo(() => new Set(items.map((i) => i.marchaId)), [items])

  const filteredMarchas = useMemo(() => {
    let result = marchas
    if (debouncedSearch) {
      const q = debouncedSearch.toLowerCase()
      result = result.filter(
        (m) =>
          m.title.toLowerCase().includes(q) ||
          m.composer.toLowerCase().includes(q),
      )
    }
    if (bandTypeFilter) {
      result = result.filter((m) => m.bandType === bandTypeFilter)
    }
    return result
  }, [marchas, debouncedSearch, bandTypeFilter])

  // -- actions --
  const addMarcha = useCallback((marchaId: string) => {
    setItems((prev) => {
      if (prev.some((i) => i.marchaId === marchaId)) return prev
      return [...prev, { id: "", marchaId, orderIndex: prev.length, notes: null }]
    })
  }, [])

  const removeItem = useCallback((marchaId: string) => {
    setItems((prev) => prev.filter((i) => i.marchaId !== marchaId))
  }, [])

  const moveItem = useCallback((index: number, direction: -1 | 1) => {
    setItems((prev) => {
      const idx = prev.findIndex((_, i) => i === index)
      if (idx === -1) return prev
      const target = idx + direction
      if (target < 0 || target >= prev.length) return prev
      const next = [...prev]
      ;[next[idx], next[target]] = [next[target], next[idx]]
      return next
    })
  }, [])

  const updateNotes = useCallback((marchaId: string, notes: string) => {
    setItems((prev) =>
      prev.map((i) => (i.marchaId === marchaId ? { ...i, notes: notes || null } : i)),
    )
  }, [])

  const saveMutation = useMutation({
    mutationFn: async (request: CrucetaRequest) => {
      const res = await fetch(
        `${API_BASE}/hermandades/${encodeURIComponent(hermandadId)}/procesiones/${encodeURIComponent(procesionId)}/cruceta`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: JSON.stringify(request),
        },
      )
      if (!res.ok) throw new Error("Failed to save cruceta")
      return res.json()
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["cruceta", hermandadId, procesionId] })
      const saved = (data?.items ?? items) as CrucetaItem[]
      setItems(saved)
      savedRef.current = saved
      toast.success("Cruceta saved successfully")
    },
    onError: () => {
      toast.error("Failed to save cruceta")
    },
  })

  const handleSave = useCallback(() => {
    const request: CrucetaRequest = {
      items: items.map((item, index) => ({
        marchaId: item.marchaId,
        orderIndex: index,
        notes: item.notes,
      })),
    }
    saveMutation.mutate(request)
  }, [items, saveMutation])

  const handleCancel = useCallback(() => {
    setItems([...savedRef.current])
  }, [])

  const hasChanges = useMemo(
    () => JSON.stringify(items) !== JSON.stringify(savedRef.current),
    [items],
  )

  const isLoading = loadingMarchas || loadingCruceta

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <LoadingSkeleton />
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-6 max-w-6xl">
      <Toaster />

      {/* Header */}
      <div className="mb-6">
        <Link
          href={`/hermandades/${hermandadId}/admin/processions`}
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-2"
        >
          <ChevronLeft className="h-4 w-4" />
          Back to Processions
        </Link>
        <h1 className="text-2xl font-bold">Cruceta Editor</h1>
        {procesion && (
          <p className="text-sm text-muted-foreground mt-1">
            {formatDate(procesion.date)} at {procesion.time}
          </p>
        )}
      </div>

      {/* Two-panel layout */}
      <div className="flex flex-col md:flex-row gap-6">

        {/* LEFT PANEL — Marcha catalog */}
        <div className="w-full md:w-1/2 lg:w-2/5 space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Marcha Catalog</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {/* Search */}
              <div className="relative">
                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
                <Input
                  placeholder="Search by title or composer..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8"
                />
              </div>

              {/* Band type filter */}
              <div className="flex gap-1 flex-wrap">
                {BAND_TYPES.map((bt) => (
                  <Button
                    key={bt.label}
                    variant={bandTypeFilter === bt.value ? "default" : "outline"}
                    size="sm"
                    onClick={() => setBandTypeFilter(bt.value)}
                  >
                    {bt.label}
                  </Button>
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Marcha list */}
          <div className="space-y-2 max-h-[60vh] overflow-y-auto pr-1">
            {filteredMarchas.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                <Music className="mx-auto h-8 w-8 mb-2" />
                <p className="text-sm">No marchas found</p>
              </div>
            ) : (
              filteredMarchas.map((marcha) => {
                const isAdded = addedMarchaIds.has(marcha.id)
                return (
                  <Card
                    key={marcha.id}
                    size="sm"
                    className={`cursor-pointer transition-colors ${
                      isAdded
                        ? "opacity-50 pointer-events-none"
                        : "hover:bg-muted/50"
                    }`}
                    onClick={() => !isAdded && addMarcha(marcha.id)}
                  >
                    <CardContent className="flex items-center justify-between py-2">
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium truncate">{marcha.title}</p>
                        <p className="text-xs text-muted-foreground truncate">{marcha.composer}</p>
                      </div>
                      <div className="flex items-center gap-2 shrink-0 ml-2">
                        <Badge className={BAND_TYPE_BADGE[marcha.bandType]}>
                          {marcha.bandType === "BANDA_PALIO"
                            ? "Palio"
                            : marcha.bandType === "AGRUPACION_MUSICAL"
                              ? "Agrup."
                              : "Corn."}
                        </Badge>
                        {isAdded ? (
                          <Badge variant="outline" className="text-xs shrink-0">Added</Badge>
                        ) : (
                          <Button size="icon-xs" variant="ghost" onClick={() => addMarcha(marcha.id)}>
                            <Plus className="h-3.5 w-3.5" />
                          </Button>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                )
              })
            )}
          </div>
        </div>

        {/* RIGHT PANEL — Current cruceta */}
        <div className="w-full md:w-1/2 lg:w-3/5 space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>
                Current Cruceta
                {items.length > 0 && (
                  <span className="text-sm font-normal text-muted-foreground ml-2">
                    ({items.length} {items.length === 1 ? "item" : "items"})
                  </span>
                )}
              </CardTitle>
            </CardHeader>
            <CardContent>
              {items.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  <Music className="mx-auto h-8 w-8 mb-2" />
                  <p className="text-sm">
                    No marchas added yet. Search the catalog and add marchas.
                  </p>
                </div>
              ) : (
                <div className="space-y-2">
                  {items.map((item, index) => {
                    const marcha = marchaMap.get(item.marchaId)
                    return (
                      <Card key={item.marchaId} size="sm">
                        <CardContent className="py-2">
                          <div className="flex items-start gap-2">
                            {/* Order number + up/down */}
                            <div className="flex flex-col items-center gap-0.5 shrink-0 pt-0.5">
                              <span className="text-xs font-medium text-muted-foreground w-5 text-center">
                                {index + 1}
                              </span>
                              <div className="flex gap-0.5">
                                <Button
                                  size="icon-xs"
                                  variant="ghost"
                                  disabled={index === 0}
                                  onClick={() => moveItem(index, -1)}
                                >
                                  <ArrowUp className="h-3 w-3" />
                                </Button>
                                <Button
                                  size="icon-xs"
                                  variant="ghost"
                                  disabled={index === items.length - 1}
                                  onClick={() => moveItem(index, 1)}
                                >
                                  <ArrowDown className="h-3 w-3" />
                                </Button>
                              </div>
                            </div>

                            {/* Marcha info */}
                            <div className="flex-1 min-w-0">
                              {marcha ? (
                                <>
                                  <p className="text-sm font-medium truncate">{marcha.title}</p>
                                  <p className="text-xs text-muted-foreground truncate">{marcha.composer}</p>
                                </>
                              ) : (
                                <p className="text-sm text-destructive truncate">Unknown marcha</p>
                              )}
                              {/* Notes input */}
                              <Input
                                placeholder="Notes (e.g. Apertura, Paso Cristo...)"
                                value={item.notes ?? ""}
                                onChange={(e) => updateNotes(item.marchaId, e.target.value)}
                                className="mt-1.5 h-7 text-xs"
                              />
                            </div>

                            {/* Remove */}
                            <Button
                              size="icon-xs"
                              variant="ghost"
                              onClick={() => removeItem(item.marchaId)}
                              className="shrink-0 mt-0.5 text-muted-foreground hover:text-destructive"
                            >
                              <X className="h-3.5 w-3.5" />
                            </Button>
                          </div>
                        </CardContent>
                      </Card>
                    )
                  })}
                </div>
              )}
            </CardContent>
            <CardFooter className="flex justify-between">
              <Button
                variant="outline"
                onClick={handleCancel}
                disabled={!hasChanges || saveMutation.isPending}
              >
                <RotateCcw className="h-4 w-4 mr-1" />
                Cancel
              </Button>
              <Button
                onClick={handleSave}
                disabled={!hasChanges || saveMutation.isPending}
              >
                <Save className="h-4 w-4 mr-1" />
                {saveMutation.isPending ? "Saving..." : "Save"}
              </Button>
            </CardFooter>
          </Card>
        </div>
      </div>
    </div>
  )
}
