import { notFound } from "next/navigation"
import Link from "next/link"
import { auth } from "@/lib/auth"
import { Hermandad, Procesion } from "@/types/hermandad"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/components/ui/table"

async function getHermandad(id: string): Promise<Hermandad | null> {
  try {
    const res = await fetch(
      `http://localhost:8080/api/hermandades/${encodeURIComponent(id)}`,
      { cache: "no-store" },
    )
    if (res.status === 404) return null
    if (!res.ok) throw new Error("Failed to fetch hermandad")
    return res.json()
  } catch {
    return null
  }
}

async function getProcesiones(hermandadId: string): Promise<Procesion[]> {
  try {
    const res = await fetch(
      `http://localhost:8080/api/procesiones?hermandadId=${encodeURIComponent(hermandadId)}&page=0&size=20`,
      { cache: "no-store" },
    )
    if (!res.ok) return []
    const data = await res.json()
    // Handle Spring Page wrapper or flat array
    return Array.isArray(data) ? data : data.content ?? []
  } catch {
    return []
  }
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("es-ES", {
    year: "numeric",
    month: "long",
    day: "numeric",
  })
}

const statusStyles: Record<string, string> = {
  PLANNED: "bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-50",
  IN_PROGRESS:
    "bg-blue-50 text-blue-700 border-blue-200 hover:bg-blue-50",
  COMPLETED:
    "bg-green-50 text-green-700 border-green-200 hover:bg-green-50",
  CANCELLED: "bg-red-50 text-red-700 border-red-200 hover:bg-red-50",
}

function StatusBadge({ status }: { status: string }) {
  return (
    <Badge
      variant="outline"
      className={statusStyles[status] ?? statusStyles.PLANNED}
    >
      {status}
    </Badge>
  )
}

export default async function HermandadDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const [hermandad, procesiones] = await Promise.all([
    getHermandad(id),
    getProcesiones(id),
  ])

  if (!hermandad) notFound()

  const session = await auth()
  // ponytail: basic auth check; refine with role extraction from JWT when needed
  const isAdmin = !!session?.user?.email

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const upcoming = procesiones
    .filter((p) => new Date(p.date) >= today)
    .sort(
      (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime(),
    )

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-4xl font-bold mb-2">{hermandad.name}</h1>
            <p className="text-lg text-muted-foreground">
              {hermandad.city} &middot; Founded {hermandad.foundedYear}
            </p>
          </div>
          {isAdmin && (
            <Link href={`/hermandades/${id}/admin`}>
              <Button variant="default">Admin</Button>
            </Link>
          )}
        </div>
        {hermandad.description && (
          <p className="mt-4 text-foreground/80 max-w-2xl">
            {hermandad.description}
          </p>
        )}
      </div>

      <section>
        <h2 className="text-xl font-semibold mb-4">
          Upcoming Processions
        </h2>
        {upcoming.length === 0 ? (
          <p className="text-muted-foreground py-4 text-center">
            No upcoming processions
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead>Time</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {upcoming.map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{formatDate(p.date)}</TableCell>
                  <TableCell>{p.time}</TableCell>
                  <TableCell>
                    <StatusBadge status={p.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </section>
    </div>
  )
}
