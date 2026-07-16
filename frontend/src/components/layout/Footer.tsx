import Link from "next/link";

export function Footer() {
  return (
    <footer className="mt-auto border-t bg-muted py-8">
      <div className="mx-auto flex max-w-7xl flex-col items-center justify-center gap-2 px-4 text-center text-sm text-muted-foreground">
        <p>&copy; {new Date().getFullYear()} Repertorio</p>
        <div className="flex items-center gap-4">
          <Link
            href="https://github.com/emilio/repertorio"
            className="hover:text-foreground transition-colors"
            target="_blank"
            rel="noopener noreferrer"
          >
            GitHub
          </Link>
          <Link href="/about" className="hover:text-foreground transition-colors">
            About
          </Link>
        </div>
      </div>
    </footer>
  );
}
