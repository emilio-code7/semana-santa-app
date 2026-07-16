"use client"

import { useState } from "react"
import Link from "next/link"
import { Search } from "lucide-react"
import { Hermandad } from "@/types/hermandad"
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"

export default function HermandadListClient({
  hermandades,
}: {
  hermandades: Hermandad[]
}) {
  const [search, setSearch] = useState("")

  const filtered = hermandades.filter(
    (h) =>
      h.name.toLowerCase().includes(search.toLowerCase()) ||
      h.city.toLowerCase().includes(search.toLowerCase()),
  )

  return (
    <>
      <div className="relative mb-6 max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Search by name or city..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-9"
        />
      </div>

      {filtered.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground">
          <p className="text-lg">No hermandades found</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filtered.map((h) => (
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
      )}
    </>
  )
}
