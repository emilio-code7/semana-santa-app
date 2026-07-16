import Link from "next/link";

interface Props {
  searchParams: Promise<{ error?: string }>;
}

export default async function AuthErrorPage({ searchParams }: Props) {
  const { error } = await searchParams;

  const errorMessages: Record<string, string> = {
    AccessDenied: "You do not have access to this resource.",
    Configuration: "There is a problem with the server configuration.",
    OAuthSignin: "There was a problem signing in with the OAuth provider.",
    OAuthCallback: "There was a problem handling the OAuth response.",
    Default: "An authentication error occurred.",
  };

  const message = error ? (errorMessages[error] ?? error) : errorMessages.Default;

  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="flex flex-col items-center gap-4 text-center">
        <h1 className="text-2xl font-semibold">Authentication Error</h1>
        <p className="text-sm text-destructive">{message}</p>
        <Link
          href="/auth/login"
          className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:opacity-90"
        >
          Try again
        </Link>
      </div>
    </div>
  );
}
