"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import { LayoutDashboard, Users, Route } from "lucide-react"

const tabs = [
  { href: "", label: "Overview", icon: LayoutDashboard },
  { href: "/members", label: "Members", icon: Users },
  { href: "/processions", label: "Processions", icon: Route },
]

export function AdminSidebar({ hermandadId }: { hermandadId: string }) {
  const pathname = usePathname()

  return (
    <nav className="flex md:flex-col gap-1 md:w-48 shrink-0 overflow-x-auto pb-2 md:pb-0">
      {tabs.map((tab) => {
        const href = `/hermandades/${hermandadId}/admin${tab.href}`
        const isActive = pathname === href
        const Icon = tab.icon
        return (
          <Link
            key={tab.href}
            href={href}
            className={cn(
              "flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-colors",
              isActive
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:text-foreground hover:bg-muted",
            )}
          >
            <Icon className="h-4 w-4 shrink-0" />
            {tab.label}
          </Link>
        )
      })}
    </nav>
  )
}
