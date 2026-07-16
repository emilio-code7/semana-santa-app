import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Users, Route, Calendar } from "lucide-react"

export default async function AdminOverviewPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params

  const [membersRes, procesionesRes] = await Promise.all([
    fetch(
      `http://localhost:8080/api/hermandades/${encodeURIComponent(id)}/members?page=0&size=1`,
      { cache: "no-store" },
    ),
    fetch(
      `http://localhost:8080/api/procesiones?hermandadId=${encodeURIComponent(id)}&page=0&size=100`,
      { cache: "no-store" },
    ),
  ])

  const memberCount = membersRes.ok
    ? ((await membersRes.json()) as { totalElements?: number }).totalElements ?? 0
    : 0

  let upcomingCount = 0
  let nextEventDate: string | null = null
  if (procesionesRes.ok) {
    const data = (await procesionesRes.json()) as {
      content?: { date: string }[]
    }
    const procesiones = Array.isArray(data) ? data : data.content ?? []
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const upcoming = procesiones
      .filter((p) => new Date(p.date) >= today)
      .sort(
        (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime(),
      )
    upcomingCount = upcoming.length
    if (upcoming.length > 0) {
      nextEventDate = new Date(upcoming[0].date).toLocaleDateString("es-ES", {
        year: "numeric",
        month: "long",
        day: "numeric",
      })
    }
  }

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-semibold">Overview</h2>
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="h-4 w-4" />
              Members
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{memberCount}</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Route className="h-4 w-4" />
              Upcoming Processions
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{upcomingCount}</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              Next Event
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-lg font-medium">
              {nextEventDate ?? "N/A"}
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
