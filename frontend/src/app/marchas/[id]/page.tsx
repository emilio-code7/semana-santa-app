import { notFound } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, ExternalLink, Clock, Calendar } from "lucide-react";
import { Marcha } from "@/types/marcha";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";

const BAND_TYPE_BADGE: Record<string, { label: string; className: string }> = {
  BANDA_PALIO: { label: "Palio", className: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300" },
  AGRUPACION_MUSICAL: { label: "Agrupación", className: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300" },
  BANDA_CORNETAS: { label: "Cornetas", className: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300" },
};

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

async function getMarcha(id: string): Promise<Marcha | null> {
  try {
    const res = await fetch(`http://localhost:8080/api/marchas/${id}`, {
      cache: "no-store",
    });
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

export default async function MarchaDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const marcha = await getMarcha(id);

  if (!marcha) notFound();

  const badge = BAND_TYPE_BADGE[marcha.bandType];

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl">
      <Link
        href="/marchas"
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground mb-6 transition-colors"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to catalog
      </Link>

      <Card>
        <CardContent className="p-6 space-y-6">
          <div>
            <div className="flex items-start justify-between gap-4 mb-2">
              <h1 className="text-2xl font-bold">{marcha.title}</h1>
              <Badge className={badge.className}>{badge.label}</Badge>
            </div>
            <p className="text-lg text-muted-foreground">{marcha.composer}</p>
          </div>

          <div className="flex flex-wrap gap-6 text-sm">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Clock className="h-4 w-4" />
              <span>{formatDuration(marcha.durationSeconds)}</span>
            </div>
            {marcha.compositionYear && (
              <div className="flex items-center gap-2 text-muted-foreground">
                <Calendar className="h-4 w-4" />
                <span>{marcha.compositionYear}</span>
              </div>
            )}
          </div>

          {marcha.youtubeUrl && (
            <a
              href={marcha.youtubeUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg border border-border bg-background px-2.5 h-8 text-sm font-medium hover:bg-muted hover:text-foreground transition-colors"
            >
              <ExternalLink className="h-4 w-4" />
              Listen on YouTube
            </a>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
