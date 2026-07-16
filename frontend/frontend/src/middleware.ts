export { auth as middleware } from "@/lib/auth";

export const config = {
  matcher: ["/hermandades/:path*/admin/:path*"],
};
