import Link from "next/link"
import { Hermandad } from "@/types/hermandad"
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { HermandadCardGridError } from "./HermandadCardGridError"

async function getHermandades(): Promise<Hermandad[]> {
  const res = await fetch("http://localhost:8080/api/hermandades", {
    cache: "no-store",
  })
  if (!res.ok) throw new Error("Failed to fetch hermandades")
  return res.json()
}

export default async function HermandadCardGrid() {
  let hermandades: Hermandad[]
  try {
    hermandades = await getHermandades()
  } catch {
    return <HermandadCardGridError />
  }

  if (hermandades.length === 0) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        <p className="text-lg">No hermandades yet</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {hermandades.map((h) => (
        <Link key={h.id} href={`/hermandades/${h.id}`} className="block">
          <Card className="h-full hover:shadow-lg transition-shadow cursor-pointer">
            <CardHeader>
              <CardTitle>{h.name}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground mb-2">
                {h.city} &middot; Founded {h.foundedYear}
              </p>
              {h.description && (
                <p className="text-sm text-muted-foreground line-clamp-3">
                  {h.description}
                </p>
              )}
            </CardContent>
          </Card>
        </Link>
      ))}
    </div>
  )
}

export function HermandadCardGridSkeleton() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {Array.from({ length: 6 }).map((_, i) => (
        <Card key={i}>
          <CardHeader>
            <Skeleton className="h-5 w-3/4" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-4 w-1/2 mb-2" />
            <Skeleton className="h-4 w-full mb-1" />
            <Skeleton className="h-4 w-2/3" />
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
