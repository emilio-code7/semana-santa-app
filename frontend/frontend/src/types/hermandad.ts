export interface Hermandad {
  id: string;
  name: string;
  city: string;
  foundedYear: number;
  description: string | null;
  createdAt: string;
}

export interface Procesion {
  id: string;
  hermandadId: string;
  date: string;
  time: string;
  status: string;
  createdAt: string;
}

export type ProcesionStatus = "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type HermandadRole = "HERMANDAD_ADMIN" | "CAPATAZ" | "BAND_DIRECTOR" | "MUSICIAN";

export interface HermandadMember {
  id: string;
  hermandadId: string;
  userId: string;
  role: HermandadRole;
  joinedAt: string;
  updatedAt: string;
}
