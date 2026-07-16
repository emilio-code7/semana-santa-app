"use client"

import { useState, useEffect, use, useCallback } from "react"
import { useSession } from "next-auth/react"
import { toast } from "sonner"
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog"
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Plus, Trash2 } from "lucide-react"
import type { HermandadMember, HermandadRole } from "@/types/hermandad"

const roleBadgeClass: Record<HermandadRole, string> = {
  HERMANDAD_ADMIN:
    "bg-purple-100 text-purple-800 border-purple-200 hover:bg-purple-100",
  CAPATAZ:
    "bg-amber-100 text-amber-800 border-amber-200 hover:bg-amber-100",
  BAND_DIRECTOR:
    "bg-blue-100 text-blue-800 border-blue-200 hover:bg-blue-100",
  MUSICIAN:
    "bg-green-100 text-green-800 border-green-200 hover:bg-green-100",
}

const roles: HermandadRole[] = [
  "HERMANDAD_ADMIN",
  "CAPATAZ",
  "BAND_DIRECTOR",
  "MUSICIAN",
]

function RoleBadge({ role }: { role: HermandadRole }) {
  return (
    <Badge variant="outline" className={roleBadgeClass[role]}>
      {role}
    </Badge>
  )
}

function MembersSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 4 }).map((_, i) => (
        <Skeleton key={i} className="h-8 w-full" />
      ))}
    </div>
  )
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString("es-ES", {
    year: "numeric",
    month: "short",
    day: "numeric",
  })
}

export default function AdminMembersPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: session } = useSession()
  const [members, setMembers] = useState<HermandadMember[]>([])
  const [loading, setLoading] = useState(true)
  const [addOpen, setAddOpen] = useState(false)
  const [newUserId, setNewUserId] = useState("")
  const [newRole, setNewRole] = useState<HermandadRole>("MUSICIAN")
  const [adding, setAdding] = useState(false)
  const [removeOpen, setRemoveOpen] = useState(false)
  const [removeTarget, setRemoveTarget] = useState<HermandadMember | null>(
    null,
  )
  const [removing, setRemoving] = useState(false)

  const authHeaders = useCallback(
    () => ({
      "Content-Type": "application/json",
      ...(session?.accessToken
        ? { Authorization: `Bearer ${session.accessToken}` }
        : {}),
    }),
    [session?.accessToken],
  )

  const fetchMembers = useCallback(async () => {
    try {
      const res = await fetch(
        `http://localhost:8080/api/hermandades/${encodeURIComponent(id)}/members?page=0&size=100`,
        { headers: authHeaders() },
      )
      if (!res.ok) throw new Error("Failed to fetch members")
      const data = (await res.json()) as
        | HermandadMember[]
        | { content: HermandadMember[] }
      setMembers(Array.isArray(data) ? data : data.content ?? [])
    } catch {
      toast.error("Failed to load members")
    } finally {
      setLoading(false)
    }
  }, [id, authHeaders])

  useEffect(() => {
    void fetchMembers()
  }, [fetchMembers])

  async function handleAddMember() {
    if (!newUserId.trim()) {
      toast.error("User ID is required")
      return
    }
    setAdding(true)
    try {
      const res = await fetch(
        `http://localhost:8080/api/hermandades/${encodeURIComponent(id)}/members`,
        {
          method: "POST",
          headers: authHeaders(),
          body: JSON.stringify({ userId: newUserId.trim(), role: newRole }),
        },
      )
      if (!res.ok) throw new Error("Failed to add member")
      toast.success("Member added")
      setAddOpen(false)
      setNewUserId("")
      setNewRole("MUSICIAN")
      await fetchMembers()
    } catch {
      toast.error("Failed to add member")
    } finally {
      setAdding(false)
    }
  }

  async function handleRoleChange(
    member: HermandadMember,
    role: HermandadRole,
  ) {
    setMembers((prev) =>
      prev.map((m) => (m.id === member.id ? { ...m, role } : m)),
    )
    try {
      const res = await fetch(
        `http://localhost:8080/api/hermandades/${encodeURIComponent(id)}/members/${encodeURIComponent(member.userId)}/role`,
        {
          method: "PATCH",
          headers: authHeaders(),
          body: JSON.stringify({ role }),
        },
      )
      if (!res.ok) throw new Error("Failed to update role")
      toast.success("Role updated")
    } catch {
      toast.error("Failed to update role")
      await fetchMembers()
    }
  }

  async function handleRemoveMember() {
    if (!removeTarget) return
    setRemoving(true)
    try {
      const res = await fetch(
        `http://localhost:8080/api/hermandades/${encodeURIComponent(id)}/members/${encodeURIComponent(removeTarget.userId)}`,
        { method: "DELETE", headers: authHeaders() },
      )
      if (!res.ok) throw new Error("Failed to remove member")
      toast.success("Member removed")
      setRemoveOpen(false)
      setRemoveTarget(null)
      await fetchMembers()
    } catch {
      toast.error("Failed to remove member")
    } finally {
      setRemoving(false)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Members</h2>
        <Dialog open={addOpen} onOpenChange={setAddOpen}>
          <DialogTrigger render={<Button />}>
            <Plus />
            Add Member
          </DialogTrigger>
        </Dialog>
      </div>

      {loading ? (
        <MembersSkeleton />
      ) : members.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            No members yet. Add the first member.
          </CardContent>
        </Card>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>User ID</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Joined</TableHead>
              <TableHead className="w-24">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {members.map((member) => (
              <TableRow key={member.id}>
                <TableCell className="font-mono text-xs">
                  {member.userId}
                </TableCell>
                <TableCell>
                  <Select
                    value={member.role}
                    onValueChange={(v) =>
                      handleRoleChange(member, v as HermandadRole)
                    }
                  >
                    <SelectTrigger size="sm">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {roles.map((r) => (
                        <SelectItem key={r} value={r}>
                          <RoleBadge role={r} />
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </TableCell>
                <TableCell>{formatDate(member.joinedAt)}</TableCell>
                <TableCell>
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => {
                      setRemoveTarget(member)
                      setRemoveOpen(true)
                    }}
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Dialog open={removeOpen} onOpenChange={setRemoveOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Remove Member</DialogTitle>
            <DialogDescription>
              Are you sure you want to remove {removeTarget?.userId}? This
              action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose render={<Button variant="outline" />}>
              Cancel
            </DialogClose>
            <Button
              variant="destructive"
              onClick={() => void handleRemoveMember()}
              disabled={removing}
            >
              {removing ? "Removing..." : "Remove"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
