"use client";

import { useSession, signIn, signOut } from "next-auth/react";

export default function AuthStatus() {
  const { data: session } = useSession();

  if (!session) {
    return (
      <button
        onClick={() => signIn("keycloak")}
        className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:opacity-90"
      >
        Sign in
      </button>
    );
  }

  return (
    <div className="flex items-center gap-4">
      <span className="text-sm text-muted-foreground">
        {session.user?.name ?? session.user?.email}
      </span>
      <button
        onClick={() => signOut()}
        className="rounded-md bg-destructive px-4 py-2 text-sm text-destructive-foreground hover:opacity-90"
      >
        Sign out
      </button>
    </div>
  );
}
