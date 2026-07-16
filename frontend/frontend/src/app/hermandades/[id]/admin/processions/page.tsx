"use client"

import { use, useState } from "react"
import { useSession } from "next-auth/react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import Link from "next/link"
import { toast, Toaster } from "sonner"
import { PlusIcon } from "lucide-react"

import type { Procesion, ProcesionStatus } from "@/types/procesion"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog"
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { Input } from "@/components/ui/input"

const API_BASE = "http://localhost:8080/api"

const statusStyles: Record<ProcesionStatus, string> = {
  PLANNED: "bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-50",
  IN_PROGRESS: "bg-blue-50 text-blue-700 border-blue-200 hover:bg-blue-50",
  COMPLETED: "bg-green-50 text-green-700 border-green-200 hover:bg-green-50",
  CANCELLED: "bg-red-50 text-red-700 border-red-200 hover:bg-red-50",
}

const validTransitions: Record<
  ProcesionStatus,
  { label: string; next: ProcesionStatus; variant: "default" | "destructive" }[]
> = {
  PLANNED: [
    { label: "Start", next: "IN_PROGRESS", variant: "default" },
    { label: "Cancel", next: "CANCELLED", variant: "destructive" },
  ],
  IN_PROGRESS: [
    { label: "Complete", next: "COMPLETED", variant: "default" },
    { label: "Cancel", next: "CANCELLED", variant: "destructive" },
  ],
  COMPLETED: [],
  CANCELLED: [],
}

function StatusBadge({ status }: { status: ProcesionStatus }) {
  return (
    <Badge variant="outline" className={statusStyles[status]}>
      {status}
    </Badge>
  )
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("es-ES", {
    year: "numeric",
    month: "long",
    day: "numeric",
  })
}

function LoadingSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className="flex gap-4">
          <Skeleton className="h-8 w-32" />
          <Skeleton className="h-8 w-20" />
          <Skeleton className="h-8 w-24" />
          <Skeleton className="h-8 w-48" />
        </div>
      ))}
    </div>
  )
}

export default function AdminProcessionsPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id: hermandadId } = use(params)
  const { data: session } = useSession()
  const token = session?.accessToken
  const queryClient = useQueryClient()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [newDate, setNewDate] = useState("")
  const [newTime, setNewTime] = useState("")

  const {
    data: processions,
    isLoading,
  } = useQuery({
    queryKey: ["processions", hermandadId],
    queryFn: async () => {
      const res = await fetch(
        `${API_BASE}/procesiones?hermandadId=${encodeURIComponent(hermandadId)}&page=0&size=20`,
        { headers: token ? { Authorization: `Bearer ${token}` } : {} },
      )
      if (!res.ok) throw new Error("Failed to fetch processions")
      const data = await res.json()
      return (Array.isArray(data) ? data : data.content ?? []) as Procesion[]
    },
    enabled: !!hermandadId,
  })

  const createMutation = useMutation({
    mutationFn: async (data: {
      hermandadId: string
      date: string
      time: string
    }) => {
      const res = await fetch(`${API_BASE}/procesiones`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error("Failed to create procesion")
      return res.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["processions", hermandadId] })
      toast.success("Procesion created successfully")
      setDialogOpen(false)
      setNewDate("")
      setNewTime("")
    },
    onError: () => {
      toast.error("Failed to create procesion")
    },
  })

  const statusMutation = useMutation({
    mutationFn: async ({
      id,
      newStatus,
    }: {
      id: string
      newStatus: ProcesionStatus
    }) => {
      const res = await fetch(`${API_BASE}/procesiones/${id}/status`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ newStatus }),
      })
      if (!res.ok) throw new Error("Failed to update status")
      return res.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["processions", hermandadId] })
      toast.success("Status updated successfully")
    },
    onError: () => {
      toast.error("Failed to update status")
    },
  })

  const sorted = [...(processions ?? [])].sort(
    (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime(),
  )

  const canCreate = newDate && newTime

  return (
    <div className="container mx-auto px-4 py-8">
      <Toaster />

      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">Procession Management</h1>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger render={<Button><PlusIcon /> Create Procesion</Button>} />
          <DialogContent>
            <DialogTitle>Create Procesion</DialogTitle>
            <DialogDescription>
              Schedule a new procession for this hermandad.
            </DialogDescription>
            <form
              onSubmit={(e) => {
                e.preventDefault()
                if (canCreate) {
                  createMutation.mutate({
                    hermandadId,
                    date: newDate,
                    time: newTime,
                  })
                }
              }}
              className="space-y-4"
            >
              <div>
                <label htmlFor="date" className="text-sm font-medium">
                  Date
                </label>
                <Input
                  id="date"
                  type="date"
                  value={newDate}
                  onChange={(e) => setNewDate(e.target.value)}
                  required
                />
              </div>
              <div>
                <label htmlFor="time" className="text-sm font-medium">
                  Time
                </label>
                <Input
                  id="time"
                  type="time"
                  value={newTime}
                  onChange={(e) => setNewTime(e.target.value)}
                  required
                />
              </div>
              <DialogFooter>
                <DialogClose render={<Button variant="outline" type="button">Cancel</Button>} />
                <Button
                  type="submit"
                  disabled={!canCreate || createMutation.isPending}
                >
                  {createMutation.isPending ? "Creating..." : "Create"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {isLoading ? (
        <LoadingSkeleton />
      ) : sorted.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground">
          <p className="text-lg">No processions planned yet. Create one!</p>
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Date</TableHead>
              <TableHead>Time</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {sorted.map((p) => (
              <TableRow key={p.id}>
                <TableCell>{formatDate(p.date)}</TableCell>
                <TableCell>{p.time}</TableCell>
                <TableCell>
                  <StatusBadge status={p.status} />
                </TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-2">
                    {validTransitions[p.status].map((action) => (
                      <Button
                        key={action.next}
                        variant={action.variant}
                        size="sm"
                        onClick={() =>
                          statusMutation.mutate({
                            id: p.id,
                            newStatus: action.next,
                          })
                        }
                        disabled={statusMutation.isPending}
                      >
                        {action.label}
                      </Button>
                    ))}
                    <Link
                      href={`/hermandades/${hermandadId}/admin/processions/${p.id}/cruceta`}
                    >
                      <Button variant="outline" size="sm">
                        Manage Cruceta
                      </Button>
                    </Link>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  )
}
