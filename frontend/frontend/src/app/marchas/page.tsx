"use client";

import { useState, useEffect, useMemo, useCallback } from "react";
import { useSession, signIn } from "next-auth/react";
import { Search, Music } from "lucide-react";
import { Marcha, BandType } from "@/types/marcha";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";

const BAND_TYPES: { label: string; value: BandType | null }[] = [
  { label: "ALL", value: null },
  { label: "Banda de Palio", value: "BANDA_PALIO" },
  { label: "Agrupación Musical", value: "AGRUPACION_MUSICAL" },
  { label: "Banda de Cornetas", value: "BANDA_CORNETAS" },
];

const BAND_TYPE_BADGE: Record<BandType, { label: string; className: string }> = {
  BANDA_PALIO: { label: "Palio", className: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300" },
  AGRUPACION_MUSICAL: { label: "Agrupación", className: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300" },
  BANDA_CORNETAS: { label: "Cornetas", className: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300" },
};

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

function MarchaCard({ marcha }: { marcha: Marcha }) {
  const badge = BAND_TYPE_BADGE[marcha.bandType];
  return (
    <Card className="h-full hover:shadow-lg transition-shadow">
      <CardHeader>
        <div className="flex items-start justify-between gap-2">
          <CardTitle className="text-base">{marcha.title}</CardTitle>
          <Badge className={badge.className}>{badge.label}</Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground mb-3">{marcha.composer}</p>
        <p className="text-xs text-muted-foreground">{formatDuration(marcha.durationSeconds)}</p>
      </CardContent>
    </Card>
  );
}

function MarchaCardSkeleton() {
  return (
    <Card>
      <CardHeader>
        <Skeleton className="h-5 w-3/4 mb-1" />
        <Skeleton className="h-4 w-1/3" />
      </CardHeader>
      <CardContent>
        <Skeleton className="h-4 w-1/2 mb-2" />
        <Skeleton className="h-3 w-1/4" />
      </CardContent>
    </Card>
  );
}

function MarchaGridSkeleton() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {Array.from({ length: 6 }).map((_, i) => (
        <MarchaCardSkeleton key={i} />
      ))}
    </div>
  );
}

export default function MarchasPage() {
  const { data: session, status } = useSession();
  const [marchas, setMarchas] = useState<Marcha[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [selectedBandType, setSelectedBandType] = useState<BandType | null>(null);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchQuery), 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Fetch marchas when authenticated
  const fetchMarchas = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("http://localhost:8080/api/marchas", { cache: "no-store" });
      if (!res.ok) throw new Error("Failed to fetch marchas");
      const data: Marcha[] = await res.json();
      setMarchas(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (status === "authenticated") {
      fetchMarchas();
    }
  }, [status, fetchMarchas]);

  // Filter marchas
  const filteredMarchas = useMemo(() => {
    let result = marchas;
    if (debouncedSearch) {
      const q = debouncedSearch.toLowerCase();
      result = result.filter(
        (m) =>
          m.title.toLowerCase().includes(q) ||
          m.composer.toLowerCase().includes(q)
      );
    }
    if (selectedBandType) {
      result = result.filter((m) => m.bandType === selectedBandType);
    }
    return result;
  }, [marchas, debouncedSearch, selectedBandType]);

  // Not authenticated
  if (status === "unauthenticated") {
    return (
      <div className="container mx-auto px-4 py-24 text-center">
        <Music className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <h1 className="text-2xl font-bold mb-2">Marcha Catalog</h1>
        <p className="text-muted-foreground mb-6">Sign in to view the marcha catalog</p>
        <Button onClick={() => signIn("keycloak")}>Sign In</Button>
      </div>
    );
  }

  // Loading auth
  if (status === "loading") {
    return (
      <div className="container mx-auto px-4 py-24">
        <MarchaGridSkeleton />
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">Marcha Catalog</h1>
        <p className="text-muted-foreground">Browse the Semana Santa marcha repertoire</p>
      </div>

      {/* Filter bar */}
      <div className="flex flex-col sm:flex-row gap-4 mb-8">
        <div className="relative flex-1">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
          <Input
            placeholder="Search by title or composer..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8"
          />
        </div>
        <div className="flex gap-1.5 flex-wrap">
          {BAND_TYPES.map((bt) => (
            <Button
              key={bt.label}
              variant={selectedBandType === bt.value ? "default" : "outline"}
              size="sm"
              onClick={() => setSelectedBandType(bt.value)}
            >
              {bt.label}
            </Button>
          ))}
        </div>
      </div>

      {/* Content */}
      {loading ? (
        <MarchaGridSkeleton />
      ) : error ? (
        <div className="text-center py-12">
          <p className="text-lg text-destructive mb-4">{error}</p>
          <Button variant="default" onClick={fetchMarchas}>
            Try Again
          </Button>
        </div>
      ) : filteredMarchas.length === 0 ? (
        <div className="text-center py-12">
          <Music className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
          <p className="text-lg text-muted-foreground">No marchas found matching your search</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredMarchas.map((marcha) => (
            <a key={marcha.id} href={`/marchas/${marcha.id}`} className="block">
              <MarchaCard marcha={marcha} />
            </a>
          ))}
        </div>
      )}
    </div>
  );
}
