export type ProcesionStatus = "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface Procesion {
  id: string;
  hermandadId: string;
  date: string;
  time: string;
  status: ProcesionStatus;
  createdAt: string;
  updatedAt: string;
}
