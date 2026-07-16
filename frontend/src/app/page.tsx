import { Suspense } from "react"
import HeroSection from "@/components/home/HeroSection"
import HermandadCardGrid, { HermandadCardGridSkeleton } from "@/components/home/HermandadCardGrid"

export default function HomePage() {
  return (
    <>
      <HeroSection />
      <section className="container mx-auto px-4 py-12">
        <h2 className="text-2xl font-bold mb-6">Featured Hermandades</h2>
        <Suspense fallback={<HermandadCardGridSkeleton />}>
          <HermandadCardGrid />
        </Suspense>
      </section>
    </>
  )
}
