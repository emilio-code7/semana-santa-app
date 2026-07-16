import NextAuth from "next-auth";
import KeycloakProvider from "next-auth/providers/keycloak";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
  }
}

declare module "@auth/core/jwt" {
  interface JWT {
    accessToken?: string;
    idToken?: string;
  }
}

export const { handlers, auth } = NextAuth({
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID || "semana-santa-client",
      clientSecret: "",
      issuer: process.env.KEYCLOAK_ISSUER || "http://localhost:8180/realms/semana-santa",
    }),
  ],
  session: { strategy: "jwt" },
  callbacks: {
    async jwt({ token, account }) {
      if (account) {
        token.accessToken = account.access_token;
        token.idToken = account.id_token;
      }
      return token;
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken;
      return session;
    },
  },
});

export async function getAccessToken(): Promise<string | null> {
  const session = await auth();
  return session?.accessToken ?? null;
}
