import Link from "next/link"
import { Button } from "@/components/ui/button"

export default function HermandadNotFound() {
  return (
    <div className="container mx-auto px-4 py-16 text-center">
      <h1 className="text-4xl font-bold mb-4">Hermandad not found</h1>
      <p className="text-muted-foreground mb-8">
        The hermandad you are looking for does not exist or has been
        removed.
      </p>
      <Link href="/hermandades">
        <Button>Back to Hermandades</Button>
      </Link>
    </div>
  )
}
