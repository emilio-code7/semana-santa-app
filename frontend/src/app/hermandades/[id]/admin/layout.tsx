import Link from "next/link"
import { notFound } from "next/navigation"
import { Hermandad } from "@/types/hermandad"
import { AdminSidebar } from "./admin-sidebar"

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

export default async function AdminLayout({
  children,
  params,
}: {
  children: React.ReactNode
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const hermandad = await getHermandad(id)
  if (!hermandad) notFound()

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
          <Link href={`/hermandades/${id}`} className="hover:text-foreground">
            {hermandad.name}
          </Link>
          <span>/</span>
          <span>Admin</span>
        </div>
        <h1 className="text-3xl font-bold">Dashboard</h1>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        <AdminSidebar hermandadId={id} />
        <div className="flex-1 min-w-0">{children}</div>
      </div>
    </div>
  )
}
