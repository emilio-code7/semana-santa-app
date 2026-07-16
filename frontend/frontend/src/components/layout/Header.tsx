"use client";

import Link from "next/link";
import { useSession, signIn, signOut } from "next-auth/react";
import { Button } from "@/components/ui/button";
import { NavLinks } from "./NavLinks";
import { MobileNav } from "./MobileNav";

export function Header() {
  const { data: session } = useSession();

  return (
    <header className="sticky top-0 z-40 w-full border-b bg-background">
      <div className="mx-auto flex h-14 md:h-16 max-w-7xl items-center justify-between px-4">
        <div className="flex items-center gap-6">
          <Link
            href="/"
            className="text-xl font-bold tracking-tight text-foreground"
          >
            Repertorio
          </Link>
          <NavLinks className="hidden md:flex" />
        </div>
        <div className="flex items-center gap-2">
          {session ? (
            <Button variant="outline" size="sm" onClick={() => signOut()}>
              Logout
            </Button>
          ) : (
            <Button size="sm" onClick={() => signIn("keycloak")}>
              Login
            </Button>
          )}
          <MobileNav />
        </div>
      </div>
    </header>
  );
}
