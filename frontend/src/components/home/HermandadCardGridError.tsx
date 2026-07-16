"use client"

import { Button } from "@/components/ui/button"

export function HermandadCardGridError() {
  return (
    <div className="text-center py-12">
      <p className="text-lg text-destructive mb-4">Could not load hermandades</p>
      <Button variant="default" onClick={() => window.location.reload()}>
        Try Again
      </Button>
    </div>
  )
}
