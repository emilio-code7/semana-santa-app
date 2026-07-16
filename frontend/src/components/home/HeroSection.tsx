import Link from "next/link"

export default function HeroSection() {
  return (
    <section className="relative flex flex-col items-center justify-center text-center px-4 py-24 md:py-32 bg-gradient-to-br from-purple-900 via-purple-800 to-amber-700 text-white overflow-hidden">
      <div className="relative z-10 max-w-3xl">
        <h1 className="font-serif text-4xl md:text-5xl lg:text-6xl font-bold mb-6 leading-tight">
          Semana Santa Management
        </h1>
        <p className="text-lg md:text-xl text-purple-100 mb-8 max-w-xl mx-auto">
          Manage brotherhoods, processions, and music for Holy Week
        </p>
        <Link
          href="/hermandades"
          className="inline-flex items-center justify-center rounded-lg bg-amber-500 hover:bg-amber-400 text-purple-900 font-semibold px-8 py-3 text-lg transition-colors"
        >
          Explore Hermandades
        </Link>
      </div>
      <div className="absolute inset-0 opacity-10 bg-[radial-gradient(circle_at_50%_50%,rgba(255,255,255,0.2)_0%,transparent_70%)]" />
    </section>
  )
}
