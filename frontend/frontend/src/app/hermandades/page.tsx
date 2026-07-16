import { Hermandad } from "@/types/hermandad"
import { auth } from "@/lib/auth"
import HermandadListClient from "./hermandad-list-client"

async function getHermandades(): Promise<Hermandad[]> {
  const res = await fetch("http://localhost:8080/api/hermandades", {
    cache: "no-store",
  })
  if (!res.ok) throw new Error("Failed to fetch hermandades")
  return res.json()
}

export default async function HermandadesPage() {
  await auth() // ponytail: warm the session cookie, not needed for public page
  const hermandades = await getHermandades()
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Hermandades</h1>
      <HermandadListClient hermandades={hermandades} />
    </div>
  )
}
